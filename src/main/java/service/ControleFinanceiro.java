package service;

import model.*;
import repository.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Fachada usada pelas telas: cadastros, lançamentos fixos, importação, relatórios e progresso. */
public class ControleFinanceiro {

    public static final List<String> MESES = Meses.NOMES;

    private final int usuarioId;
    private final CategoriaRepository categoriaRepo;
    private final ContaRepository contaRepo;
    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;
    private final InvestimentoRepository investRepo;
    private final LancamentoFixoRepository fixoRepo;
    private final UsuarioRepository usuarioRepo = new UsuarioRepository();
    private final ImportacaoCsv importacao;

    /** Tudo o que esta instância lê ou grava pertence ao usuário informado (o usuário logado). */
    public ControleFinanceiro(int usuarioId) {
        this.usuarioId = usuarioId;
        this.categoriaRepo = new CategoriaRepository(usuarioId);
        this.contaRepo = new ContaRepository(usuarioId);
        this.receitaRepo = new ReceitaRepository(usuarioId);
        this.despesaRepo = new DespesaRepository(usuarioId);
        this.investRepo = new InvestimentoRepository(usuarioId);
        this.fixoRepo = new LancamentoFixoRepository(usuarioId);
        this.importacao = new ImportacaoCsv(usuarioId);
    }

    // --- Usuário ---

    public Usuario getUsuario() {
        return usuarioRepo.buscarPorId(usuarioId).orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
    }

    public void atualizarUsuario(String nome, LocalDate dataNascimento) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Informe o nome.");
        if (dataNascimento != null && dataNascimento.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Data de nascimento no futuro.");
        usuarioRepo.atualizarPerfil(usuarioId, nome.trim(), getUsuario().getEmail(), dataNascimento);
    }

    /** Nível e XP do usuário: lançamentos e missões cumpridas (ver {@link CalculadoraXp}). */
    public Progresso getProgresso() {
        var missoes = new repository.MissaoRepository(usuarioId);
        return CalculadoraXp.calcular(getReceitas(), getDespesas(), getInvestimentos(),
                CalculadoraXp.xpDeMissoes(missoes.totalConclusoes(), missoes.totalSemPrazoConcluidas()), LocalDate.now());
    }

    // --- Categorias ---

    public void salvarCategoria(Categoria c)   { categoriaRepo.salvar(validarNome(c.getNome(), c)); }
    public void atualizarCategoria(Categoria c) { categoriaRepo.atualizar(validarNome(c.getNome(), c)); }
    public void excluirCategoria(int id)        { categoriaRepo.excluir(id); }
    public List<Categoria> getCategorias()      { return categoriaRepo.listarTodos(); }

    // --- Contas ---

    public void salvarConta(Conta c)   { contaRepo.salvar(validarNome(c.getNome(), c)); }
    public void atualizarConta(Conta c) { contaRepo.atualizar(validarNome(c.getNome(), c)); }
    public void excluirConta(int id)    { contaRepo.excluir(id); }
    public List<Conta> getContas()      { return contaRepo.listarTodos(); }

    // --- Receitas ---

    public void salvarReceita(Receita r) {
        validarLancamento(r.getOrigem(), "Origem", r.getValor(), r.getData());
        exigirConta(r.getContaId());
        receitaRepo.salvar(r);
    }

    public void atualizarReceita(Receita r) {
        validarLancamento(r.getOrigem(), "Origem", r.getValor(), r.getData());
        exigirConta(r.getContaId());
        receitaRepo.atualizar(r);
    }

    public void excluirReceita(int id)    { receitaRepo.excluir(id); }
    public List<Receita> getReceitas()    { return receitaRepo.listarTodos(); }

    // --- Despesas ---

    public void salvarDespesa(Despesa d) {
        validarLancamento(d.getDetalhamento(), "Detalhamento", d.getValor(), d.getData());
        exigirConta(d.getContaId());
        exigirCategoria(d.getCategoriaId());
        despesaRepo.salvar(d);
    }

    public void atualizarDespesa(Despesa d) {
        validarLancamento(d.getDetalhamento(), "Detalhamento", d.getValor(), d.getData());
        exigirConta(d.getContaId());
        exigirCategoria(d.getCategoriaId());
        despesaRepo.atualizar(d);
    }

    public void excluirDespesa(int id)    { despesaRepo.excluir(id); }
    public List<Despesa> getDespesas()    { return despesaRepo.listarTodos(); }

