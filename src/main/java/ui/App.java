package ui;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ui.components.NavBar;
import ui.home.HomeView;

public class App extends Application {

    private static Stage primaryStage;
    private static BorderPane root;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        DatabaseManager.inicializar();

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
            case "home"      -> root.setCenter(new HomeView().getView());
            case "cadastros" -> root.setCenter(new ui.cadastros.CadastrosView().getView());
            case "resumo"    -> root.setCenter(new ui.resumo.ResumoView().getView());
        }
    }

    public static Stage getStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
