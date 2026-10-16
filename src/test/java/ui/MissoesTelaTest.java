package ui;

import javafx.scene.Node;
import javafx.scene.control.*;
import model.Missao;
import model.TipoMissao;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.CalculadoraXp;
import service.ControleFinanceiro;
import service.Missoes;
import ui.components.Ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Aba Missões: cadastro de cada tipo, validação, concluir pela lista e objetivos sem prazo. */
class MissoesTelaTest extends TelaTestBase {

    private static final String PROMPT_TITULO = "Ex: Academia, Pagar a fatura, Aprender inglês";

    private Usuario usuario;
    private Missoes missoes;
    private final LocalDate hoje = LocalDate.now();

    @Override
    protected void prepararDados() {
        usuario = criarUsuario("Eva Lima", "eva");
        missoes = new Missoes(usuario.getId());
        missoes.criar(Missao.diaria("Levar o carro", null, List.of(hoje)));
        missoes.criar(Missao.diaria("Pagar o IPVA", null, List.of(hoje.minusDays(1))));
    }

    @BeforeEach
    void abrirMissoes() {
        entrarComo(usuario);
        clicar("MISSÕES");
    }

    @Test
    void mostraAsMissoesDeHojeEAsAtrasadas() {
        assertTrue(temTexto("Hoje — " + Ui.diaCurto(hoje)));
        assertTrue(temTexto("Levar o carro"));
        assertTrue(temTexto("Pagar o IPVA"), "atrasada desde ontem");
        assertTrue(temTextoComecandoCom("atrasada · " + Ui.diaCurto(hoje.minusDays(1))));
    }

    @Test
    void marcarNaListaConcluiERendeXpEDesmarcarDesfaz() {
        int xpAntes = xp();
        clickOn(caixaDaMissao("Levar o carro"));
        assertTrue(missoes.doDia(hoje).get(0).concluida());
        assertEquals(xpAntes + CalculadoraXp.XP_POR_MISSAO, xp());
        assertTrue(rotulo("Levar o carro").getStyleClass().contains("missao-feita"), "aparece riscada");

        clickOn(caixaDaMissao("Levar o carro"));
        assertFalse(missoes.doDia(hoje).get(0).concluida());
        assertEquals(xpAntes, xp());
    }

    @Test
    void concluirAtrasadaTiraDaListaDeAtrasadas() {
        clickOn(caixaDaMissao("Pagar o IPVA"));
        assertTrue(missoes.atrasadas(7).isEmpty());
        assertTrue(temTexto("Nada atrasado."));
    }

    @Test
    void cadastrarDiariaComVariosDias() {
        preencher(PROMPT_TITULO, "Consulta");
        escolherDia(hoje.plusDays(1));
        clicar("Adicionar dia");
        escolherDia(hoje.plusDays(3));
        clicar("Adicionar dia");
        assertTrue(temTextoComecandoCom(Ui.diaCurto(hoje.plusDays(1))), "chip do dia adicionado");
        clicar("Salvar Missão");

        assertTrue(temTexto("✔ Salvo."));
        Missao m = buscar("Consulta");
        assertEquals(TipoMissao.DIARIA, m.tipo());
        assertEquals(List.of(hoje.plusDays(1), hoje.plusDays(3)), List.copyOf(m.datas()));
    }

    @Test
    void diariaSemAdicionarDiaUsaODiaSelecionadoEApareceEmHoje() {
        preencher(PROMPT_TITULO, "Dentista");
        clicar("Salvar Missão");

        assertEquals(List.of(hoje), List.copyOf(buscar("Dentista").datas()));
        assertTrue(temTexto("Dentista"));
        assertNotNull(caixaDaMissao("Dentista"), "entra na lista de hoje");
    }

    @Test
    void cadastrarSemanalNosDiasMarcados() {
        preencher(PROMPT_TITULO, "Academia");
        escolherTipo(TipoMissao.SEMANAL);
        clickOn(caixaDeDiaDaSemana(DayOfWeek.MONDAY));
        clickOn(caixaDeDiaDaSemana(hoje.getDayOfWeek()));
        clicar("Salvar Missão");

        Missao m = buscar("Academia");
        assertEquals(TipoMissao.SEMANAL, m.tipo());
        assertEquals(EnumSet.of(DayOfWeek.MONDAY, hoje.getDayOfWeek()), m.diasSemana());
        assertEquals(hoje, m.inicio());
        assertNotNull(caixaDaMissao("Academia"), "hoje é um dos dias marcados");
    }

    @Test
    void cadastrarMensalNoDiaDoMes() {
        preencher(PROMPT_TITULO, "Pagar a fatura");
        escolherTipo(TipoMissao.MENSAL);
        @SuppressWarnings("unchecked")
        Spinner<Integer> dia = (Spinner<Integer>) exigir(n -> n instanceof Spinner<?>, "dia do mês");
        interact(() -> dia.getValueFactory().setValue(5));
        clicar("Salvar Missão");

        Missao m = buscar("Pagar a fatura");
        assertEquals(TipoMissao.MENSAL, m.tipo());
        assertEquals(5, m.diaMes());
        assertTrue(temTexto("todo dia 5"), "coluna Quando da tabela");
    }

