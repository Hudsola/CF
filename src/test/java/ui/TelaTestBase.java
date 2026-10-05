package ui;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;
import service.Autenticacao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Base dos testes de interface: abre o app de verdade (com um banco temporário) e oferece atalhos para
 * achar campos, botões e textos na tela. Por padrão roda sem janela (Monocle), ver o surefire no pom.xml.
 */
abstract class TelaTestBase extends FxRobot {

    protected static final String SENHA = "senha-forte-1";

    private Application app;

    @BeforeEach
    void abrirApp() throws Exception {
        Path banco = Files.createTempFile("controle_financeiro_tela_", ".db");
        banco.toFile().deleteOnExit();
        DatabaseManager.setUrl("jdbc:sqlite:" + banco.toAbsolutePath());
        DatabaseManager.inicializar();
        prepararDados();
        FxToolkit.registerPrimaryStage();
        app = FxToolkit.setupApplication(App.class);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void fecharApp() throws Exception {
        FxToolkit.cleanupApplication(app);
        FxToolkit.cleanupStages();
    }

    /** Dados criados antes de o app abrir (o app abre sempre na tela de login). */
    protected void prepararDados() {}

    protected static Usuario criarUsuario(String nome, String login) {
        return new Autenticacao().cadastrar(new Autenticacao.Cadastro(nome, login, login + "@teste.com",
                LocalDate.of(1990, 6, 15), SENHA));
    }

    /** Entra direto (sem passar pela tela de login). */
    protected void entrarComo(Usuario u) {
        interact(() -> App.entrar(u));
    }

    // --- Busca de nós ---

    /** Primeiro nó visível (ele e todos os pais) que atende ao critério. */
    protected Optional<Node> achar(Predicate<Node> criterio) {
        return lookup(n -> criterio.test(n) && visivel(n)).tryQuery();
    }

    protected Node exigir(Predicate<Node> criterio, String descricao) {
        return achar(criterio).orElseThrow(() -> new AssertionError("Não encontrado na tela: " + descricao));
    }

    protected boolean temTexto(String texto) {
        return achar(n -> n instanceof Labeled l && texto.equals(l.getText())).isPresent();
    }

    protected boolean temTextoComecandoCom(String inicio) {
        return achar(n -> n instanceof Labeled l && l.getText() != null && l.getText().startsWith(inicio)).isPresent();
    }

    protected Button botao(String texto) {
        return (Button) exigir(n -> n instanceof Button b && texto.equals(b.getText()), "botão \"" + texto + "\"");
    }

    /** Clica com o mouse (robô do JavaFX) em um botão, link ou aba pelo texto. */
    protected void clicar(String texto) {
        clickOn(exigir(n -> (n instanceof ButtonBase || n.getStyleClass().contains("tab-label"))
                && n instanceof Labeled l && texto.equals(l.getText()), "\"" + texto + "\""));
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Preenche o campo de texto (ou senha) identificado pelo texto de exemplo (prompt). */
    protected void preencher(String prompt, String valor) {
        TextInputControl campo = (TextInputControl) exigir(
                n -> n instanceof TextInputControl t && prompt.equals(t.getPromptText()), "campo \"" + prompt + "\"");
        interact(() -> campo.setText(valor));
    }

    /** Espera algo que acontece em segundo plano (ex: login, que calcula o hash da senha). */
    protected void esperar(BooleanSupplier condicao) throws Exception {
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, condicao::getAsBoolean);
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static boolean visivel(Node n) {
        for (Node p = n; p != null; p = p.getParent()) if (!p.isVisible()) return false;
        return n.getScene() != null;
    }
}
