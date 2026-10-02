package service;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Cada usuário só vê e altera os próprios dados. */
class IsolamentoUsuariosTest {

    private ControleFinanceiro ana, bia;
    private int contaAna, contaBia, catAna;

    @BeforeEach
    void setUp() {
        int idAna = DatabaseTestHelper.setup();
        int idBia = DatabaseTestHelper.novoUsuario("bia");
        ana = new ControleFinanceiro(idAna);
        bia = new ControleFinanceiro(idBia);
        ana.salvarConta(new Conta("Nubank"));
        bia.salvarConta(new Conta("Nubank"));          // mesmo nome é permitido em usuários diferentes
        contaAna = ana.getContas().get(0).getId();
        contaBia = bia.getContas().get(0).getId();
        catAna = ana.getCategorias().get(0).getId();
        ana.salvarReceita(new Receita("Salário", new BigDecimal("5000"), contaAna, LocalDate.of(2026, 9, 5)));
        ana.salvarDespesa(new Despesa(catAna, "Aluguel", new BigDecimal("1500"), contaAna, LocalDate.of(2026, 9, 10)));
    }

    @Test
    void cadaUsuarioVeSoOsProprios() {
        assertEquals(1, ana.getReceitas().size());
        assertTrue(bia.getReceitas().isEmpty());
        assertTrue(bia.getDespesas().isEmpty());
        assertEquals(1, bia.getContas().size());
        assertNotEquals(ana.getCategorias().get(0).getId(), bia.getCategorias().get(0).getId(), "categorias próprias");
        assertEquals(new BigDecimal("3500.00"), ana.saldoTotal());
        assertEquals(new BigDecimal("0.00"), bia.saldoTotal());
        assertTrue(bia.anosDisponiveis().isEmpty());
        assertTrue(bia.gerarResumoAnual(2026).stream().noneMatch(ResumoMensal::temMovimento));
    }

    @Test
    void naoPodeLancarEmContaOuCategoriaDeOutroUsuario() {
        assertThrows(IllegalArgumentException.class,
                () -> bia.salvarReceita(new Receita("X", BigDecimal.TEN, contaAna, LocalDate.of(2026, 9, 1))));
        assertThrows(IllegalArgumentException.class,
                () -> bia.salvarDespesa(new Despesa(catAna, "X", BigDecimal.TEN, contaBia, LocalDate.of(2026, 9, 1))));
        assertTrue(ana.getReceitas().size() == 1 && bia.getReceitas().isEmpty());
    }

    @Test
    void naoPodeAlterarNemExcluirRegistrosDeOutroUsuario() {
        Receita r = ana.getReceitas().get(0);
        assertThrows(RuntimeException.class, () -> bia.excluirReceita(r.getId()));
        assertThrows(RuntimeException.class, () -> bia.excluirConta(contaAna));
        assertThrows(RuntimeException.class, () -> bia.excluirCategoria(catAna));
        assertThrows(RuntimeException.class, () -> bia.atualizarConta(new Conta(contaAna, "Roubada")));
        assertEquals(1, ana.getReceitas().size());
        assertEquals("Nubank", ana.getContas().get(0).getNome());
    }
}
