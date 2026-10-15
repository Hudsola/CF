package ui.agenda;

import javafx.scene.Node;
import ui.components.EmBreve;

/** Aba "Agenda" (ainda sem funcionalidades). */
public class AgendaView {

    public Node getView() {
        return EmBreve.tela("Agenda", "Compromissos, lembretes e vencimentos, com visão de calendário.");
    }
}
