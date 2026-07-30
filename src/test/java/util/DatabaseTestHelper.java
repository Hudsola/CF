package util;

import db.DatabaseManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseTestHelper {

    public static void setup() {
        try {
            Path banco = Files.createTempFile("controle_financeiro_test_", ".db");

            DatabaseManager.setUrl("jdbc:sqlite:" + banco.toAbsolutePath());

            DatabaseManager.inicializar();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar banco de teste", e);
        }
    }
}