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

    private final CategoriaRepository categoriaRepo = new CategoriaRepository();
    private final ContaRepository contaRepo = new ContaRepository();
    private final ReceitaRepository receitaRepo = new ReceitaRepository();
    private final DespesaRepository despesaRepo = new DespesaRepository();
    private final InvestimentoRepository investRepo = new InvestimentoRepository();
    private final LancamentoFixoRepository fixoRepo = new LancamentoFixoRepository();
    private final UsuarioRepository usuarioRepo = new UsuarioRepository();
    private final ImportacaoCsv importacao = new ImportacaoCsv();

    // --- Usuário ---

    public Usuario getUsuario() {
        return usuarioRepo.buscarPrincipal();
    }

    public void atualizarUsuario(String nome, LocalDate dataNascimento) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Informe o nome.");
        if (dataNascimento != null && dataNascimento.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Data de nascimento no futuro.");
        usuarioRepo.atualizar(new Usuario(getUsuario().getId(), nome.trim(), dataNascimento));
    }

    /** Nível e XP calculados a partir de todos os lançamentos (ver {@link CalculadoraXp}). */
    public Progresso getProgresso() {
        return CalculadoraXp.calcular(getReceitas(), getDespesas(), getInvestimentos(), LocalDate.now());
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
        receitaRepo.salvar(r);
    }

    public void atualizarReceita(Receita r) {
        validarLancamento(r.getOrigem(), "Origem", r.getValor(), r.getData());
        receitaRepo.atualizar(r);
    }

    public void excluirReceita(int id)    { receitaRepo.excluir(id); }
    public List<Receita> getReceitas()    { return receitaRepo.listarTodos(); }

    // --- Despesas ---

    public void salvarDespesa(Despesa d) {
        validarLancamento(d.getDetalhamento(), "Detalhamento", d.getValor(), d.getData());
        despesaRepo.salvar(d);
    }

    public void atualizarDespesa(Despesa d) {
        validarLancamento(d.getDetalhamento(), "Detalhamento", d.getValor(), d.getData());
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
        investRepo.salvar(i);
    }

    public void atualizarInvestimento(Investimento i) {
        validarLancamento(i.getTipo(), "Tipo", i.getValor(), i.getData());
        investRepo.atualizar(i);
    }

    public void excluirInvestimento(int id)        { investRepo.excluir(id); }
    public List<Investimento> getInvestimentos()   { return investRepo.listarTodos(); }

    // --- Lançamentos Fixos ---

    public void salvarLancamentoFixo(LancamentoFixo lf) {
        validarFixo(lf);
        fixoRepo.salvar(lf);
    }

    public void atualizarLancamentoFixo(LancamentoFixo lf) {
        validarFixo(lf);
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
                somar(investRepo.listarPorPeriodo(p), Investimento::getValor));
    }

    /** Saldo de todo o histórico: receitas − despesas − investimentos. */
    public BigDecimal saldoTotal() {
        return somar(getReceitas(), Receita::getValor)
                .subtract(somar(getDespesas(), Despesa::getValor))
                .subtract(somar(getInvestimentos(), Investimento::getValor));
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

    /** Doze resumos (janeiro a dezembro) com saldo e saldo acumulado; faz 3 consultas ao banco. */
    public List<ResumoMensal> gerarResumoAnual(int ano) {
        Periodo p = Periodo.doAno(ano);
        Map<Integer, BigDecimal> rec = somarPorMes(receitaRepo.listarPorPeriodo(p), Receita::getData, Receita::getValor);
        Map<Integer, BigDecimal> inv = somarPorMes(investRepo.listarPorPeriodo(p), Investimento::getData, Investimento::getValor);
        List<Despesa> despesas = despesaRepo.listarPorPeriodo(p);
        Map<Integer, BigDecimal> desp = somarPorMes(despesas, Despesa::getData, Despesa::getValor);

        List<ResumoMensal> resumos = new ArrayList<>();
        BigDecimal acumulado = Dinheiro.ZERO;
        for (int m = 1; m <= 12; m++) {
            ResumoMensal rm = new ResumoMensal(Meses.nome(m));
            rm.setReceita(rec.getOrDefault(m, Dinheiro.ZERO));
            rm.setInvestimentos(inv.getOrDefault(m, Dinheiro.ZERO));
            rm.setDespesaTotal(desp.getOrDefault(m, Dinheiro.ZERO));
            BigDecimal saldo = rm.getReceita().subtract(rm.getDespesaTotal()).subtract(rm.getInvestimentos());
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