    /** Despesas do ano (ou de um mês dele), opcionalmente de uma categoria. */
    public List<Despesa> pesquisarDespesas(Integer categoriaId, int ano, String mes) {
        return despesaRepo.pesquisar(categoriaId, Periodo.de(ano, mes));
    }

    // --- Investimentos ---

    public void salvarInvestimento(Investimento i) {
        validarLancamento(i.getTipo(), "Tipo", i.getValor(), i.getData());
        exigirConta(i.getContaId());
        investRepo.salvar(i);
    }

    /** Um investimento já resgatado não pode ser alterado: o lucro ou prejuízo dependem do valor aplicado. */
    public void atualizarInvestimento(Investimento i) {
        validarLancamento(i.getTipo(), "Tipo", i.getValor(), i.getData());
        exigirConta(i.getContaId());
        if (buscarInvestimento(i.getId()).isResgatado())
            throw new IllegalArgumentException("Este investimento já foi resgatado. Desfaça o resgate antes de alterar.");
        investRepo.atualizar(i);
    }

    /** Exclui o investimento e, se ele já foi resgatado, o lançamento de lucro ou prejuízo do resgate. */
    public void excluirInvestimento(int id) {
        Investimento i = buscarInvestimento(id);
        apagarResultado(i);
        investRepo.excluir(id);
    }

    public List<Investimento> getInvestimentos()   { return investRepo.listarTodos(); }

    /** Nome da categoria em que o prejuízo de um resgate é lançado (criada se o usuário ainda não tiver). */
    public static final String CATEGORIA_INVESTIMENTOS = "Investimentos";

    /**
     * Resgata o investimento inteiro. O valor aplicado volta para o disponível (devolução, não é renda) e só a
     * diferença vira lançamento, na mesma conta e na data do resgate: lucro como receita ou prejuízo como despesa
     * (categoria {@value #CATEGORIA_INVESTIMENTOS}). Sem diferença, nada é lançado.
     *
     * @return o lucro (positivo), o prejuízo (negativo) ou zero
     */
    public BigDecimal resgatarInvestimento(int id, LocalDate data, BigDecimal valorSacado) {
        Investimento i = buscarInvestimento(id);
        if (i.isResgatado()) throw new IllegalArgumentException("Este investimento já foi resgatado.");
        if (data == null) throw new IllegalArgumentException("Informe a data do resgate.");
        if (data.isBefore(i.getData())) throw new IllegalArgumentException("O resgate não pode ser antes da aplicação.");
        if (data.isAfter(LocalDate.now())) throw new IllegalArgumentException("O resgate não pode ser numa data futura.");
        if (valorSacado == null || valorSacado.signum() < 0)
            throw new IllegalArgumentException("Informe o valor sacado (zero ou mais).");
        BigDecimal sacado = Dinheiro.arredondar(valorSacado);
        BigDecimal resultado = sacado.subtract(i.getValor());

        Integer receitaId = null, despesaId = null;
        if (resultado.signum() > 0)
            receitaId = receitaRepo.salvar(new Receita("Lucro do investimento: " + i.getTipo(), resultado, i.getContaId(), data));
        else if (resultado.signum() < 0)
            despesaId = despesaRepo.salvar(new Despesa(categoriaInvestimentos(), "Prejuízo do investimento: " + i.getTipo(),
                    resultado.negate(), i.getContaId(), data));
        try {
            investRepo.registrarResgate(id, data, sacado, receitaId, despesaId);
        } catch (RuntimeException e) {
            // não deixa um lucro ou prejuízo lançado sem o resgate gravado
            if (receitaId != null) receitaRepo.excluir(receitaId);
            if (despesaId != null) despesaRepo.excluir(despesaId);
            throw e;
        }
        return resultado;
    }

    /** Desfaz o resgate: apaga o lançamento de lucro ou prejuízo e o investimento volta a ficar guardado. */
    public void desfazerResgate(int id) {
        Investimento i = buscarInvestimento(id);
        if (!i.isResgatado()) throw new IllegalArgumentException("Este investimento não foi resgatado.");
        apagarResultado(i);
        investRepo.limparResgate(id);
    }

