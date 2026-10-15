package ui;

import javafx.scene.control.Button;
import model.Conta;
import model.Dinheiro;
import model.Despesa;
import model.Meses;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ControleFinanceiro;
import ui.components.NavBar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Estrutura do app em abas: Home, Missões, Finanças, Agenda e Estoque. */
class NavegacaoAbasTest extends TelaTestBase {

    private static final Map<String, String> ABAS = Map.of(
            "home", "HOME", "missoes", "MISSÕES", "financas", "FINANÇAS", "agenda", "AGENDA", "estoque", "ESTOQUE");

    private Usuario usuario;

    @Override
    protected void prepararDados() {
        usuario = criarUsuario("Davi Rocha", "davi");
        ControleFinanceiro cf = new ControleFinanceiro(usuario.getId());
        cf.salvarConta(new Conta("Carteira"));
        int conta = cf.getContas().get(0).getId();
        int categoria = cf.getCategorias().get(0).getId();
        cf.salvarDespesa(new Despesa(categoria, "Mercado", new BigDecimal("120.50"), conta, LocalDate.now()));
    }

    @BeforeEach
    void entrar() {
        entrarComo(usuario);
    }

    @Test
    void barraTemAsCincoAbasNaOrdemEAbreNaHome() {
        assertEquals(List.of("home", "missoes", "financas", "agenda", "estoque"), List.copyOf(NavBar.ABAS.keySet()));
        NavBar barra = (NavBar) exigir(n -> n instanceof NavBar, "barra de navegação");
        List<String> textos = barra.getChildren().stream()
                .filter(n -> n.getStyleClass().contains("nav-button")).map(n -> ((Button) n).getText()).toList();
        assertEquals(List.of("HOME", "MISSÕES", "FINANÇAS", "AGENDA", "ESTOQUE"), textos);
        assertEquals("home", App.telaAtual());
        assertEquals(List.of("HOME"), ativos());
    }

    @Test
    void cadaBotaoDaBarraAbreSuaAbaEFicaDestacado() {
        for (var aba : NavBar.ABAS.entrySet()) {
            clicar(aba.getValue());
            assertEquals(aba.getKey(), App.telaAtual());
            assertEquals(List.of(aba.getValue()), ativos(), "só a aba aberta fica destacada");
        }
    }

    @Test
    void abasSemFuncionalidadeMostramEmBreve() {
        for (String[] aba : new String[][]{{"MISSÕES", "Missões"}, {"AGENDA", "Agenda"}, {"ESTOQUE", "Estoque"}}) {
            clicar(aba[0]);
            assertTrue(temTexto(aba[1]), "título da aba " + aba[1]);
            assertTrue(temTexto("EM BREVE"));
        }
    }

    @Test
    void financasTemVisaoGeralCadastrosEResumo() {
        clicar("FINANÇAS");
        for (String sub : new String[]{"Visão geral", "Cadastros", "Resumo"})
            assertTrue(achar(n -> n.getStyleClass().contains("tab-label") && sub.equals(((javafx.scene.control.Label) n).getText()))
                    .isPresent(), "sub-aba " + sub);
        assertTrue(temTexto("Saldo por Conta"), "abre na visão geral");
    }

    @Test
    void homeTemUmCardPorAbaQueLevaAteEla() {
        for (String[] card : new String[][]{{"Abrir Finanças →", "financas"}, {"Abrir Missões →", "missoes"},
                {"Abrir Agenda →", "agenda"}, {"Abrir Estoque →", "estoque"}}) {
            clicar("HOME");
            clicar(card[0]);
            assertEquals(card[1], App.telaAtual());
            assertEquals(List.of(ABAS.get(card[1])), ativos(), "a barra acompanha a navegação feita pela Home");
        }
    }

    @Test
    void cardDeFinancasNaHomeMostraOsTotaisDoMes() {
        assertTrue(temTexto("FINANÇAS — " + Meses.nome(LocalDate.now())));
        assertTrue(temTexto(Dinheiro.formatar(new BigDecimal("120.50"))), "despesa do mês");
        assertTrue(temTexto(Dinheiro.formatar(new BigDecimal("-120.50"))), "saldo do mês negativo");
    }

    @Test
    void recriarABarraMantemAAbaAtivaETelaDesconhecidaEhRecusada() {
        clicar("FINANÇAS");
        interact(App::atualizarBarra);
        assertEquals(List.of("FINANÇAS"), ativos());
        assertThrows(IllegalArgumentException.class, () -> App.navegarPara("cadastros"));
        assertEquals("financas", App.telaAtual());
    }

    private List<String> ativos() {
        NavBar barra = (NavBar) exigir(n -> n instanceof NavBar, "barra de navegação");
        return barra.getChildren().stream()
                .filter(n -> n.getStyleClass().contains("nav-button-active"))
                .map(n -> ((Button) n).getText()).toList();
    }

}
