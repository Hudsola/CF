package repository;

import model.Conta;
import org.junit.jupiter.api.*;
import util.DatabaseTestHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContaRepositoryTest {

    private final ContaRepository repo = new ContaRepository();

    @BeforeEach
    void setUp() { DatabaseTestHelper.setup(); }

    @Test
    @Order(1)
    void deveSalvarEListar() {
        repo.salvar(new Conta("Nubank"));
        List<Conta> lista = repo.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Nubank", lista.get(0).getNome());
    }

    @Test
    @Order(2)
    void deveRejeitarDuplicadaCaseInsensitive() {
        repo.salvar(new Conta("Nubank"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> repo.salvar(new Conta("nubank")));
        assertTrue(ex.getMessage().toLowerCase().contains("já existe"));
    }

    @Test
    @Order(3)
    void deveRejeitarDuplicadaComEspacos() {
        repo.salvar(new Conta("Itaú"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> repo.salvar(new Conta("ITAÚ")));
        assertTrue(ex.getMessage().toLowerCase().contains("já existe"));
    }

    @Test
    @Order(4)
    void deveAtualizarNome() {
        repo.salvar(new Conta("Conta Antiga"));
        Conta conta = repo.listarTodos().get(0);
        repo.atualizar(new Conta(conta.getId(), "Conta Nova"));
        assertEquals("Conta Nova", repo.listarTodos().get(0).getNome());
    }

    @Test
    @Order(5)
    void deveExcluirContaSemVinculos() {
        repo.salvar(new Conta("Para Excluir"));
        Conta conta = repo.listarTodos().get(0);
        repo.excluir(conta.getId());
        assertTrue(repo.listarTodos().isEmpty());
    }
}
