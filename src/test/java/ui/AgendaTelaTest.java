package ui;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import model.Missao;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.Missoes;
import ui.components.Ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Aba Agenda: calendário do mês com as missões e a lista do dia selecionado. */
class AgendaTelaTest extends TelaTestBase {

    private Usuario usuario;
    private Missoes missoes;
    private final LocalDate hoje = LocalDate.now();
    private Missao feita;

    @Override
    protected void prepararDados() {
        usuario = criarUsuario("Fábio Reis", "fabio");
        missoes = new Missoes(usuario.getId());
        missoes.criar(Missao.diaria("Reunião", null, List.of(hoje)));
        feita = missoes.criar(Missao.diaria("Remédio", null, List.of(hoje)));
        missoes.concluir(feita.id(), hoje);
        missoes.criar(Missao.diaria("Viagem", null, List.of(hoje.plusDays(1))));
        missoes.criar(Missao.semPrazo("Comprar uma moto", null));
    }

    @BeforeEach
    void abrirAgenda() {
        entrarComo(usuario);
        clicar("AGENDA");
    }

    @Test
    void abreNoMesAtualComODiaDeHojeSelecionado() {
        assertTrue(temTexto(nomeDoMes(YearMonth.from(hoje))));
        String extenso = Ui.diaPorExtenso(hoje);
        assertTrue(temTexto(Character.toUpperCase(extenso.charAt(0)) + extenso.substring(1)));
        assertTrue(celula(hoje).getStyleClass().containsAll(List.of("agenda-dia-hoje", "agenda-dia-selecionado")));
    }

    @Test
    void calendarioMostraPendentesEFeitasComCoresDiferentesESemPrazoNaoAparece() {
        assertTrue(item(hoje, "Reunião").getStyleClass().contains("agenda-item-pendente"));
        assertTrue(item(hoje, "Remédio").getStyleClass().contains("agenda-item-feita"));
        assertFalse(temTexto("Comprar uma moto"), "missão sem prazo não tem dia");
    }

    @Test
    void marcarNaListaDoDiaAtualizaOCalendario() {
        clickOn(caixa("Reunião"));
        assertTrue(missoes.doDia(hoje).stream().allMatch(o -> o.concluida()));
        assertTrue(item(hoje, "Reunião").getStyleClass().contains("agenda-item-feita"));
    }

    @Test
    void clicarNumDiaMostraAsMissoesDeleEDiaFuturoNaoPodeSerMarcado() {
        LocalDate amanha = hoje.plusDays(1);
        if (!YearMonth.from(amanha).equals(YearMonth.from(hoje))) clicar("›");
        clickOn(celula(amanha));

        assertTrue(celula(amanha).getStyleClass().contains("agenda-dia-selecionado"));
        assertTrue(caixa("Viagem").isDisabled(), "esse dia ainda não chegou");
        assertFalse(achar(n -> n instanceof Label l && "Reunião".equals(l.getText())
                && l.getStyleClass().contains("missao-titulo")).isPresent(), "a lista mostra só o dia escolhido");
    }

    @Test
    void navegaEntreOsMesesEVoltaParaHoje() {
        YearMonth atual = YearMonth.from(hoje);
        clicar("›");
        assertTrue(temTexto(nomeDoMes(atual.plusMonths(1))));
        clicar("‹");
        clicar("‹");
        assertTrue(temTexto(nomeDoMes(atual.minusMonths(1))));
        clicar("Hoje");
        assertTrue(temTexto(nomeDoMes(atual)));
        assertTrue(celula(hoje).getStyleClass().contains("agenda-dia-selecionado"));
    }

    // --- auxiliares ---

    private static String nomeDoMes(YearMonth m) {
        String nome = m.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Ui.PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " " + m.getYear();
    }

    private Node celula(LocalDate dia) {
        return exigir(n -> n.getStyleClass().contains("agenda-dia") && dia.equals(n.getUserData()), "dia " + dia);
    }

    private Label item(LocalDate dia, String titulo) {
        return (Label) celula(dia).lookupAll(".agenda-item").stream()
                .filter(n -> titulo.equals(((Label) n).getText())).findFirst()
                .orElseThrow(() -> new AssertionError(titulo + " não aparece em " + dia));
    }

    private CheckBox caixa(String titulo) {
        Node rotulo = exigir(n -> n instanceof Label l && titulo.equals(l.getText())
                && l.getStyleClass().contains("missao-titulo"), titulo + " na lista do dia");
        return (CheckBox) rotulo.getParent().getParent().lookup(".check-box");
    }
}
