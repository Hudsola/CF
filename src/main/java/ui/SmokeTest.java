package ui;

import db.DatabaseManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import model.Usuario;
import service.Autenticacao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Teste rápido do app empacotado (JAR ou instalador): {@code ControleFinanceiro --smoke-test}.
 *
 * Usa um banco temporário (os dados do usuário não são tocados), cria um usuário, entra e abre todas as
 * telas. Termina com código 0 se tudo abriu, ou 1 se qualquer erro aconteceu — usado pelo CI.
 */
final class SmokeTest {

    static final String ARGUMENTO = "--smoke-test";
    private static final String[] TELAS = {"home", "cadastros", "resumo", "home"};

    private SmokeTest() {}

    /** Antes de abrir o banco: aponta para um banco temporário e encerra com erro em qualquer exceção. */
    static void prepararBanco() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> falhar(e));
        try {
            Path pasta = Files.createTempDirectory("controle-financeiro-smoke");
            DatabaseManager.setUrl("jdbc:sqlite:" + pasta.resolve("smoke.db"));
        } catch (IOException e) {
            falhar(e);
        }
    }

    /** Com a janela já aberta: entra com um usuário novo e percorre as telas, uma a cada 300 ms. */
    static void executar() {
        try {
            Usuario u = new Autenticacao().cadastrar(new Autenticacao.Cadastro(
                    "Smoke Test", "smoke", "smoke@teste.com", LocalDate.of(1990, 1, 1), "senha-do-smoke"));
            App.entrar(u);
        } catch (RuntimeException e) {
            falhar(e);
        }
        proximaTela(0);
    }

    private static void proximaTela(int i) {
        PauseTransition pausa = new PauseTransition(Duration.millis(300));
        pausa.setOnFinished(e -> {
            if (i == TELAS.length) {
                System.out.println("SMOKE TEST OK: todas as telas abriram sem erro.");
                Platform.exit();
                System.exit(0);
            }
            App.navegarPara(TELAS[i]);
            proximaTela(i + 1);
        });
        pausa.play();
    }

    private static void falhar(Throwable e) {
        System.err.println("SMOKE TEST FALHOU:");
        e.printStackTrace();
        System.exit(1);
    }
}
