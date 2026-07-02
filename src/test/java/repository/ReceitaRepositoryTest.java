package repository;

import model.Conta;
import model.Receita;
import org.junit.jupiter.api.*;
import util.DatabaseTestHelper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReceitaRepositoryTest {

    private final ReceitaRepository repo   = new ReceitaRepository();
    private final ContaRepository contaRepo = new ContaRepository();
    private int contaId;

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
        contaRepo.salvar(new Conta("Nubank"));
        contaId = contaRepo.listarTodos().get(0).getId();
    }

    private Receita nova(String origem, double valor) {
        return new Receita(origem, valor, contaId, LocalDate.of(2025, 6, 10), "JUNHO", 2025);
    }

    @Test
    @Order(1)
    void deveSalvarEListar() {
        repo.salvar(nova("Salário", 5000.00));
        List<Receita> lista = repo.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Salário", lista.get(0).getOrigem());
        assertEquals(5000.00, lista.get(0).getValor(), 0.01);
    }

    @Test
    @Order(2)
    void deveListarPorAno() {
        repo.salvar(nova("Salário", 5000.00));
        repo.salvar(new Receita("Outro", 100, contaId, LocalDate.of(2024, 1, 1), "JANEIRO", 2024));
        assertEquals(1, repo.listarPorAno(2025).size());
        assertEquals(1, repo.listarPorAno(2024).size());
    }

    @Test
    @Order(3)
    void deveAtualizar() {
        repo.salvar(nova("Salário", 5000.00));
        Receita r = repo.listarTodos().get(0);
        repo.atualizar(new Receita(r.getId(), "Salário Atualizado", 5500.00, contaId, "Nubank",
            LocalDate.of(2025, 6, 10), "JUNHO", 2025));
        Receita atualizada = repo.listarTodos().get(0);
        assertEquals("Salário Atualizado", atualizada.getOrigem());
        assertEquals(5500.00, atualizada.getValor(), 0.01);
    }

    @Test
    @Order(4)
    void deveExcluir() {
        repo.salvar(nova("Freelance", 1000.00));
        Receita r = repo.listarTodos().get(0);
        repo.excluir(r.getId());
        assertTrue(repo.listarTodos().isEmpty());
    }

    @Test
    @Order(5)
    void deveLancarErroAoExcluirIdInexistente() {
        assertThrows(RuntimeException.class, () -> repo.excluir(9999));
    }
}
