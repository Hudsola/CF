package ui.home;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Usuario;
import service.ControleFinanceiro;
import ui.components.Ui;

/** Diálogo para editar nome e data de nascimento exibidos no card de perfil. */
final class PerfilDialog {

    private PerfilDialog() {}

    /** Mostra o diálogo; devolve true se o perfil foi salvo. */
    static boolean editar(ControleFinanceiro cf) {
        Usuario u = cf.getUsuario();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar perfil");
        dialog.setHeaderText(null);

        TextField tfNome = new TextField(u.getNome());
        DatePicker dpNasc = Ui.seletorData(u.getDataNascimento());
        Label lblErro = new Label();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Nome:"), tfNome);
        grid.addRow(1, new Label("Nascimento:"), dpNasc);
        grid.add(lblErro, 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Valida antes de fechar: se der erro, mostra a mensagem e mantém o diálogo aberto.
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            try {
                cf.atualizarUsuario(tfNome.getText(), dpNasc.getValue());
            } catch (Exception ex) {
                Ui.mostrarMsg(lblErro, ex.getMessage(), false);
                e.consume();
            }
        });
        return dialog.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
