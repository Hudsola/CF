package ui;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ui.components.NavBar;
import ui.home.HomeView;

public class App extends Application {

    private static Stage primaryStage;
    private static BorderPane root;
    private static App instancia;

    @Override
    public void start(Stage stage) {
        instancia = this;
        primaryStage = stage;
        try {
            DatabaseManager.inicializar();
        } catch (RuntimeException e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR,
                    e.getMessage() + "\n\nBanco: " + DatabaseManager.arquivoBanco());
            alerta.setTitle("Controle Financeiro");
            alerta.setHeaderText("Não foi possível abrir o banco de dados");
            alerta.showAndWait();
            throw e;
        }

        root = new BorderPane();
        NavBar navBar = new NavBar();
        root.setTop(navBar);

        // Abre na home por padrão
        navegarPara("home");

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        stage.setTitle("Controle Financeiro Pessoal");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void navegarPara(String tela) {
        switch (tela) {
            case "home" -> root.setCenter(new HomeView().getView());
            case "cadastros" -> root.setCenter(new ui.cadastros.CadastrosView().getView());
            case "resumo"    -> root.setCenter(new ui.resumo.ResumoView().getView());
        }
    }

    /** Abre um arquivo ou pasta no programa padrão do sistema (no Windows, o Explorer para pastas). */
    public static void abrirNoSistema(java.nio.file.Path caminho) {
        if (instancia != null) instancia.getHostServices().showDocument(caminho.toUri().toString());
    }

    public static Stage getStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
