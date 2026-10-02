package ui.login;

import db.DatabaseManager;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.Usuario;
import service.Autenticacao;
import service.GoogleOAuth;
import ui.App;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Executa o login com Google numa thread separada (a espera pelo navegador não trava a tela) e mostra
 * uma janelinha "Aguardando…" com botão Cancelar.
 */
public final class FluxoGoogle {

    private FluxoGoogle() {}

    /** Credenciais do Google Cloud, se o arquivo google-oauth.json estiver na pasta de dados. */
    public static Optional<GoogleOAuth.Credenciais> credenciais() {
        try {
            return GoogleOAuth.carregarCredenciais(DatabaseManager.pastaDados());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public static String comoConfigurar() {
        return "Login com Google não configurado. Salve o arquivo de credenciais do Google Cloud como\n"
                + GoogleOAuth.ARQUIVO_CREDENCIAIS + " na pasta " + DatabaseManager.pastaDados()
                + "\n(passo a passo no README, seção \"Login com Google\").";
    }

    /**
     * @param vincularA id do usuário a quem ligar a conta Google (perfil antigo ou usuário logado);
     *                  nulo para entrar/criar conta pelo Google
     */
    public static void executar(Window dono, Integer vincularA, Consumer<Usuario> sucesso, Consumer<String> erro) {
        Optional<GoogleOAuth.Credenciais> cred = credenciais();
        if (cred.isEmpty()) { erro.accept(comoConfigurar()); return; }
        GoogleOAuth oauth = new GoogleOAuth(cred.get());

        Stage espera = new Stage();
        espera.initOwner(dono);
        espera.initModality(Modality.WINDOW_MODAL);
        espera.setTitle("Entrar com Google");
        Label msg = new Label("Conclua o login na janela do navegador que foi aberta…");
        msg.getStyleClass().add("form-label");
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("btn-secondary");
        VBox caixa = new VBox(14, new ProgressIndicator(), msg, cancelar);
        caixa.setAlignment(Pos.CENTER);
        caixa.setPadding(new Insets(24));
        caixa.getStyleClass().add("main-content");
        Scene cena = new Scene(caixa, 420, 200);
        if (dono != null && dono.getScene() != null) cena.getStylesheets().setAll(dono.getScene().getStylesheets());
        espera.setScene(cena);

        Task<Usuario> tarefa = new Task<>() {
            @Override protected Usuario call() throws Exception {
                Autenticacao.PerfilGoogle perfil = oauth.autenticar(App::abrirEndereco, Duration.ofMinutes(5));
                return new Autenticacao().entrarComGoogle(perfil, vincularA);
            }
        };
        tarefa.setOnSucceeded(e -> { espera.close(); sucesso.accept(tarefa.getValue()); });
        tarefa.setOnFailed(e -> {
            espera.close();
            Throwable t = tarefa.getException();
            erro.accept(t.getMessage() != null ? t.getMessage() : t.toString());
        });
        cancelar.setOnAction(e -> oauth.cancelar());
        espera.setOnCloseRequest(e -> oauth.cancelar());

        Thread t = new Thread(tarefa, "login-google");
        t.setDaemon(true);
        t.start();
        espera.show();
    }
}
