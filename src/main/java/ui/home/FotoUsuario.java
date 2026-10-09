package ui.home;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import service.FotoPerfil;
import ui.components.Ui;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/** Carrega e troca a foto de perfil do usuário (ver {@link FotoPerfil}). */
final class FotoUsuario {

    private FotoUsuario() {}

    /** Imagem da foto do usuário, se houver e for legível. */
    static Optional<Image> imagem(int usuarioId, double tamanho) {
        return FotoPerfil.arquivo(usuarioId).map(p -> new Image(p.toUri().toString(), tamanho, tamanho, true, true))
                .filter(img -> !img.isError());
    }

    /** Abre o seletor de arquivo e salva a imagem escolhida. Devolve true se a foto mudou. */
    static boolean escolher(Window dono, int usuarioId) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Escolher foto de perfil");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens",
                FotoPerfil.EXTENSOES.stream().map(e -> "*." + e).toList()));
        File arquivo = fc.showOpenDialog(dono);
        if (arquivo == null) return false;
        Path origem = arquivo.toPath();
        if (new Image(origem.toUri().toString(), 8, 8, true, false).isError()) {
            erro("Não foi possível ler essa imagem.");
            return false;
        }
        try {
            FotoPerfil.salvarEnviada(usuarioId, origem);
            return true;
        } catch (RuntimeException e) {
            erro(e.getMessage());
            return false;
        }
    }

    private static void erro(String msg) {
        Alert a = Ui.comIcone(new Alert(Alert.AlertType.ERROR, msg));
        a.setTitle("Foto de perfil");
        a.setHeaderText(null);
        a.showAndWait();
    }
}
