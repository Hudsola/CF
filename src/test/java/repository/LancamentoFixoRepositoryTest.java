package repository;

import model.Conta;
import model.LancamentoFixo;
import model.LancamentoFixo.Tipo;
import org.junit.jupiter.api.*;
import util.DatabaseTestHelper;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LancamentoFixoRepositoryTest {

    private final LancamentoFixoRepository repo      = new LancamentoFixoRepository();
    private final ContaRepository          contaRepo = new ContaRepository();
    private int contaId;

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
        contaRepo.salvar(new Conta("Nubank"));
        contaId = contaRepo.listarTodos().get(0).getId();
    }

    private LancamentoFixo novoFixo(Tipo tipo, String desc) {
        return new LancamentoFixo(tipo, desc, 0, 500.00, contaId, 5);
    }

    @Test
    @Order(1)
    void deveSalvarEListar() {
        repo.salvar(novoFixo(Tipo.RECEITA, "Salário"));
        assertEquals(1, repo.listarTodos().size());
        assertEquals("Salário", repo.listarTodos().get(0).getDescricao());
    }

    @Test
    @Order(2)
    void deveListarAtivos() {
        repo.salvar(novoFixo(Tipo.RECEITA, "Salário"));
        repo.salvar(novoFixo(Tipo.DESPESA, "Condomínio"));
        assertEquals(2, repo.listarAtivos().size());
    }

    @Test
    @Order(3)
    void deveAlternarAtivo() {
        repo.salvar(novoFixo(Tipo.DESPESA, "Netflix"));
        LancamentoFixo lf = repo.listarTodos().get(0);
        assertTrue(lf.isAtivo());
        repo.alternarAtivo(lf.getId());
        assertFalse(repo.listarTodos().get(0).isAtivo());
        repo.alternarAtivo(lf.getId());
        assertTrue(repo.listarTodos().get(0).isAtivo());
    }

    @Test
    @Order(4)
    void naoDeveAplicarDuasVezesNoMesmoMes() {
        repo.salvar(novoFixo(Tipo.RECEITA, "Salário"));
        LancamentoFixo lf = repo.listarTodos().get(0);
        assertFalse(repo.jaAplicado(lf.getId(), "JUNHO", 2025));
        repo.registrarAplicacao(lf.getId(), "JUNHO", 2025);
        assertTrue(repo.jaAplicado(lf.getId(), "JUNHO", 2025));
    }

    @Test
    @Order(5)
    void deveAtualizar() {
        repo.salvar(novoFixo(Tipo.RECEITA, "Salário Antigo"));
        LancamentoFixo lf = repo.listarTodos().get(0);
        repo.atualizar(new LancamentoFixo(lf.getId(), Tipo.RECEITA, "Salário Novo",
            0, "", 600.00, contaId, "Nubank", 10, true));
        assertEquals("Salário Novo", repo.listarTodos().get(0).getDescricao());
        assertEquals(600.00, repo.listarTodos().get(0).getValor(), 0.01);
    }

    @Test
    @Order(6)
    void deveExcluirComAplicacoes() {
        repo.salvar(novoFixo(Tipo.DESPESA, "Internet"));
        LancamentoFixo lf = repo.listarTodos().get(0);
        repo.registrarAplicacao(lf.getId(), "MAIO", 2025);
        repo.excluir(lf.getId());
        assertTrue(repo.listarTodos().isEmpty());
    }
}
