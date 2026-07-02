package db;

import java.sql.*;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:controle_financeiro.db";
    private static String customUrl = null;

    /** Permite injetar URL customizada (ex: :memory: para testes). */
    public static void setUrl(String url) { customUrl = url; }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(customUrl != null ? customUrl : URL);
    }

    public static void inicializar() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome             TEXT    NOT NULL,
                    data_nascimento  TEXT,
                    nivel            INTEGER NOT NULL DEFAULT 1,
                    xp               INTEGER NOT NULL DEFAULT 0,
                    xp_proximo_nivel INTEGER NOT NULL DEFAULT 100
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categorias (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL UNIQUE COLLATE NOCASE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS contas (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL UNIQUE COLLATE NOCASE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS receitas (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    origem    TEXT    NOT NULL,
                    valor     REAL    NOT NULL,
                    conta_id  INTEGER NOT NULL REFERENCES contas(id),
                    data      TEXT    NOT NULL,
                    mes       TEXT    NOT NULL,
                    ano       INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS despesas (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    categoria_id  INTEGER NOT NULL REFERENCES categorias(id),
                    detalhamento  TEXT    NOT NULL,
                    valor         REAL    NOT NULL,
                    conta_id      INTEGER NOT NULL REFERENCES contas(id),
                    data          TEXT    NOT NULL,
                    mes           TEXT    NOT NULL,
                    ano           INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS investimentos (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo      TEXT    NOT NULL,
                    valor     REAL    NOT NULL,
                    conta_id  INTEGER NOT NULL REFERENCES contas(id),
                    data      TEXT    NOT NULL,
                    mes       TEXT    NOT NULL,
                    ano       INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS lancamentos_fixos (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo           TEXT    NOT NULL,
                    descricao      TEXT    NOT NULL,
                    categoria_id   INTEGER NOT NULL DEFAULT 0,
                    valor          REAL    NOT NULL,
                    conta_id       INTEGER NOT NULL REFERENCES contas(id),
                    dia_vencimento INTEGER NOT NULL,
                    ativo          INTEGER NOT NULL DEFAULT 1
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS aplicacoes_fixos (
                    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                    lancamento_fixo_id INTEGER NOT NULL,
                    mes                TEXT    NOT NULL,
                    ano                INTEGER NOT NULL,
                    UNIQUE(lancamento_fixo_id, mes, ano)
                )
            """);

            // Categorias padrão
            stmt.execute("""
                INSERT OR IGNORE INTO categorias (nome) VALUES
                    ('Alimentação'),('Moradia'),('Educação'),('Pet'),
                    ('Saúde'),('Transporte'),('Pessoais'),('Lazer'),('Financeiros')
            """);

            // Usuário padrão (se não existir)
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (id, nome, nivel, xp, xp_proximo_nivel)
                VALUES (1, 'Usuário', 1, 0, 100)
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco: " + e.getMessage(), e);
        }
    }
}
