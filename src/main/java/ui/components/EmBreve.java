package ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Tela de uma aba que ainda não tem funcionalidades. */
public final class EmBreve {

    private EmBreve() {}

    public static StackPane tela(String titulo, String descricao) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("section-title");
        Label lblDescricao = new Label(descricao);
        lblDescricao.getStyleClass().add("form-label");
        lblDescricao.setWrapText(true);
        Label selo = new Label("EM BREVE");
        selo.getStyleClass().add("selo-em-breve");

        VBox card = new VBox(12, lblTitulo, lblDescricao, selo);
        card.getStyleClass().add("fixos-block");
        card.setPadding(new Insets(28));
        card.setMaxWidth(520);
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane raiz = new StackPane(card);
        raiz.getStyleClass().add("main-content");
        raiz.setPadding(new Insets(20));
        return raiz;
    }
}
