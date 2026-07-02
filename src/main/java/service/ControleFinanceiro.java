package service;

import model.*;
import model.LancamentoFixo.Tipo;
import repository.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ControleFinanceiro {

    public static final List<String> MESES = List.of(
        "JANEIRO","FEVEREIRO","MARÇO","ABRIL","MAIO","JUNHO",
        "JULHO","AGOSTO","SETEMBRO","OUTUBRO","NOVEMBRO","DEZEMBRO");

    private final CategoriaRepository     categoriaRepo = new CategoriaRepository();
    private final ContaRepository         contaRepo     = new ContaRepository();
    private final ReceitaRepository       receitaRepo   = new ReceitaRepository();
    private final DespesaRepository       despesaRepo   = new DespesaRepository();
    private final InvestimentoRepository  investRepo    = new InvestimentoRepository();
    private final LancamentoFixoRepository fixoRepo     = new LancamentoFixoRepository();
    private final UsuarioRepository       usuarioRepo   = new UsuarioRepository();

    // --- Usuário ---
    public Usuario getUsuario()              { return usuarioRepo.buscarPrincipal(); }
    public void atualizarUsuario(Usuario u)  { usuarioRepo.atualizar(u); }

    // --- Categorias ---
    public void salvarCategoria(Categoria c)    { categoriaRepo.salvar(c); }
    public void atualizarCategoria(Categoria c) { categoriaRepo.atualizar(c); }
    public void excluirCategoria(int id)        { categoriaRepo.excluir(id); }
    public List<Categoria> getCategorias()      { return categoriaRepo.listarTodos(); }

    // --- Contas ---
    public void salvarConta(Conta c)            { contaRepo.salvar(c); }
    public void atualizarConta(Conta c)         { contaRepo.atualizar(c); }
    public void excluirConta(int id)            { contaRepo.excluir(id); }
    public List<Conta> getContas()              { return contaRepo.listarTodos(); }

    // --- Receitas ---
    public void salvarReceita(Receita r)        { receitaRepo.salvar(r); }
    public void atualizarReceita(Receita r)     { receitaRepo.atualizar(r); }
    public void excluirReceita(int id)          { receitaRepo.excluir(id); }
    public List<Receita> getReceitas()          { return receitaRepo.listarTodos(); }

    // --- Despesas ---
    public void salvarDespesa(Despesa d)        { despesaRepo.salvar(d); }
    public void atualizarDespesa(Despesa d)     { despesaRepo.atualizar(d); }
    public void excluirDespesa(int id)          { despesaRepo.excluir(id); }
    public List<Despesa> getDespesas()          { return despesaRepo.listarTodos(); }

    // --- Investimentos ---
    public void salvarInvestimento(Investimento i)    { investRepo.salvar(i); }
    public void atualizarInvestimento(Investimento i) { investRepo.atualizar(i); }
    public void excluirInvestimento(int id)           { investRepo.excluir(id); }
    public List<Investimento> getInvestimentos()      { return investRepo.listarTodos(); }

    // --- Lançamentos Fixos ---
    public void salvarLancamentoFixo(LancamentoFixo lf)    { fixoRepo.salvar(lf); }
    public void atualizarLancamentoFixo(LancamentoFixo lf) { fixoRepo.atualizar(lf); }
    public void excluirLancamentoFixo(int id)              { fixoRepo.excluir(id); }
    public void alternarAtivoFixo(int id)                  { fixoRepo.alternarAtivo(id); }
    public List<LancamentoFixo> getLancamentosFixos()      { return fixoRepo.listarTodos(); }
    public List<LancamentoFixo> getLancamentosFixosAtivos(){ return fixoRepo.listarAtivos(); }

    public int aplicarFixosMes(String mes, int ano) {
        int aplicados = 0;
        for (LancamentoFixo lf : fixoRepo.listarAtivos()) {
            if (fixoRepo.jaAplicado(lf.getId(), mes, ano)) continue;
            YearMonth ym = YearMonth.of(ano, MESES.indexOf(mes.toUpperCase()) + 1);
            int dia = Math.min(lf.getDiaVencimento(), ym.lengthOfMonth());
            LocalDate data = LocalDate.of(ano, ym.getMonthValue(), dia);
            switch (lf.getTipo()) {
                case RECEITA      -> salvarReceita(new Receita(lf.getDescricao(), lf.getValor(), lf.getContaId(), data, mes, ano));
                case DESPESA      -> salvarDespesa(new Despesa(lf.getCategoriaId(), lf.getDescricao(), lf.getValor(), lf.getContaId(), data, mes, ano));
                case INVESTIMENTO -> salvarInvestimento(new Investimento(lf.getDescricao(), lf.getValor(), lf.getContaId(), data, mes, ano));
            }
            fixoRepo.registrarAplicacao(lf.getId(), mes, ano);
            aplicados++;
        }
        return aplicados;
    }

    // --- Cálculos ---

    public double somarReceitas(String mes, int ano) {
        return receitaRepo.listarPorAno(ano).stream()
            .filter(r -> r.getMes().equalsIgnoreCase(mes)).mapToDouble(Receita::getValor).sum();
    }

    public double somarReceitasAno(int ano) {
        return receitaRepo.listarPorAno(ano).stream().mapToDouble(Receita::getValor).sum();
    }

    public double somarInvestimentos(String mes, int ano) {
        return investRepo.listarPorAno(ano).stream()
            .filter(i -> i.getMes().equalsIgnoreCase(mes)).mapToDouble(Investimento::getValor).sum();
    }

    public double somarInvestimentosAno(int ano) {
        return investRepo.listarPorAno(ano).stream().mapToDouble(Investimento::getValor).sum();
    }

    public double somarDespesas(String nomeCategoria, String mes, int ano) {
        return despesaRepo.listarPorAno(ano).stream()
            .filter(d -> d.getCategoriaNome().equalsIgnoreCase(nomeCategoria) && d.getMes().equalsIgnoreCase(mes))
            .mapToDouble(Despesa::getValor).sum();
    }

    public double somarDespesasCategoriaAno(String nomeCategoria, int ano) {
        return despesaRepo.listarPorAno(ano).stream()
            .filter(d -> d.getCategoriaNome().equalsIgnoreCase(nomeCategoria))
            .mapToDouble(Despesa::getValor).sum();
    }

    public double somarTotalDespesas(String mes, int ano) {
        return despesaRepo.listarPorAno(ano).stream()
            .filter(d -> d.getMes().equalsIgnoreCase(mes)).mapToDouble(Despesa::getValor).sum();
    }

    public double somarTotalDespesasAno(int ano) {
        return despesaRepo.listarPorAno(ano).stream().mapToDouble(Despesa::getValor).sum();
    }

    public double saldoTotal() {
        double rec  = getReceitas().stream().mapToDouble(Receita::getValor).sum();
        double desp = getDespesas().stream().mapToDouble(Despesa::getValor).sum();
        double inv  = getInvestimentos().stream().mapToDouble(Investimento::getValor).sum();
        return rec - desp - inv;
    }

    private boolean todos(String mes) { return "Todos".equalsIgnoreCase(mes); }

    public double porcentagemRendaGasta(int ano, String mes) {
        double rec  = todos(mes) ? somarReceitasAno(ano)      : somarReceitas(mes, ano);
        double desp = todos(mes) ? somarTotalDespesasAno(ano) : somarTotalDespesas(mes, ano);
        return rec == 0 ? 0 : desp / rec;
    }

    public double porcentagemRendaInvestida(int ano, String mes) {
        double rec = todos(mes) ? somarReceitasAno(ano)      : somarReceitas(mes, ano);
        double inv = todos(mes) ? somarInvestimentosAno(ano) : somarInvestimentos(mes, ano);
        return rec == 0 ? 0 : -inv / rec;
    }

    public double saldoEmConta(int ano, String mes) {
        double rec  = todos(mes) ? somarReceitasAno(ano)      : somarReceitas(mes, ano);
        double desp = todos(mes) ? somarTotalDespesasAno(ano) : somarTotalDespesas(mes, ano);
        double inv  = todos(mes) ? somarInvestimentosAno(ano) : somarInvestimentos(mes, ano);
        return rec - desp - inv;
    }

    // --- Divisões ---

    public Map<String,Double> divisaoReceitasPorOrigem(int ano, String mes) {
        Map<String,Double> r = new LinkedHashMap<>();
        receitaRepo.listarPorAno(ano).stream()
            .filter(x -> todos(mes) || x.getMes().equalsIgnoreCase(mes))
            .collect(Collectors.groupingBy(Receita::getOrigem, Collectors.summingDouble(Receita::getValor)))
            .entrySet().stream().sorted(Map.Entry.<String,Double>comparingByValue().reversed())
            .forEach(e -> r.put(e.getKey(), e.getValue()));
        return r;
    }

    public Map<String,Double> divisaoGastosPorCategoria(int ano, String mes) {
        Map<String,Double> r = new LinkedHashMap<>();
        getCategorias().forEach(cat -> {
            double v = todos(mes) ? somarDespesasCategoriaAno(cat.getNome(), ano)
                                  : somarDespesas(cat.getNome(), mes, ano);
            if (v > 0) r.put(cat.getNome(), v);
        });
        return r;
    }

    public Map<String,Double> divisaoInvestimentosPorTipo(int ano, String mes) {
        Map<String,Double> r = new LinkedHashMap<>();
        investRepo.listarPorAno(ano).stream()
            .filter(x -> todos(mes) || x.getMes().equalsIgnoreCase(mes))
            .collect(Collectors.groupingBy(Investimento::getTipo, Collectors.summingDouble(Investimento::getValor)))
            .entrySet().stream().sorted(Map.Entry.<String,Double>comparingByValue().reversed())
            .forEach(e -> r.put(e.getKey(), e.getValue()));
        return r;
    }

    // --- Resumo anual ---

    public List<ResumoMensal> gerarResumoAnual(int ano) {
        List<ResumoMensal> resumos = new ArrayList<>();
        double saldoAcum = 0;
        for (String mes : MESES) {
            ResumoMensal rm = new ResumoMensal(mes);
            double rec  = somarReceitas(mes, ano);
            double inv  = somarInvestimentos(mes, ano);
            double desp = somarTotalDespesas(mes, ano);
            rm.setReceita(rec); rm.setInvestimentos(inv); rm.setDespesaTotal(desp);
            rm.setAlimentacao(somarDespesas("Alimentação", mes, ano));
            rm.setMoradia    (somarDespesas("Moradia",      mes, ano));
            rm.setEducacao   (somarDespesas("Educação",     mes, ano));
            rm.setPet        (somarDespesas("Pet",          mes, ano));
            rm.setSaude      (somarDespesas("Saúde",        mes, ano));
            rm.setTransporte (somarDespesas("Transporte",   mes, ano));
            rm.setPessoais   (somarDespesas("Pessoais",     mes, ano));
            rm.setLazer      (somarDespesas("Lazer",        mes, ano));
            rm.setFinanceiros(somarDespesas("Financeiros",  mes, ano));
            double saldo = rec + inv - desp;
            rm.setSaldo(saldo);
            saldoAcum += saldo;
            rm.setSaldoAcumulado(saldoAcum);
            resumos.add(rm);
        }
        return resumos;
    }

    public List<Integer> anosDisponiveis() {
        Set<Integer> anos = new TreeSet<>();
        getReceitas().forEach(r -> anos.add(r.getAno()));
        getDespesas().forEach(d -> anos.add(d.getAno()));
        getInvestimentos().forEach(i -> anos.add(i.getAno()));
        return new ArrayList<>(anos);
    }
}
