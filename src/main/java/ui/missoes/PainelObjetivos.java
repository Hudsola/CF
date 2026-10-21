package ui.missoes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Missao;
import model.TipoMissao;
import service.CalculadoraXp;
import service.Missoes;
import ui.components.Ui;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Objetivos em andamento (missões sem prazo) e missões com requisitos pendentes, cada uma com as suas
 * sub-missões logo abaixo. Requisitos (sub-missões sem prazo) têm caixa de marcar; a caixa da missão-mãe só
 * fica liberada depois que todos os requisitos estão cumpridos. Sub-missões recorrentes aparecem só como
 * informação (são marcadas por dia, em "Hoje" ou na Agenda).
 */
class PainelObjetivos extends VBox {

    private final Missoes missoes;
    private final Runnable aoMudar;
    private final Label lblMsg = new Label();

    PainelObjetivos(Missoes missoes, Runnable aoMudar) {
        super(6);
        this.missoes = missoes;
        this.aoMudar = aoMudar;
        lblMsg.setWrapText(true);
        lblMsg.managedProperty().bind(lblMsg.textProperty().isNotEmpty());
    }

    void atualizar() {
        getChildren().clear();
        List<Missao> todas = missoes.listarEmOrdem();
        boolean algum = false;
        for (Missao m : todas) {
            if (m.subMissao()) continue;
            List<Missao> subs = todas.stream().filter(s -> m.id() == (s.missaoPaiId() == null ? -1 : s.missaoPaiId())).toList();
            List<Missao> pendentes = subs.stream().filter(s -> s.requisito() && s.concluidaEm() == null).toList();
            boolean objetivoAberto = m.tipo() == TipoMissao.SEM_PRAZO && m.concluidaEm() == null;
            if (!objetivoAberto && pendentes.isEmpty()) continue;
            algum = true;
            getChildren().add(cabecalho(m, subs, pendentes));
            for (Missao s : subs) getChildren().add(s.requisito() ? requisito(s) : habito(s));
        }
        if (!algum) {
            Label vazio = new Label("Nenhum objetivo em andamento.");
            vazio.getStyleClass().add("empty-label");
            getChildren().add(vazio);
        }
        getChildren().add(lblMsg);
    }

    private HBox cabecalho(Missao m, List<Missao> subs, List<Missao> pendentes) {
        Label titulo = new Label(m.titulo());
        titulo.getStyleClass().addAll("missao-titulo", m.principal() ? "missao-principal" : "missao-secundaria");
        long requisitos = subs.stream().filter(Missao::requisito).count();
        Label progresso = new Label(requisitos == 0 ? m.categoria().nome()
                : m.categoria().nome() + " · " + (requisitos - pendentes.size()) + "/" + requisitos + " requisitos");
        progresso.getStyleClass().add("form-label");
        HBox linha = new HBox(10);
        if (m.tipo() == TipoMissao.SEM_PRAZO) {
            CheckBox marcar = new CheckBox();
            marcar.setDisable(!pendentes.isEmpty());
            marcar.setTooltip(new Tooltip(pendentes.isEmpty()
                    ? "Concluir o objetivo (+" + CalculadoraXp.xpPorObjetivo(m.categoria()) + " XP)"
                    : "Faltam os requisitos: " + pendentes.stream().map(Missao::titulo).collect(Collectors.joining(", "))));
            marcar.setOnAction(e -> executar(marcar, () -> missoes.concluirSemPrazo(m.id())));
            linha.getChildren().add(marcar);
        }
        if (m.principal()) {
            Label estrela = new Label("★");
            estrela.getStyleClass().add("estrela-principal");
            estrela.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);   // não vira "…" quando falta espaço
            linha.getChildren().add(estrela);
        }
        linha.getChildren().add(new VBox(2, titulo, progresso));
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.getStyleClass().addAll("missao-item", "objetivo");
        return linha;
    }

    private HBox requisito(Missao r) {
        CheckBox marcar = new CheckBox();
        boolean feito = r.concluidaEm() != null;
        marcar.setSelected(feito);
        marcar.setTooltip(new Tooltip(feito ? "Desmarcar o requisito" : "Requisito cumprido (+" + CalculadoraXp.xpPorObjetivo(r.categoria()) + " XP)"));
        marcar.setOnAction(e -> executar(marcar, () -> {
            if (marcar.isSelected()) missoes.concluirSemPrazo(r.id());
            else missoes.reabrirSemPrazo(r.id());
        }));
        Label titulo = new Label(r.titulo());
        titulo.getStyleClass().add("missao-titulo");
        if (feito) titulo.getStyleClass().add("missao-feita");
        HBox linha = new HBox(10, new Label("↳"), marcar, new VBox(titulo));   // mesma estrutura das outras listas
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(0, 0, 0, 24));
        linha.getStyleClass().addAll("missao-item", "requisito");
        return linha;
    }

    private HBox habito(Missao s) {
        Label texto = new Label("↳ " + s.titulo() + "  (" + s.tipo().nome().toLowerCase() + " · " + s.quando() + ")");
        texto.getStyleClass().add("form-label");
        HBox linha = new HBox(texto);
        linha.setPadding(new Insets(0, 0, 0, 24));
        return linha;
    }

    private void executar(CheckBox marcar, Runnable acao) {
        try {
            acao.run();
            lblMsg.setText("");
            aoMudar.run();
        } catch (RuntimeException ex) {
            marcar.setSelected(!marcar.isSelected());
            Ui.mostrarMsg(lblMsg, ex.getMessage(), false);
        }
    }
}
