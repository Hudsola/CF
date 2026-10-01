package repository;

import model.Conta;
import model.Despesa;
import model.Periodo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static util.Assercoes.assertValor;
import static util.Assercoes.v;

class DespesaRepositoryTest {

    private final DespesaRepository   repo      = new DespesaRepository();
    private final ContaRepository     contaRepo = new ContaRepository();
    private final CategoriaRepository catRepo   = new CategoriaRepository();
    private int contaId;
    private int moradia;
    private int lazer;

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
        contaRepo.salvar(new Conta("Nubank"));
        contaId = contaRepo.listarTodos().get(0).getId();
        moradia = categoria("Moradia");
        lazer = categoria("Lazer");
    }

    private int categoria(String nome) {
        return catRepo.listarTodos().stream().filter(c -> c.getNome().equals(nome)).findFirst().orElseThrow().getId();
    }

    private Despesa nova(String detalhe, String valor) {
        return new Despesa(moradia, detalhe, v(valor), contaId, LocalDate.of(2025, 6, 5));
    }

    @Test
    void deveSalvarEListar() {
        repo.salvar(nova("Condomínio", "850.00"));
        List<Despesa> lista = repo.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Condomínio", lista.get(0).getDetalhamento());
        assertEquals("Moradia", lista.get(0).getCategoriaNome());
        assertValor("850.00", lista.get(0).getValor());
    }

    @Test
    void devePesquisarPorPeriodoECategoria() {
        repo.salvar(nova("Condomínio", "850.00"));
        repo.salvar(new Despesa(lazer, "Cinema", v("40"), contaId, LocalDate.of(2025, 6, 20)));
        repo.salvar(new Despesa(moradia, "Aluguel", v("1200.00"), contaId, LocalDate.of(2024, 3, 5)));

        assertEquals(2, repo.listarPorPeriodo(Periodo.doAno(2025)).size());
        assertEquals(1, repo.pesquisar(lazer, Periodo.doAno(2025)).size());
        assertEquals(1, repo.pesquisar(moradia, Periodo.de(2024, "MARÇO")).size());
        assertEquals(1, repo.listarPorContaEPeriodo(contaId, Periodo.de(2024, "Todos")).size());
    }

    @Test
    void deveAtualizar() {
        repo.salvar(nova("Internet", "100.00"));
        Despesa d = repo.listarTodos().get(0);
        repo.atualizar(new Despesa(d.getId(), moradia, "Moradia", "Internet Fibra",
            v("120.00"), contaId, "Nubank", LocalDate.of(2025, 6, 5)));
        assertEquals("Internet Fibra", repo.listarTodos().get(0).getDetalhamento());
        assertValor("120.00", repo.listarTodos().get(0).getValor());
    }

    @Test
    void deveExcluir() {
        repo.salvar(nova("Água", "60.00"));
        repo.excluir(repo.listarTodos().get(0).getId());
        assertTrue(repo.listarTodos().isEmpty());
    }
}
