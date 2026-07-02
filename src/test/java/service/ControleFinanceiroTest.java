package service;

import model.*;
import model.LancamentoFixo.Tipo;
import org.junit.jupiter.api.*;
import repository.ContaRepository;
import repository.CategoriaRepository;
import util.DatabaseTestHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControleFinanceiroTest {

    private ControleFinanceiro cf;
    private int contaId;
    private int catId;

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
        cf = new ControleFinanceiro();

        cf.salvarConta(new Conta("Nubank"));
        contaId = cf.getContas().get(0).getId();
        catId = cf.getCategorias().stream()
            .filter(c -> c.getNome().equalsIgnoreCase("Moradia"))
            .findFirst().orElseThrow().getId();
    }

    // --- Receitas ---

    @Test
    @Order(1)
    void deveSomarReceitasPorMes() {
        cf.salvarReceita(new Receita("Salário", 5000.00, contaId, LocalDate.of(2025, 6, 5), "JUNHO", 2025));
        cf.salvarReceita(new Receita("Freelance", 1000.00, contaId, LocalDate.of(2025, 6, 15), "JUNHO", 2025));
        cf.salvarReceita(new Receita("Bônus", 2000.00, contaId, LocalDate.of(2025, 7, 1), "JULHO", 2025));

        assertEquals(6000.00, cf.somarReceitas("JUNHO", 2025), 0.01);
        assertEquals(2000.00, cf.somarReceitas("JULHO", 2025), 0.01);
        assertEquals(8000.00, cf.somarReceitasAno(2025), 0.01);
    }

    // --- Despesas ---

    @Test
    @Order(2)
    void deveSomarDespesasPorCategoria() {
        cf.salvarDespesa(new Despesa(catId, "Condomínio", 850.00, contaId, LocalDate.of(2025, 6, 10), "JUNHO", 2025));
        cf.salvarDespesa(new Despesa(catId, "Água", 80.00, contaId, LocalDate.of(2025, 6, 15), "JUNHO", 2025));

        assertEquals(930.00, cf.somarTotalDespesas("JUNHO", 2025), 0.01);
        assertEquals(930.00, cf.somarTotalDespesasAno(2025), 0.01);
    }

    // --- Saldo ---

    @Test
    @Order(3)
    void deveCalcularSaldoEmConta() {
        cf.salvarReceita(new Receita("Salário", 5000.00, contaId, LocalDate.of(2025, 6, 5), "JUNHO", 2025));
        cf.salvarDespesa(new Despesa(catId, "Condomínio", 1000.00, contaId, LocalDate.of(2025, 6, 10), "JUNHO", 2025));
        cf.salvarInvestimento(new Investimento("Poupança", 500.00, contaId, LocalDate.of(2025, 6, 1), "JUNHO", 2025));

        // saldo = receita - despesa - investimento
        assertEquals(3500.00, cf.saldoEmConta(2025, "JUNHO"), 0.01);
    }

    @Test
    @Order(4)
    void deveRetornarSaldoZeroSemDados() {
        assertEquals(0.0, cf.saldoEmConta(2025, "JUNHO"), 0.01);
    }

    // --- Percentuais ---

    @Test
    @Order(5)
    void deveCalcularPorcentagemRendaGasta() {
        cf.salvarReceita(new Receita("Salário", 4000.00, contaId, LocalDate.of(2025, 6, 5), "JUNHO", 2025));
        cf.salvarDespesa(new Despesa(catId, "Aluguel", 1000.00, contaId, LocalDate.of(2025, 6, 10), "JUNHO", 2025));

        assertEquals(0.25, cf.porcentagemRendaGasta(2025, "JUNHO"), 0.001);
    }

    @Test
    @Order(6)
    void deveRetornarZeroPorcentagemSemReceita() {
        cf.salvarDespesa(new Despesa(catId, "Supermercado", 200.00, contaId, LocalDate.of(2025, 6, 1), "JUNHO", 2025));
        assertEquals(0.0, cf.porcentagemRendaGasta(2025, "JUNHO"), 0.001);
    }

    // --- Resumo anual ---

    @Test
    @Order(7)
    void deveGerarResumoAnualComSaldoAcumulado() {
        cf.salvarReceita(new Receita("Salário", 3000.00, contaId, LocalDate.of(2025, 1, 5), "JANEIRO", 2025));
        cf.salvarReceita(new Receita("Salário", 3000.00, contaId, LocalDate.of(2025, 2, 5), "FEVEREIRO", 2025));
        cf.salvarDespesa(new Despesa(catId, "Aluguel", 1000.00, contaId, LocalDate.of(2025, 1, 10), "JANEIRO", 2025));

        List<ResumoMensal> resumo = cf.gerarResumoAnual(2025);
        ResumoMensal janeiro   = resumo.get(0);
        ResumoMensal fevereiro = resumo.get(1);

        assertEquals(2000.00, janeiro.getSaldo(), 0.01);
        assertEquals(2000.00, janeiro.getSaldoAcumulado(), 0.01);
        assertEquals(3000.00, fevereiro.getSaldo(), 0.01);
        assertEquals(5000.00, fevereiro.getSaldoAcumulado(), 0.01);
    }

    // --- Divisões ---

    @Test
    @Order(8)
    void deveDividirReceitasPorOrigem() {
        cf.salvarReceita(new Receita("Salário",   5000.00, contaId, LocalDate.of(2025, 6, 5),  "JUNHO", 2025));
        cf.salvarReceita(new Receita("Freelance", 1000.00, contaId, LocalDate.of(2025, 6, 10), "JUNHO", 2025));

        Map<String, Double> div = cf.divisaoReceitasPorOrigem(2025, "JUNHO");
        assertEquals(5000.00, div.get("Salário"),   0.01);
        assertEquals(1000.00, div.get("Freelance"), 0.01);
    }

    // --- Lançamentos Fixos ---

    @Test
    @Order(9)
    void deveAplicarFixosMesESerIdempotente() {
        cf.salvarLancamentoFixo(new LancamentoFixo(Tipo.RECEITA, "Salário", 0, 5000.00, contaId, 5));

        int primeira = cf.aplicarFixosMes("JUNHO", 2025);
        int segunda  = cf.aplicarFixosMes("JUNHO", 2025); // não deve duplicar

        assertEquals(1, primeira);
        assertEquals(0, segunda);
        assertEquals(1, cf.getReceitas().size());
    }

    // --- Anos disponíveis ---

    @Test
    @Order(10)
    void deveListarAnosDisponiveis() {
        cf.salvarReceita(new Receita("R1", 100.00, contaId, LocalDate.of(2024, 1, 1), "JANEIRO", 2024));
        cf.salvarReceita(new Receita("R2", 100.00, contaId, LocalDate.of(2025, 1, 1), "JANEIRO", 2025));

        List<Integer> anos = cf.anosDisponiveis();
        assertTrue(anos.contains(2024));
        assertTrue(anos.contains(2025));
    }
}
