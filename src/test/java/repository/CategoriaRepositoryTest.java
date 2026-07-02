package repository;

import model.Categoria;
import org.junit.jupiter.api.*;
import util.DatabaseTestHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoriaRepositoryTest {

    private final CategoriaRepository repo = new CategoriaRepository();

    @BeforeEach
    void setUp() { DatabaseTestHelper.setup(); }

    @Test
    @Order(1)
    void deveSalvarEListarCategoria() {
        repo.salvar(new Categoria("Transporte Extra"));
        List<Categoria> lista = repo.listarTodos();
        assertTrue(lista.stream().anyMatch(c -> c.getNome().equalsIgnoreCase("Transporte Extra")));
    }

    @Test
    @Order(2)
    void deveConterCategoriasDefault() {
        List<Categoria> lista = repo.listarTodos();
        assertTrue(lista.stream().anyMatch(c -> c.getNome().equalsIgnoreCase("Moradia")));
        assertTrue(lista.stream().anyMatch(c -> c.getNome().equalsIgnoreCase("Alimentação")));
    }

    @Test
    @Order(3)
    void deveRejeitarDuplicadaCaseInsensitive() {
        // "moradia" já existe como "Moradia" nas categorias padrão
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> repo.salvar(new Categoria("moradia")));
        assertTrue(ex.getMessage().toLowerCase().contains("já existe"));
    }

    @Test
    @Order(4)
    void deveAtualizarCategoria() {
        repo.salvar(new Categoria("Teste"));
        Categoria cat = repo.listarTodos().stream()
            .filter(c -> c.getNome().equals("Teste")).findFirst().orElseThrow();
        repo.atualizar(new Categoria(cat.getId(), "Teste Atualizado"));
        Categoria atualizada = repo.listarTodos().stream()
            .filter(c -> c.getId() == cat.getId()).findFirst().orElseThrow();
        assertEquals("Teste Atualizado", atualizada.getNome());
    }

    @Test
    @Order(5)
    void deveExcluirCategoriaSeVinculada() {
        repo.salvar(new Categoria("ParaExcluir"));
        Categoria cat = repo.listarTodos().stream()
            .filter(c -> c.getNome().equals("ParaExcluir")).findFirst().orElseThrow();
        repo.excluir(cat.getId());
        assertFalse(repo.listarTodos().stream().anyMatch(c -> c.getNome().equals("ParaExcluir")));
    }
}
