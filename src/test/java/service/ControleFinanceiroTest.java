package service;

import model.*;
import model.LancamentoFixo.Tipo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static util.Assercoes.assertValor;
import static util.Assercoes.v;

class ControleFinanceiroTest {

    private int usuarioId;

    private ControleFinanceiro cf;
    private int contaId;
    private int catId;

    @BeforeEach
    void setUp() {
        usuarioId = DatabaseTestHelper.setup();
        cf = new ControleFinanceiro(usuarioId);
        cf.salvarConta(new Conta("Nubank"));
        contaId = cf.getContas().get(0).getId();
        catId = categoria("Moradia");
    }

    private int categoria(String nome) {
        return cf.getCategorias().stream().filter(c -> c.getNome().equals(nome)).findFirst().orElseThrow().getId();
    }

    private void receita(String origem, String valor, LocalDate data) {
        cf.salvarReceita(new Receita(origem, v(valor), contaId, data));
    }

    private void despesa(String detalhe, String valor, LocalDate data) {
        cf.salvarDespesa(new Despesa(catId, detalhe, v(valor), contaId, data));
    }

    private void investimento(String tipo, String valor, LocalDate data) {
        cf.salvarInvestimento(new Investimento(tipo, v(valor), contaId, data));
    }

    // --- Totais ---

    @Test
    void deveSomarReceitasPorMesEAno() {
        receita("Salário", "5000.00", LocalDate.of(2025, 6, 5));
        receita("Freelance", "1000.00", LocalDate.of(2025, 6, 15));
        receita("Bônus", "2000.00", LocalDate.of(2025, 7, 1));

        assertValor("6000.00", cf.totais(2025, "JUNHO").receitas());
        assertValor("2000.00", cf.totais(2025, "julho").receitas());
        assertValor("8000.00", cf.totais(2025, "Todos").receitas());
    }

    @Test
    void deveSomarDespesasEInvestimentos() {
        despesa("Condomínio", "850.00", LocalDate.of(2025, 6, 10));
        despesa("Água", "80.00", LocalDate.of(2025, 6, 15));
        investimento("Poupança", "500.00", LocalDate.of(2025, 6, 1));
        investimento("Ações", "1000.00", LocalDate.of(2025, 7, 1));

        assertValor("930.00", cf.totais(2025, "JUNHO").despesas());
        assertValor("500.00", cf.totais(2025, "JUNHO").investimentos());
        assertValor("1500.00", cf.totais(2025, "Todos").investimentos());
    }

    @Test
    void deveSomarCentavosSemErroDeArredondamento() {
        for (int i = 0; i < 10; i++) despesa("Café", "0.10", LocalDate.of(2025, 6, 1));
        assertEquals(v("1.00"), cf.totais(2025, "JUNHO").despesas());
    }

    @Test
    void deveCalcularSaldoEPercentuais() {
        receita("Salário", "5000.00", LocalDate.of(2025, 6, 5));
        despesa("Condomínio", "1000.00", LocalDate.of(2025, 6, 10));
        investimento("Poupança", "500.00", LocalDate.of(2025, 6, 1));

        TotaisPeriodo t = cf.totais(2025, "JUNHO");
        assertValor("3500.00", t.saldo());
        assertValor("0.20", t.percentualGasto());
        assertValor("0.10", t.percentualInvestido());
    }

    @Test
    void deveRetornarZeroSemDadosOuSemReceita() {
        assertValor("0", cf.totais(2025, "JUNHO").saldo());
        despesa("Supermercado", "200.00", LocalDate.of(2025, 6, 1));
        assertValor("0", cf.totais(2025, "JUNHO").percentualGasto());
    }

