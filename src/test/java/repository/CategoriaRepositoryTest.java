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

    @Test
    @Order(6)
    void deveExcluirCategoriaJuntoComSeusMapeamentos() {
        repo.salvar(new Categoria("ComMapeamento"));
        Categoria cat = repo.listarTodos().stream()
            .filter(c -> c.getNome().equals("ComMapeamento")).findFirst().orElseThrow();
        MapeamentoRepository mapRepo = new MapeamentoRepository();
        mapRepo.salvar(new model.MapeamentoDescricao("Loja X", cat.getId(), "Loja X"));

        repo.excluir(cat.getId());

        assertTrue(mapRepo.listarTodos().isEmpty());
    }

    @Test
    @Order(7)
    void naoDeveExcluirCategoriaUsadaEmLancamentoFixo() {
        new ContaRepository().salvar(new model.Conta("Nubank"));
        int contaId = new ContaRepository().listarTodos().get(0).getId();
        Categoria moradia = repo.listarTodos().stream()
            .filter(c -> c.getNome().equals("Moradia")).findFirst().orElseThrow();
        new LancamentoFixoRepository().salvar(new model.LancamentoFixo(
            model.LancamentoFixo.Tipo.DESPESA, "Aluguel", moradia.getId(), new java.math.BigDecimal("1500.00"), contaId, 10));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> repo.excluir(moradia.getId()));
        assertTrue(ex.getMessage().contains("lançamentos fixos"));
    }
}
