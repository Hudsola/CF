package repository;

import model.Categoria;
import model.Conta;
import model.Despesa;
import org.junit.jupiter.api.*;
import util.DatabaseTestHelper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DespesaRepositoryTest {

    private final DespesaRepository   repo      = new DespesaRepository();
    private final ContaRepository     contaRepo = new ContaRepository();
    private final CategoriaRepository catRepo   = new CategoriaRepository();
    private int contaId;
    private int categoriaId;

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
        contaRepo.salvar(new Conta("Nubank"));
        contaId = contaRepo.listarTodos().get(0).getId();
        categoriaId = catRepo.listarTodos().stream()
            .filter(c -> c.getNome().equalsIgnoreCase("Moradia"))
            .findFirst().orElseThrow().getId();
    }

    private Despesa nova(String detalhe, double valor) {
        return new Despesa(categoriaId, detalhe, valor, contaId,
            LocalDate.of(2025, 6, 5), "JUNHO", 2025);
    }

    @Test
    @Order(1)
    void deveSalvarEListar() {
        repo.salvar(nova("Condomínio", 850.00));
        List<Despesa> lista = repo.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Condomínio", lista.get(0).getDetalhamento());
        assertEquals(850.00, lista.get(0).getValor(), 0.01);
    }

    @Test
    @Order(2)
    void deveListarPorAno() {
        repo.salvar(nova("Condomínio", 850.00));
        repo.salvar(new Despesa(categoriaId, "Aluguel", 1200.00, contaId,
            LocalDate.of(2024, 3, 5), "MARÇO", 2024));
        assertEquals(1, repo.listarPorAno(2025).size());
        assertEquals(1, repo.listarPorAno(2024).size());
    }

    @Test
    @Order(3)
    void deveAtualizar() {
        repo.salvar(nova("Internet", 100.00));
        Despesa d = repo.listarTodos().get(0);
        repo.atualizar(new Despesa(d.getId(), categoriaId, "Moradia", "Internet Fibra",
            120.00, contaId, "Nubank", LocalDate.of(2025, 6, 5), "JUNHO", 2025));
        assertEquals("Internet Fibra", repo.listarTodos().get(0).getDetalhamento());
        assertEquals(120.00, repo.listarTodos().get(0).getValor(), 0.01);
    }

    @Test
    @Order(4)
    void deveExcluir() {
        repo.salvar(nova("Água", 60.00));
        Despesa d = repo.listarTodos().get(0);
        repo.excluir(d.getId());
        assertTrue(repo.listarTodos().isEmpty());
    }
}