    @Test
    void deveCalcularPercentuaisDoAnoInteiro() {
        receita("Salário", "4000.00", LocalDate.of(2025, 6, 5));
        receita("Freelance", "2000.00", LocalDate.of(2025, 7, 5));
        despesa("Aluguel", "1500.00", LocalDate.of(2025, 6, 10));
        investimento("Poupança", "500.00", LocalDate.of(2025, 7, 10));

        TotaisPeriodo t = cf.totais(2025, "Todos");
        assertValor("0.25", t.percentualGasto());
        assertValor("0.0833", t.percentualInvestido());
        assertValor("4000.00", t.saldo());
    }

    @Test
    void deveCalcularSaldoTotal() {
        receita("Salário", "5000.00", LocalDate.of(2024, 6, 5));
        despesa("Aluguel", "1000.00", LocalDate.of(2025, 6, 10));
        investimento("Poupança", "500.00", LocalDate.of(2025, 6, 15));
        assertValor("3500.00", cf.saldoTotal());
    }

    // --- Saldo por conta ---

    @Test
    void deveCalcularSaldoPorContaComSaldoInicial() {
        cf.salvarConta(new Conta(0, "Itaú", v("1000.00")));
        int itau = cf.getContas().stream().filter(c -> c.getNome().equals("Itaú")).findFirst().orElseThrow().getId();

        receita("Salário", "5000.00", LocalDate.of(2025, 6, 5));                       // Nubank
        despesa("Aluguel", "1500.00", LocalDate.of(2025, 6, 10));                     // Nubank
        cf.salvarDespesa(new Despesa(catId, "Condomínio", v("300.00"), itau, LocalDate.of(2025, 6, 10)));
        cf.salvarInvestimento(new Investimento("CDB", v("200.00"), itau, LocalDate.of(2025, 7, 1)));

        List<SaldoConta> saldos = cf.saldosPorConta();     // ordenado por nome: Itaú, Nubank
        SaldoConta s1 = saldos.get(0), s2 = saldos.get(1);
        assertEquals("Itaú", s1.getNome());
        assertValor("1000.00", s1.saldoInicial());
        assertValor("300.00", s1.despesas());
        assertValor("200.00", s1.investimentos());
        assertValor("500.00", s1.saldo());              // 1000 − 300 − 200
        assertValor("3500.00", s2.saldo());             // 5000 − 1500
        assertValor("4000.00", cf.saldoTotal());        // soma das contas, com o saldo inicial
    }

    @Test
    void movimentoPorContaDeveConsiderarSoOPeriodoESemSaldoInicial() {
        cf.salvarConta(new Conta(0, "Itaú", v("1000.00")));
        receita("Salário", "5000.00", LocalDate.of(2025, 6, 5));
        despesa("Aluguel", "1500.00", LocalDate.of(2025, 7, 10));

        List<SaldoConta> junho = cf.movimentoPorConta(2025, "JUNHO");
        assertEquals(1, junho.size(), "conta sem lançamentos no período fica de fora");
        assertEquals("Nubank", junho.get(0).getNome());
        assertValor("0", junho.get(0).saldoInicial());
        assertValor("5000.00", junho.get(0).movimento());
        assertValor("3500.00", cf.movimentoPorConta(2025, "Todos").get(0).movimento());
    }

    // --- Resumo anual ---

    @Test
    void deveGerarResumoAnualComSaldoAcumulado() {
        receita("Salário", "3000.00", LocalDate.of(2025, 1, 5));
        receita("Salário", "3000.00", LocalDate.of(2025, 2, 5));
        despesa("Aluguel", "1000.00", LocalDate.of(2025, 1, 10));

        List<ResumoMensal> resumo = cf.gerarResumoAnual(2025);
        assertEquals(12, resumo.size());
        assertValor("2000.00", resumo.get(0).getSaldo());
        assertValor("2000.00", resumo.get(0).getSaldoAcumulado());
        assertValor("3000.00", resumo.get(1).getSaldo());
        assertValor("5000.00", resumo.get(1).getSaldoAcumulado());
        assertFalse(resumo.get(2).temMovimento());
    }

