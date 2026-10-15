package ui;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.Usuario;
import ui.agenda.AgendaView;
import ui.components.NavBar;
import ui.components.Ui;
import ui.estoque.EstoqueView;
import ui.financas.FinancasView;
import ui.home.HomeView;
import ui.login.LoginView;
import ui.missoes.MissoesView;

import java.net.URI;

public class App extends Application {

    private static Stage primaryStage;
    private static BorderPane root;
    private static App instancia;
    private static NavBar barra;
    private static String telaAtual;

    @Override
    public void start(Stage stage) {
        instancia = this;
        primaryStage = stage;
        boolean smokeTest = getParameters().getRaw().contains(SmokeTest.ARGUMENTO);
        if (smokeTest) SmokeTest.prepararBanco();
        try {
            DatabaseManager.inicializar();
        } catch (RuntimeException e) {
            Alert alerta = Ui.comIcone(new Alert(Alert.AlertType.ERROR,
                    e.getMessage() + "\n\nBanco: " + DatabaseManager.arquivoBanco()));
            alerta.setTitle("Controle Financeiro");
            alerta.setHeaderText("Não foi possível abrir o banco de dados");
            alerta.showAndWait();
            throw e;
        }

        root = new BorderPane();
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        stage.setTitle("Controle Financeiro Pessoal");
        stage.getIcons().setAll(Ui.icones());
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        mostrarLogin();
        stage.show();
        if (smokeTest) SmokeTest.executar();
    }

    /** Tela inicial: nada dos dados financeiros é mostrado antes do login. */
    public static void mostrarLogin() {
        Sessao.encerrar();
        barra = null;
        telaAtual = null;
        root.setTop(null);
        root.setCenter(new LoginView(App::entrar).getView());
    }

    /** Chamado pela tela de login quando o usuário se autentica. */
    public static void entrar(Usuario usuario) {
        Sessao.iniciar(usuario);
        barra = new NavBar();
        root.setTop(barra);
        navegarPara("home");
    }

    /** Abre uma das abas da barra: home, missoes, financas, agenda ou estoque. */
    public static void navegarPara(String tela) {
        if (!Sessao.ativa()) { mostrarLogin(); return; }
        root.setCenter(switch (tela) {
            case "home" -> new HomeView().getView();
            case "missoes" -> new MissoesView().getView();
            case "financas" -> new FinancasView().getView();
            case "agenda" -> new AgendaView().getView();
            case "estoque" -> new EstoqueView().getView();
            default -> throw new IllegalArgumentException("Tela desconhecida: " + tela);
        });
        telaAtual = tela;
        if (barra != null) barra.marcarAtivo(tela);
    }

    /** Aba aberta no momento (nulo antes do login). */
    public static String telaAtual() { return telaAtual; }

    /** Recria a barra superior (ex: depois de o usuário mudar o nome no perfil). */
    public static void atualizarBarra() {
        if (!Sessao.ativa()) return;
        barra = new NavBar();
        if (telaAtual != null) barra.marcarAtivo(telaAtual);
        root.setTop(barra);
    }

    /** Abre um arquivo ou pasta no programa padrão do sistema (no Windows, o Explorer para pastas). */
    public static void abrirNoSistema(java.nio.file.Path caminho) {
        abrirEndereco(caminho.toUri());
    }

    /** Abre um endereço no navegador padrão. */
    public static void abrirEndereco(URI endereco) {
        if (instancia != null) instancia.getHostServices().showDocument(endereco.toString());
    }

    public static Stage getStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