    private Investimento buscarInvestimento(int id) {
        return getInvestimentos().stream().filter(x -> x.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Investimento não encontrado."));
    }

    /** Apaga o lançamento de resultado do resgate, se ainda existir (o usuário pode tê-lo excluído à mão). */
    private void apagarResultado(Investimento i) {
        if (i.getReceitaResultadoId() != null && getReceitas().stream().anyMatch(r -> r.getId() == i.getReceitaResultadoId()))
            receitaRepo.excluir(i.getReceitaResultadoId());
        if (i.getDespesaResultadoId() != null && getDespesas().stream().anyMatch(d -> d.getId() == i.getDespesaResultadoId()))
            despesaRepo.excluir(i.getDespesaResultadoId());
    }

    /** Id da categoria {@value #CATEGORIA_INVESTIMENTOS} do usuário, criando-a se não existir. */
    private int categoriaInvestimentos() {
        return getCategorias().stream().filter(c -> c.getNome().equalsIgnoreCase(CATEGORIA_INVESTIMENTOS)).findFirst()
                .orElseGet(() -> {
                    categoriaRepo.salvar(new Categoria(CATEGORIA_INVESTIMENTOS));
                    return getCategorias().stream().filter(c -> c.getNome().equalsIgnoreCase(CATEGORIA_INVESTIMENTOS))
                            .findFirst().orElseThrow();
                }).getId();
    }

    // --- Lançamentos Fixos ---

    public void salvarLancamentoFixo(LancamentoFixo lf) {
        validarFixo(lf);
        exigirDonoDoFixo(lf);
        fixoRepo.salvar(lf);
    }

    public void atualizarLancamentoFixo(LancamentoFixo lf) {
        validarFixo(lf);
        exigirDonoDoFixo(lf);
        fixoRepo.atualizar(lf);
    }

    public void excluirLancamentoFixo(int id)              { fixoRepo.excluir(id); }
    public void alternarAtivoFixo(int id)                  { fixoRepo.alternarAtivo(id); }
    public List<LancamentoFixo> getLancamentosFixos()      { return fixoRepo.listarTodos(); }
    public List<LancamentoFixo> getLancamentosFixosAtivos() { return fixoRepo.listarAtivos(); }

    public int aplicarFixosMes(String mesInformado, int ano) {
        YearMonth ym = YearMonth.of(ano, Meses.numero(mesInformado));
        String mes = Meses.nome(ym.getMonthValue());
        int aplicados = 0;
        for (LancamentoFixo lf : fixoRepo.listarAtivos()) {
            if (fixoRepo.jaAplicado(lf.getId(), mes, ano)) continue;
            LocalDate data = ym.atDay(Math.min(lf.getDiaVencimento(), ym.lengthOfMonth()));
            switch (lf.getTipo()) {
                case RECEITA -> salvarReceita(new Receita(lf.getDescricao(), lf.getValor(), lf.getContaId(), data));
                case DESPESA -> salvarDespesa(new Despesa(lf.getCategoriaId(), lf.getDescricao(), lf.getValor(), lf.getContaId(), data));
                case INVESTIMENTO -> salvarInvestimento(new Investimento(lf.getDescricao(), lf.getValor(), lf.getContaId(), data));
            }
            fixoRepo.registrarAplicacao(lf.getId(), mes, ano);
            aplicados++;
        }
        return aplicados;
    }

    public int aplicarFixosIntervalo(String mesInicio, int anoInicio, String mesFim, int anoFim) {
        YearMonth atual = YearMonth.of(anoInicio, Meses.numero(mesInicio));
        YearMonth fim = YearMonth.of(anoFim, Meses.numero(mesFim));
        if (atual.isAfter(fim))
            throw new IllegalArgumentException("O mês inicial deve ser anterior ou igual ao mês final.");
        int total = 0;
        for (; !atual.isAfter(fim); atual = atual.plusMonths(1))
            total += aplicarFixosMes(Meses.nome(atual.getMonthValue()), atual.getYear());
        return total;
    }

    // --- Importação CSV ---

    public ResultadoLeituraCsv lerCsv(String caminho, int contaId) throws IOException {
        return importacao.ler(caminho, contaId);
    }

    public int confirmarImportacao(List<LinhaImportacao> linhas, int contaId) {
        return importacao.confirmar(linhas, contaId);
    }

    // --- Relatórios ---

    /** Totais do ano inteiro ({@code mes} = "Todos") ou de um mês. */
    public TotaisPeriodo totais(int ano, String mes) {
        Periodo p = Periodo.de(ano, mes);
        return new TotaisPeriodo(
                somar(receitaRepo.listarPorPeriodo(p), Receita::getValor),
                somar(despesaRepo.listarPorPeriodo(p), Despesa::getValor),
                somar(investRepo.listarPorPeriodo(p), Investimento::getValor),
                somar(investRepo.listarResgatadosNoPeriodo(p), Investimento::getValor));
    }

    /** Saldo geral: soma dos saldos de todas as contas (saldos iniciais + todos os lançamentos). */
    public BigDecimal saldoTotal() {
        return saldosPorConta().stream().map(SaldoConta::saldo).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    /** Saldo atual de cada conta: saldo inicial + receitas − despesas − aplicado + resgatado, de todo o histórico. */
    public List<SaldoConta> saldosPorConta() {
        List<Investimento> todos = getInvestimentos();
        return montarSaldos(getReceitas(), getDespesas(), todos,
                todos.stream().filter(Investimento::isResgatado).toList(), true);
    }

    /** Quanto está guardado em investimentos (aplicações ainda não resgatadas). */
    public BigDecimal guardadoEmInvestimentos() {
        return somar(getInvestimentos().stream().filter(i -> !i.isResgatado()).toList(), Investimento::getValor);
    }

    /** Movimento de cada conta no ano (ou mês) — sem saldo inicial; contas sem lançamentos no período ficam de fora. */
    public List<SaldoConta> movimentoPorConta(int ano, String mes) {
        Periodo p = Periodo.de(ano, mes);
        return montarSaldos(receitaRepo.listarPorPeriodo(p), despesaRepo.listarPorPeriodo(p),
                investRepo.listarPorPeriodo(p), investRepo.listarResgatadosNoPeriodo(p), false).stream()
                .filter(s -> s.receitas().signum() != 0 || s.despesas().signum() != 0 || s.investimentos().signum() != 0
                        || s.resgatados().signum() != 0)
                .toList();
    }

    /** @param resgatados investimentos cujo aplicado voltou para a conta (pela data do resgate) */
    private List<SaldoConta> montarSaldos(List<Receita> receitas, List<Despesa> despesas, List<Investimento> investimentos,
                                          List<Investimento> resgatados, boolean comSaldoInicial) {
        Map<Integer, BigDecimal> rec = somarPorConta(receitas, Receita::getContaId, Receita::getValor);
        Map<Integer, BigDecimal> desp = somarPorConta(despesas, Despesa::getContaId, Despesa::getValor);
        Map<Integer, BigDecimal> inv = somarPorConta(investimentos, Investimento::getContaId, Investimento::getValor);
        Map<Integer, BigDecimal> res = somarPorConta(resgatados, Investimento::getContaId, Investimento::getValor);
        return getContas().stream().map(c -> new SaldoConta(c,
                        comSaldoInicial ? c.getSaldoInicial() : Dinheiro.ZERO,
                        rec.getOrDefault(c.getId(), Dinheiro.ZERO),
                        desp.getOrDefault(c.getId(), Dinheiro.ZERO),
                        inv.getOrDefault(c.getId(), Dinheiro.ZERO),
                        res.getOrDefault(c.getId(), Dinheiro.ZERO)))
                .toList();
    }

    private static <T> Map<Integer, BigDecimal> somarPorConta(List<T> itens, Function<T, Integer> conta,
                                                             Function<T, BigDecimal> valor) {
        Map<Integer, BigDecimal> r = new HashMap<>();
        for (T t : itens) r.merge(conta.apply(t), valor.apply(t), BigDecimal::add);
        return r;
    }

    public Map<String, BigDecimal> divisaoReceitasPorOrigem(int ano, String mes) {
        return agruparOrdenado(receitaRepo.listarPorPeriodo(Periodo.de(ano, mes)), Receita::getOrigem, Receita::getValor);
    }

    public Map<String, BigDecimal> divisaoGastosPorCategoria(int ano, String mes) {
        return agruparOrdenado(despesaRepo.listarPorPeriodo(Periodo.de(ano, mes)), Despesa::getCategoriaNome, Despesa::getValor);
    }

    public Map<String, BigDecimal> divisaoInvestimentosPorTipo(int ano, String mes) {
        return agruparOrdenado(investRepo.listarPorPeriodo(Periodo.de(ano, mes)), Investimento::getTipo, Investimento::getValor);
    }

    /** Doze resumos (janeiro a dezembro) com saldo e saldo acumulado; faz 4 consultas ao banco. */
    public List<ResumoMensal> gerarResumoAnual(int ano) {
        Periodo p = Periodo.doAno(ano);
        Map<Integer, BigDecimal> rec = somarPorMes(receitaRepo.listarPorPeriodo(p), Receita::getData, Receita::getValor);
        Map<Integer, BigDecimal> inv = somarPorMes(investRepo.listarPorPeriodo(p), Investimento::getData, Investimento::getValor);
        Map<Integer, BigDecimal> res = somarPorMes(investRepo.listarResgatadosNoPeriodo(p), Investimento::getDataResgate,
                Investimento::getValor);
        List<Despesa> despesas = despesaRepo.listarPorPeriodo(p);
        Map<Integer, BigDecimal> desp = somarPorMes(despesas, Despesa::getData, Despesa::getValor);

        List<ResumoMensal> resumos = new ArrayList<>();
        BigDecimal acumulado = Dinheiro.ZERO;
        for (int m = 1; m <= 12; m++) {
            ResumoMensal rm = new ResumoMensal(Meses.nome(m));
            rm.setReceita(rec.getOrDefault(m, Dinheiro.ZERO));
            rm.setInvestimentos(inv.getOrDefault(m, Dinheiro.ZERO));
            rm.setResgatados(res.getOrDefault(m, Dinheiro.ZERO));
            rm.setDespesaTotal(desp.getOrDefault(m, Dinheiro.ZERO));
            BigDecimal saldo = rm.getReceita().subtract(rm.getDespesaTotal()).subtract(rm.getInvestimentos()).add(rm.getResgatados());
            rm.setSaldo(saldo);
            acumulado = acumulado.add(saldo);
            rm.setSaldoAcumulado(acumulado);
            resumos.add(rm);
        }
        for (Despesa d : despesas)
            resumos.get(d.getData().getMonthValue() - 1).getDespesasPorCategoria()
                    .merge(d.getCategoriaNome(), d.getValor(), BigDecimal::add);
        return resumos;
    }

    public List<Integer> anosDisponiveis() {
        Set<Integer> anos = new TreeSet<>(receitaRepo.anos());
        anos.addAll(despesaRepo.anos());
        anos.addAll(investRepo.anos());
        return new ArrayList<>(anos);
    }

    // --- Auxiliares ---

    private static <T> BigDecimal somar(List<T> itens, Function<T, BigDecimal> valor) {
        return itens.stream().map(valor).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private static <T> Map<Integer, BigDecimal> somarPorMes(List<T> itens, Function<T, LocalDate> data,
                                                           Function<T, BigDecimal> valor) {
        Map<Integer, BigDecimal> r = new HashMap<>();
        for (T t : itens) r.merge(data.apply(t).getMonthValue(), valor.apply(t), BigDecimal::add);
        return r;
    }

    /** Soma por chave, do maior para o menor valor. */
    private static <T> Map<String, BigDecimal> agruparOrdenado(List<T> itens, Function<T, String> chave,
                                                              Function<T, BigDecimal> valor) {
        return itens.stream()
                .collect(Collectors.groupingBy(chave, Collectors.reducing(Dinheiro.ZERO, valor, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Impede gravar lançamentos em conta ou categoria de outro usuário (ou inexistente). */
    private void exigirConta(int contaId) {
        if (!contaRepo.pertence(contaId)) throw new IllegalArgumentException("Conta inválida. Selecione uma das suas contas.");
    }

    private void exigirCategoria(int categoriaId) {
        if (!categoriaRepo.pertence(categoriaId))
            throw new IllegalArgumentException("Categoria inválida. Selecione uma das suas categorias.");
    }

    private void exigirDonoDoFixo(LancamentoFixo lf) {
        exigirConta(lf.getContaId());
        if (lf.getTipo() == LancamentoFixo.Tipo.DESPESA) exigirCategoria(lf.getCategoriaId());
    }

    private static void validarLancamento(String texto, String campo, BigDecimal valor, LocalDate data) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException(campo + " é obrigatório.");
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("Valor deve ser maior que zero.");
        if (data == null) throw new IllegalArgumentException("Selecione uma data.");
    }

    private static <T> T validarNome(String nome, T item) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório.");
        return item;
    }

    private static void validarFixo(LancamentoFixo lf) {
        if (lf.getDescricao() == null || lf.getDescricao().isBlank())
            throw new IllegalArgumentException("Descrição obrigatória.");
        if (lf.getTipo() == LancamentoFixo.Tipo.DESPESA && lf.getCategoriaId() <= 0)
            throw new IllegalArgumentException("Lançamento fixo de despesa precisa de uma categoria.");
        if (lf.getValor().signum() <= 0)
            throw new IllegalArgumentException("Valor deve ser maior que zero.");
        if (lf.getDiaVencimento() < 1 || lf.getDiaVencimento() > 31)
            throw new IllegalArgumentException("Dia do mês deve estar entre 1 e 31.");
    }
}