    @Test
    void saldoDoResumoAnualDeveSerIgualAoSaldoEmConta() {
        receita("Salário", "5000.00", LocalDate.of(2025, 6, 5));
        despesa("Aluguel", "1000.00", LocalDate.of(2025, 6, 10));
        investimento("Poupança", "500.00", LocalDate.of(2025, 6, 1));

        ResumoMensal junho = cf.gerarResumoAnual(2025).get(5);
        assertValor("3500.00", junho.getSaldo());
        assertEquals(cf.totais(2025, "JUNHO").saldo(), junho.getSaldo());
    }

    @Test
    void resumoDeveTrazerDespesasPorCategoriaInclusiveCriadasPeloUsuario() {
        cf.salvarCategoria(new Categoria("Viagem"));
        int viagem = categoria("Viagem");
        despesa("Aluguel", "1000.00", LocalDate.of(2025, 6, 10));
        cf.salvarDespesa(new Despesa(viagem, "Passagem", v("800.00"), contaId, LocalDate.of(2025, 6, 20)));

        ResumoMensal junho = cf.gerarResumoAnual(2025).get(5);
        assertValor("1000.00", junho.getDespesaCategoria("Moradia"));
        assertValor("800.00", junho.getDespesaCategoria("Viagem"));
        assertValor("0", junho.getDespesaCategoria("Lazer"));
    }

    // --- Divisões ---

    @Test
    void deveDividirPorOrigemCategoriaETipoOrdenadoPorValor() {
        receita("Freelance", "1000.00", LocalDate.of(2025, 6, 10));
        receita("Salário", "5000.00", LocalDate.of(2025, 6, 5));
        despesa("Aluguel", "1000.00", LocalDate.of(2025, 6, 10));
        despesa("Condomínio", "500.00", LocalDate.of(2025, 6, 15));
        investimento("Poupança", "500.00", LocalDate.of(2025, 6, 1));
        investimento("Ações", "1000.00", LocalDate.of(2025, 6, 10));

        Map<String, BigDecimal> rec = cf.divisaoReceitasPorOrigem(2025, "JUNHO");
        assertEquals(List.of("Salário", "Freelance"), List.copyOf(rec.keySet()));
        assertValor("1500.00", cf.divisaoGastosPorCategoria(2025, "JUNHO").get("Moradia"));
        assertEquals(List.of("Ações", "Poupança"), List.copyOf(cf.divisaoInvestimentosPorTipo(2025, "Todos").keySet()));
    }

    // --- Lançamentos fixos ---

    @Test
    void deveAplicarFixosMesESerIdempotente() {
        cf.salvarLancamentoFixo(new LancamentoFixo(Tipo.RECEITA, "Salário", 0, v("5000.00"), contaId, 5));

        assertEquals(1, cf.aplicarFixosMes("JUNHO", 2025));
        assertEquals(0, cf.aplicarFixosMes("JUNHO", 2025));
        assertEquals(1, cf.getReceitas().size());
    }

    @Test
    void deveAplicarFixosDeDespesaEInvestimento() {
        cf.salvarLancamentoFixo(new LancamentoFixo(Tipo.DESPESA, "Aluguel", catId, v("1500.00"), contaId, 10));
        cf.salvarLancamentoFixo(new LancamentoFixo(Tipo.INVESTIMENTO, "Poupança", 0, v("500.00"), contaId, 10));

        assertEquals(2, cf.aplicarFixosMes("JUNHO", 2025));

        Despesa d = cf.getDespesas().get(0);
        assertEquals("Aluguel", d.getDetalhamento());
        assertValor("1500.00", d.getValor());
        assertEquals(LocalDate.of(2025, 6, 10), d.getData());
        assertEquals("Poupança", cf.getInvestimentos().get(0).getTipo());
    }

    @Test
    void deveAjustarVencimentoParaUltimoDiaDoMes() {
        cf.salvarLancamentoFixo(new LancamentoFixo(Tipo.RECEITA, "Salário", 0, v("5000.00"), contaId, 31));
        cf.aplicarFixosMes("FEVEREIRO", 2025);
        assertEquals(LocalDate.of(2025, 2, 28), cf.getReceitas().get(0).getData());
    }

