package util;

import db.DatabaseManager;

/**
 * Configura o banco em memória para os testes.
 * Cada teste que chamar setup() recebe um banco limpo.
 */
public class DatabaseTestHelper {

    public static void setup() {
        DatabaseManager.setUrl("jdbc:sqlite::memory:");
        DatabaseManager.inicializar();
    }
}
