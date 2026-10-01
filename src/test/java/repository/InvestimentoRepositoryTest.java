package repository;

import model.Conta;
import model.Investimento;
import model.Periodo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static util.Assercoes.assertValor;
import static util.Assercoes.v;

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

    private Investimento novo(String tipo, String valor) {
        return new Investimento(tipo, v(valor), contaId, LocalDate.of(2025, 6, 1));
    }

    @Test
    void deveSalvarEListar() {
        repo.salvar(novo("Tesouro Direto", "500.00"));
        assertEquals(1, repo.listarTodos().size());
        assertEquals("Tesouro Direto", repo.listarTodos().get(0).getTipo());
    }

    @Test
    void deveListarPorPeriodo() {
        repo.salvar(novo("Tesouro Direto", "500.00"));
        repo.salvar(new Investimento("CDB", v("300.00"), contaId, LocalDate.of(2024, 1, 1)));
        assertEquals(1, repo.listarPorPeriodo(Periodo.doAno(2025)).size());
        assertEquals(1, repo.listarPorPeriodo(Periodo.doAno(2024)).size());
    }

    @Test
    void deveAtualizar() {
        repo.salvar(novo("Ações", "1000.00"));
        Investimento i = repo.listarTodos().get(0);
        repo.atualizar(new Investimento(i.getId(), "Ações PETR4", v("1100.00"), contaId, "XP",
            LocalDate.of(2025, 6, 1)));
        assertEquals("Ações PETR4", repo.listarTodos().get(0).getTipo());
        assertValor("1100.00", repo.listarTodos().get(0).getValor());
    }

    @Test
    void deveExcluir() {
        repo.salvar(novo("Poupança", "200.00"));
        repo.excluir(repo.listarTodos().get(0).getId());
        assertTrue(repo.listarTodos().isEmpty());
    }
}
