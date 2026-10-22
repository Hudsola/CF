package ui.estoque;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import service.Estoque;

/** Aba "Estoque": produtos da casa com código, descrição, quantidade e unidade, e ajustes manuais. */
public class EstoqueView {

    private final Estoque estoque = new Estoque(ui.Sessao.usuario().getId());

    public Node getView() {
        Label titulo = new Label("Estoque da casa");
        titulo.getStyleClass().add("section-title");
        Node produtos = new AbaProdutos(estoque).getView();
        VBox bloco = new VBox(10, titulo, produtos);
        bloco.getStyleClass().add("fixos-block");
        bloco.setPadding(new Insets(16));
        VBox.setVgrow(produtos, Priority.ALWAYS);

        VBox root = new VBox(bloco);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");
        VBox.setVgrow(bloco, Priority.ALWAYS);
        return root;
    }
}
