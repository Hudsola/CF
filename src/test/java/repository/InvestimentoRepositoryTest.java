package repository;

import model.Conta;
import model.Investimento;
import org.junit.jupiter.api.*;
import util.DatabaseTestHelper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvestimentoRepositoryTest {

    private final InvestimentoRepository repo      = new InvestimentoRepository();
    private final ContaRepository        contaRepo = new ContaRepository();
    private int contaId;

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
        contaRepo.salvar(new Conta("XP"));
        contaId = contaRepo.listarTodos().get(0).getId();
    }

    private Investimento novo(String tipo, double valor) {
        return new Investimento(tipo, valor, contaId, LocalDate.of(2025, 6, 1), "JUNHO", 2025);
    }

    @Test
    @Order(1)
    void deveSalvarEListar() {
        repo.salvar(novo("Tesouro Direto", 500.00));
        assertEquals(1, repo.listarTodos().size());
        assertEquals("Tesouro Direto", repo.listarTodos().get(0).getTipo());
    }

    @Test
    @Order(2)
    void deveListarPorAno() {
        repo.salvar(novo("Tesouro Direto", 500.00));
        repo.salvar(new Investimento("CDB", 300.00, contaId, LocalDate.of(2024, 1, 1), "JANEIRO", 2024));
        assertEquals(1, repo.listarPorAno(2025).size());
        assertEquals(1, repo.listarPorAno(2024).size());
    }

    @Test
    @Order(3)
    void deveAtualizar() {
        repo.salvar(novo("Ações", 1000.00));
        Investimento i = repo.listarTodos().get(0);
        repo.atualizar(new Investimento(i.getId(), "Ações PETR4", 1100.00, contaId, "XP",
            LocalDate.of(2025, 6, 1), "JUNHO", 2025));
        assertEquals("Ações PETR4", repo.listarTodos().get(0).getTipo());
        assertEquals(1100.00, repo.listarTodos().get(0).getValor(), 0.01);
    }

    @Test
    @Order(4)
    void deveExcluir() {
        repo.salvar(novo("Poupança", 200.00));
        Investimento i = repo.listarTodos().get(0);
        repo.excluir(i.getId());
        assertTrue(repo.listarTodos().isEmpty());
    }
}
