package util;

import db.DatabaseManager;
import repository.UsuarioRepository;

import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseTestHelper {

    /** Cria um banco temporário com um usuário (e as categorias padrão dele) e devolve o id desse usuário. */
    public static int setup() {
        try {
            Path banco = Files.createTempFile("controle_financeiro_test_", ".db");
            banco.toFile().deleteOnExit();

            DatabaseManager.setUrl("jdbc:sqlite:" + banco.toAbsolutePath());
            DatabaseManager.inicializar();

            return novoUsuario("teste");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar banco de teste", e);
        }
    }

    /** Usuário extra no banco atual (para testar o isolamento entre usuários). Senha não importa aqui. */
    public static int novoUsuario(String login) {
        return new UsuarioRepository().criar("Usuário " + login, login, login + "@teste.com", null, "sem-senha", null);
    }
}
