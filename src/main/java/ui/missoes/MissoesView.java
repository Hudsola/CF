package ui.missoes;

import javafx.scene.Node;
import ui.components.EmBreve;

/** Aba "Missões" (ainda sem funcionalidades). */
public class MissoesView {

    public Node getView() {
        return EmBreve.tela("Missões", "Objetivos e desafios que rendem XP e fazem você subir de nível.");
    }
}
