package repository;

import model.Conta;
import model.Periodo;
import model.Receita;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static util.Assercoes.assertValor;
import static util.Assercoes.v;

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

    private Receita nova(String origem, String valor) {
        return new Receita(origem, v(valor), contaId, LocalDate.of(2025, 6, 10));
    }

    @Test
    void deveSalvarEListar() {
        repo.salvar(nova("Salário", "5000.00"));
        List<Receita> lista = repo.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Salário", lista.get(0).getOrigem());
        assertValor("5000.00", lista.get(0).getValor());
        assertEquals("JUNHO", lista.get(0).getMes());
        assertEquals(2025, lista.get(0).getAno());
    }

    @Test
    void devePreservarCentavosExatos() {
        repo.salvar(nova("A", "0.10"));
        repo.salvar(nova("B", "0.20"));
        assertEquals(v("0.30"), repo.listarTodos().stream().map(Receita::getValor).reduce(v("0.00"), java.math.BigDecimal::add));
    }

    @Test
    void deveListarPorPeriodoEAnos() {
        repo.salvar(nova("Salário", "5000.00"));
        repo.salvar(new Receita("Outro", v("100"), contaId, LocalDate.of(2024, 1, 1)));
        assertEquals(1, repo.listarPorPeriodo(Periodo.doAno(2025)).size());
        assertEquals(1, repo.listarPorPeriodo(Periodo.de(2024, "JANEIRO")).size());
        assertTrue(repo.listarPorPeriodo(Periodo.de(2024, "FEVEREIRO")).isEmpty());
        assertEquals(Set.of(2024, 2025), repo.anos());
    }

    @Test
    void deveAtualizar() {
        repo.salvar(nova("Salário", "5000.00"));
        Receita r = repo.listarTodos().get(0);
        repo.atualizar(new Receita(r.getId(), "Salário Atualizado", v("5500.00"), contaId, "Nubank",
            LocalDate.of(2025, 7, 10)));
        Receita atualizada = repo.listarTodos().get(0);
        assertEquals("Salário Atualizado", atualizada.getOrigem());
        assertValor("5500.00", atualizada.getValor());
        assertEquals("JULHO", atualizada.getMes());
    }

    @Test
    void deveExcluir() {
        repo.salvar(nova("Freelance", "1000.00"));
        repo.excluir(repo.listarTodos().get(0).getId());
        assertTrue(repo.listarTodos().isEmpty());
    }

    @Test
    void deveLancarErroAoExcluirIdInexistente() {
        assertThrows(RuntimeException.class, () -> repo.excluir(9999));
    }
}
