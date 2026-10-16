package ui.missoes;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.OcorrenciaMissao;
import service.CalculadoraXp;
import service.Missoes;
import ui.components.Ui;

import java.util.List;

/**
 * Lista de ocorrências de missões com uma caixa de marcar em cada uma: marcar conclui o dia (e rende XP),
 * desmarcar desfaz. Dias futuros não podem ser marcados. Usada em Missões (hoje e atrasadas) e na Agenda.
 */
public class ListaMissoes extends VBox {

    private final Missoes missoes;
    private final Runnable aoMudar;
    private final boolean mostrarData;
    private final Label lblMsg = new Label();

    /**
     * @param mostrarData inclui a data em cada item (para listas com vários dias, como as atrasadas)
     * @param aoMudar     chamado depois de concluir ou desfazer (para atualizar o resto da tela)
     */
    public ListaMissoes(Missoes missoes, boolean mostrarData, Runnable aoMudar) {
        super(6);
        this.missoes = missoes;
        this.mostrarData = mostrarData;
        this.aoMudar = aoMudar;
        getStyleClass().add("lista-missoes");
        lblMsg.setWrapText(true);
        lblMsg.managedProperty().bind(lblMsg.textProperty().isNotEmpty());
    }

    public void mostrar(List<OcorrenciaMissao> ocorrencias, String textoVazio) {
        getChildren().clear();
        if (ocorrencias.isEmpty()) {
            Label vazio = new Label(textoVazio);
            vazio.getStyleClass().add("empty-label");
            getChildren().add(vazio);
        }
        for (OcorrenciaMissao o : ocorrencias) getChildren().add(item(o));
        getChildren().add(lblMsg);
    }

    private HBox item(OcorrenciaMissao o) {
        CheckBox marcar = new CheckBox();
        marcar.setSelected(o.concluida());
        boolean futuro = o.data().isAfter(missoes.hoje());
        marcar.setDisable(futuro);
        marcar.setTooltip(new Tooltip(futuro ? "Esse dia ainda não chegou."
                : o.concluida() ? "Desmarcar (desfaz a conclusão)" : "Marcar como feita (+" + CalculadoraXp.XP_POR_MISSAO + " XP)"));
        marcar.setOnAction(e -> alternar(o, marcar));

        Label titulo = new Label(o.missao().titulo());
        titulo.getStyleClass().add("missao-titulo");
        if (o.concluida()) titulo.getStyleClass().add("missao-feita");
        String detalhe = o.missao().tipo().nome() + " · " + o.missao().quando();
        if (mostrarData) detalhe = Ui.diaCurto(o.data()) + " · " + detalhe;
        if (o.atrasada(missoes.hoje())) {
            detalhe = "atrasada · " + detalhe;
            titulo.getStyleClass().add("missao-atrasada");
        }
        Label lblDetalhe = new Label(detalhe);
        lblDetalhe.getStyleClass().add("form-label");

        HBox linha = new HBox(10, marcar, new VBox(2, titulo, lblDetalhe));
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.getStyleClass().add("missao-item");
        return linha;
    }

    private void alternar(OcorrenciaMissao o, CheckBox marcar) {
        try {
            if (marcar.isSelected()) missoes.concluir(o.missao().id(), o.data());
            else missoes.desfazerConclusao(o.missao().id(), o.data());
            lblMsg.setText("");
            aoMudar.run();
        } catch (RuntimeException ex) {
            marcar.setSelected(!marcar.isSelected());
            Ui.mostrarMsg(lblMsg, ex.getMessage(), false);
        }
    }
}