    @Test
    void objetivoSemPrazoEhConcluidoEReabertoPelaTabela() {
        preencher(PROMPT_TITULO, "Aprender inglês");
        escolherTipo(TipoMissao.SEM_PRAZO);
        clicar("Salvar Missão");
        assertTrue(temTexto("• Aprender inglês"), "lista de objetivos em andamento");

        int xpAntes = xp();
        selecionarNaTabela("Aprender inglês");
        clicar("Concluir / reabrir objetivo");
        assertTrue(temTexto("✔ Objetivo \"Aprender inglês\" concluído!"));
        assertEquals(hoje, buscar("Aprender inglês").concluidaEm());
        assertEquals(xpAntes + CalculadoraXp.XP_MISSAO_SEM_PRAZO, xp());
        assertTrue(temTexto("Nenhum objetivo em andamento."));

        selecionarNaTabela("Aprender inglês");
        clicar("Concluir / reabrir objetivo");
        assertNull(buscar("Aprender inglês").concluidaEm());
    }

    @Test
    void concluirObjetivoSemSelecionarUmaMissaoSemPrazoMostraErro() {
        selecionarNaTabela("Levar o carro");
        clicar("Concluir / reabrir objetivo");
        assertTrue(temTexto("✖ Selecione uma missão sem prazo na tabela."));
    }

    @Test
    void validacoesAparecemNaTela() {
        clicar("Salvar Missão");
        assertTrue(temTexto("✖ Informe o título da missão."));

        preencher(PROMPT_TITULO, "Academia");
        escolherTipo(TipoMissao.SEMANAL);
        clicar("Salvar Missão");
        assertTrue(temTexto("✖ Escolha pelo menos um dia da semana."));
        assertEquals(2, missoes.listar().size(), "nada novo foi gravado");
    }

    @Test
    void editarMissaoPelaTela() {
        selecionarNaTabela("Levar o carro");
        clicar("Editar");
        preencher(PROMPT_TITULO, "Levar o carro na revisão");
        clicar("Atualizar Missão");

        assertTrue(temTexto("✔ Alterações salvas."));
        assertNotNull(buscar("Levar o carro na revisão"));
        assertNotNull(caixaDaMissao("Levar o carro na revisão"), "a lista de hoje é atualizada");
    }

    // --- auxiliares ---

    private int xp() {
        return new ControleFinanceiro(usuario.getId()).getProgresso().xpTotal();
    }

    private Missao buscar(String titulo) {
        return missoes.listar().stream().filter(m -> m.titulo().equals(titulo)).findFirst()
                .orElseThrow(() -> new AssertionError("missão \"" + titulo + "\" não foi gravada"));
    }

    private Label rotulo(String titulo) {
        return (Label) exigir(n -> n instanceof Label l && titulo.equals(l.getText())
                && l.getStyleClass().contains("missao-titulo"), "missão \"" + titulo + "\" na lista");
    }

    /** Caixa de marcar da missão numa lista (hoje, atrasadas ou dia da Agenda). */
    private CheckBox caixaDaMissao(String titulo) {
        Node item = rotulo(titulo).getParent().getParent();
        return (CheckBox) item.lookup(".check-box");
    }

    private CheckBox caixaDeDiaDaSemana(DayOfWeek dia) {
        String texto = dia.getDisplayName(TextStyle.SHORT, Ui.PT_BR).replace(".", "");
        return (CheckBox) exigir(n -> n instanceof CheckBox c && texto.equals(c.getText()), "dia " + texto);
    }

    private void escolherTipo(TipoMissao tipo) {
        @SuppressWarnings("unchecked")
        ComboBox<TipoMissao> combo = (ComboBox<TipoMissao>) exigir(
                n -> n instanceof ComboBox<?> c && c.getValue() instanceof TipoMissao, "tipo da missão");
        interact(() -> combo.setValue(tipo));
    }

    private void escolherDia(LocalDate dia) {
        DatePicker dp = (DatePicker) exigir(n -> n instanceof DatePicker, "seletor do dia");
        interact(() -> dp.setValue(dia));
    }

    private void selecionarNaTabela(String titulo) {
        @SuppressWarnings("unchecked")
        TableView<Missao> t = (TableView<Missao>) exigir(n -> n instanceof TableView<?>, "tabela de missões");
        interact(() -> {
            for (int i = 0; i < t.getItems().size(); i++)
                if (t.getItems().get(i).titulo().equals(titulo)) t.getSelectionModel().clearAndSelect(i);
        });
    }
}
