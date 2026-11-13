package service;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Investimento como saldo guardado: a aplicação sai do disponível sem ser despesa; o resgate (total) devolve o
 * aplicado e lança só a diferença — lucro como receita ou prejuízo como despesa.
 */
class InvestimentosTest {

    /** Aplicação no início de outubro e resgate no fim do mês (datas passadas, como exige o resgate). */
    private static final LocalDate APLICACAO = LocalDate.of(2026, 10, 1);
    private static final LocalDate RESGATE = LocalDate.of(2026, 10, 31);

    private int usuario;
    private ControleFinanceiro cf;
    private int conta;

    @BeforeEach
    void setUp() {
        usuario = DatabaseTestHelper.setup();
        cf = new ControleFinanceiro(usuario);
        cf.salvarConta(new Conta("Nubank"));
        conta = cf.getContas().get(0).getId();
        cf.salvarReceita(new Receita("Salário", new BigDecimal("10000"), conta, APLICACAO));
    }

    private Investimento aplicar(String valor) {
        cf.salvarInvestimento(new Investimento("CDB", new BigDecimal(valor), conta, APLICACAO));
        return cf.getInvestimentos().get(0);
    }

    private static void assertValor(String esperado, BigDecimal valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor), "esperado " + esperado + ", veio " + valor);
    }

    @Test
    void aplicacaoTiraDoDisponivelEFicaGuardadaSemVirarDespesa() {
        aplicar("7000");

        assertValor("3000", cf.saldoTotal());
        assertValor("7000", cf.guardadoEmInvestimentos());
        assertTrue(cf.getDespesas().isEmpty(), "aplicar não é despesa");
        TotaisPeriodo outubro = cf.totais(2026, "Outubro");
        assertValor("0", outubro.despesas());
        assertValor("7000", outubro.investimentos());
        assertValor("3000", outubro.saldo());
    }

    @Test
    void resgateComPrejuizoDevolveOAplicadoELancaSoADiferencaComoDespesa() {
        Investimento i = aplicar("7000");

        BigDecimal resultado = cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("5000"));

        assertValor("-2000", resultado);
        assertValor("8000", cf.saldoTotal(), "10k − 7k aplicados + 5k sacados");
        assertValor("0", cf.guardadoEmInvestimentos());
        assertEquals(1, cf.getReceitas().size(), "o valor sacado não vira receita (é devolução)");
        Despesa prejuizo = cf.getDespesas().get(0);
        assertValor("2000", prejuizo.getValor());
        assertEquals("Prejuízo do investimento: CDB", prejuizo.getDetalhamento());
        assertEquals(ControleFinanceiro.CATEGORIA_INVESTIMENTOS, prejuizo.getCategoriaNome());
        assertEquals(RESGATE, prejuizo.getData());
        assertEquals(conta, prejuizo.getContaId());

        Investimento resgatado = cf.getInvestimentos().get(0);
        assertTrue(resgatado.isResgatado());
        assertEquals(RESGATE, resgatado.getDataResgate());
        assertValor("5000", resgatado.getValorResgatado());
        assertValor("-2000", resgatado.getResultado());
    }

    private static void assertValor(String esperado, BigDecimal valor, String msg) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor), msg + ": veio " + valor);
    }

    @Test
    void resgateComLucroLancaADiferencaComoReceita() {
        Investimento i = aplicar("7000");
        assertValor("1000", cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("8000")));

        assertValor("11000", cf.saldoTotal());
        assertTrue(cf.getDespesas().isEmpty());
        Receita lucro = cf.getReceitas().stream().filter(r -> r.getOrigem().startsWith("Lucro")).findFirst().orElseThrow();
        assertValor("1000", lucro.getValor());
        assertEquals("Lucro do investimento: CDB", lucro.getOrigem());
        assertEquals(RESGATE, lucro.getData());
    }

    @Test
    void resgateSemDiferencaNaoLancaNada() {
        Investimento i = aplicar("7000");
        assertValor("0", cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("7000")));
        assertValor("10000", cf.saldoTotal());
        assertEquals(1, cf.getReceitas().size());
        assertTrue(cf.getDespesas().isEmpty());
    }

    @Test
    void resgateZeradoEhPrejuizoTotal() {
        Investimento i = aplicar("7000");
        cf.resgatarInvestimento(i.getId(), RESGATE, BigDecimal.ZERO);
        assertValor("3000", cf.saldoTotal());
        assertValor("7000", cf.getDespesas().get(0).getValor());
    }

    @Test
    void desfazerResgateApagaOResultadoEVoltaAFicarGuardado() {
        Investimento i = aplicar("7000");
        cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("5000"));

        cf.desfazerResgate(i.getId());

        assertFalse(cf.getInvestimentos().get(0).isResgatado());
        assertTrue(cf.getDespesas().isEmpty(), "o prejuízo lançado foi apagado");
        assertValor("3000", cf.saldoTotal());
        assertValor("7000", cf.guardadoEmInvestimentos());
        assertThrows(IllegalArgumentException.class, () -> cf.desfazerResgate(i.getId()), "não está resgatado");

        cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("9000"));   // pode resgatar de novo
        cf.desfazerResgate(i.getId());
        assertEquals(1, cf.getReceitas().size(), "o lucro lançado também é apagado");
    }

    @Test
    void desfazerFuncionaMesmoSeOLancamentoDeResultadoJaFoiApagadoAMao() {
        Investimento i = aplicar("7000");
        cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("5000"));
        cf.excluirDespesa(cf.getDespesas().get(0).getId());
        cf.desfazerResgate(i.getId());
        assertFalse(cf.getInvestimentos().get(0).isResgatado());
    }

    @Test
    void validaOResgate() {
        Investimento i = aplicar("7000");
        assertErro("antes da aplicação", () -> cf.resgatarInvestimento(i.getId(), APLICACAO.minusDays(1), BigDecimal.ONE));
        assertErro("data futura", () -> cf.resgatarInvestimento(i.getId(), LocalDate.now().plusDays(1), BigDecimal.ONE));
        assertErro("Informe a data", () -> cf.resgatarInvestimento(i.getId(), null, BigDecimal.ONE));
        assertErro("zero ou mais", () -> cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("-1")));
        assertErro("zero ou mais", () -> cf.resgatarInvestimento(i.getId(), RESGATE, null));
        assertErro("não encontrado", () -> cf.resgatarInvestimento(999, RESGATE, BigDecimal.ONE));
        assertFalse(cf.getInvestimentos().get(0).isResgatado(), "nada foi gravado");

        cf.resgatarInvestimento(i.getId(), APLICACAO, new BigDecimal("7000"));   // no mesmo dia da aplicação vale
        assertErro("já foi resgatado", () -> cf.resgatarInvestimento(i.getId(), RESGATE, BigDecimal.ONE));
    }

    private static void assertErro(String trecho, org.junit.jupiter.api.function.Executable acao) {
        var ex = assertThrows(IllegalArgumentException.class, acao);
        assertTrue(ex.getMessage().contains(trecho), "esperado \"" + trecho + "\", veio \"" + ex.getMessage() + "\"");
    }

    @Test
    void investimentoResgatadoNaoPodeSerAlteradoEExcluirApagaOResultado() {
        Investimento i = aplicar("7000");
        cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("5000"));

        assertErro("Desfaça o resgate", () -> cf.atualizarInvestimento(
                new Investimento(i.getId(), "CDB", new BigDecimal("6000"), conta, "Nubank", APLICACAO)));

        cf.excluirInvestimento(i.getId());
        assertTrue(cf.getInvestimentos().isEmpty());
        assertTrue(cf.getDespesas().isEmpty(), "o prejuízo do resgate sai junto");
        assertValor("10000", cf.saldoTotal());
    }

    @Test
    void prejuizosUsamUmaUnicaCategoriaInvestimentos() {
        Investimento a = aplicar("1000");
        cf.salvarInvestimento(new Investimento("Ações", new BigDecimal("2000"), conta, APLICACAO));
        Investimento b = cf.getInvestimentos().stream().filter(x -> x.getTipo().equals("Ações")).findFirst().orElseThrow();
        int categoriasAntes = cf.getCategorias().size();

        cf.resgatarInvestimento(a.getId(), RESGATE, new BigDecimal("900"));
        cf.resgatarInvestimento(b.getId(), RESGATE, new BigDecimal("1500"));

        assertEquals(categoriasAntes + 1, cf.getCategorias().size(), "categoria criada uma vez só");
        assertEquals(2, cf.getDespesas().stream()
                .filter(d -> d.getCategoriaNome().equals(ControleFinanceiro.CATEGORIA_INVESTIMENTOS)).count());
    }

    @Test
    void resumoMesAMesTiraNoMesDaAplicacaoEDevolveNoMesDoResgate() {
        cf.salvarInvestimento(new Investimento("CDB", new BigDecimal("7000"), conta, LocalDate.of(2026, 9, 10)));
        Investimento i = cf.getInvestimentos().get(0);
        cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("5000"));

        List<ResumoMensal> ano = cf.gerarResumoAnual(2026);
        ResumoMensal setembro = ano.get(8), outubro = ano.get(9);
        assertValor("7000", setembro.getInvestimentos());
        assertValor("-7000", setembro.getSaldo());
        assertValor("7000", outubro.getResgatados());
        assertValor("2000", outubro.getDespesaTotal());
        assertValor("15000", outubro.getSaldo(), "10k de salário + 7k devolvidos − 2k de prejuízo");
        assertValor("8000", ano.get(11).getSaldoAcumulado());

        TotaisPeriodo totalOutubro = cf.totais(2026, "Outubro");
        assertValor("7000", totalOutubro.resgatados());
        SaldoConta movimento = cf.movimentoPorConta(2026, "Outubro").get(0);
        assertValor("7000", movimento.resgatados());
        assertValor("0", movimento.investimentos());
    }

    @Test
    void saldoPorContaMostraAplicadoEResgatado() {
        Investimento i = aplicar("7000");
        cf.resgatarInvestimento(i.getId(), RESGATE, new BigDecimal("5000"));
        SaldoConta s = cf.saldosPorConta().get(0);
        assertValor("7000", s.investimentos());
        assertValor("7000", s.resgatados());
        assertValor("0", s.investidoLiquido());
        assertValor("8000", s.saldo());
    }

    @Test
    void outroUsuarioNaoResgataNemDesfaz() {
        Investimento i = aplicar("7000");
        ControleFinanceiro outro = new ControleFinanceiro(DatabaseTestHelper.novoUsuario("outro"));
        assertThrows(IllegalArgumentException.class, () -> outro.resgatarInvestimento(i.getId(), RESGATE, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> outro.desfazerResgate(i.getId()));
        assertFalse(cf.getInvestimentos().get(0).isResgatado());
    }
}
