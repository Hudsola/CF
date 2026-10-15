package ui.estoque;

import javafx.scene.Node;
import ui.components.EmBreve;

/** Aba "Estoque" (ainda sem funcionalidades). */
public class EstoqueView {

    public Node getView() {
        return EmBreve.tela("Estoque", "Controle de itens e quantidades.");
    }
}