    @Test
    void deveAplicarFixosEmIntervaloQueViraOAno() {
        cf.salvarLancamentoFixo(new LancamentoFixo(Tipo.RECEITA, "Salário", 0, v("5000.00"), contaId, 5));
        assertEquals(3, cf.aplicarFixosIntervalo("NOVEMBRO", 2024, "JANEIRO", 2025));
        assertThrows(IllegalArgumentException.class, () -> cf.aplicarFixosIntervalo("MARÇO", 2025, "JANEIRO", 2025));
    }

    @Test
    void naoDeveSalvarFixoDeDespesaSemCategoria() {
        assertThrows(IllegalArgumentException.class, () -> cf.salvarLancamentoFixo(
                new LancamentoFixo(Tipo.DESPESA, "Internet", 0, v("100.00"), contaId, 10)));
        assertTrue(cf.getLancamentosFixos().isEmpty());
    }

    @Test
    void deveAceitarMesEmMinusculasERejeitarMesInvalido() {
        cf.salvarLancamentoFixo(new LancamentoFixo(Tipo.RECEITA, "Salário", 0, v("5000.00"), contaId, 5));

        assertEquals(1, cf.aplicarFixosMes("março", 2025));
        assertEquals("MARÇO", cf.getReceitas().get(0).getMes());
        assertThrows(IllegalArgumentException.class, () -> cf.aplicarFixosMes("Marco", 2025));
    }

    // --- Validações ---

    @Test
    void naoDeveSalvarDespesaComCategoriaInexistente() {
        assertThrows(RuntimeException.class, () -> cf.salvarDespesa(
                new Despesa(9999, "X", v("10.00"), contaId, LocalDate.of(2025, 6, 1))));
    }

    @Test
    void naoDeveSalvarLancamentoComValorZeroOuSemTexto() {
        assertThrows(IllegalArgumentException.class,
                () -> cf.salvarReceita(new Receita("Salário", v("0"), contaId, LocalDate.of(2025, 6, 1))));
        assertThrows(IllegalArgumentException.class,
                () -> cf.salvarInvestimento(new Investimento(" ", v("10"), contaId, LocalDate.of(2025, 6, 1))));
        assertThrows(IllegalArgumentException.class,
                () -> cf.salvarDespesa(new Despesa(catId, "X", v("10"), contaId, null)));
    }

    // --- Pesquisa e anos ---

    @Test
    void devePesquisarDespesasPorCategoriaMesEAno() {
        int lazer = categoria("Lazer");
        despesa("Aluguel", "1000.00", LocalDate.of(2025, 6, 10));
        cf.salvarDespesa(new Despesa(lazer, "Cinema", v("40.00"), contaId, LocalDate.of(2025, 7, 1)));

        assertEquals(2, cf.pesquisarDespesas(null, 2025, "Todos").size());
        assertEquals(1, cf.pesquisarDespesas(lazer, 2025, "Todos").size());
        assertEquals(0, cf.pesquisarDespesas(lazer, 2025, "JUNHO").size());
        assertEquals(0, cf.pesquisarDespesas(null, 2024, "Todos").size());
    }

    @Test
    void deveListarAnosDisponiveis() {
        receita("R1", "100.00", LocalDate.of(2024, 1, 1));
        despesa("D1", "100.00", LocalDate.of(2026, 1, 1));
        assertEquals(List.of(2024, 2026), cf.anosDisponiveis());
    }

    // --- Usuário ---

    @Test
    void deveAtualizarPerfilEValidar() {
        cf.atualizarUsuario("  Hudson ", LocalDate.of(1995, 5, 10));
        assertEquals("Hudson", cf.getUsuario().getNome());
        assertThrows(IllegalArgumentException.class, () -> cf.atualizarUsuario("", null));
        assertThrows(IllegalArgumentException.class, () -> cf.atualizarUsuario("X", LocalDate.now().plusDays(1)));
    }
}
