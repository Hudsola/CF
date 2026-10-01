package db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class DatabaseManager {

    static final String NOME_ARQUIVO = "controle_financeiro.db";

    /** Variável de ambiente que troca a pasta dos dados (útil para testes ou para guardar em outro disco). */
    static final String VARIAVEL_PASTA = "CONTROLE_FINANCEIRO_PASTA";

    private static String customUrl = null;
    private static Path arquivoPadrao = null;

    /** Versão do esquema gravada em PRAGMA user_version. Suba ao criar uma nova migração. */
    static final int VERSAO_ESQUEMA = 2;

    /** Permite injetar URL customizada (ex: :memory: para testes). */
    public static void setUrl(String url) { customUrl = url; }

    private static String url() {
        return customUrl != null ? customUrl : "jdbc:sqlite:" + arquivoBanco();
    }

    /**
     * Arquivo do banco em uso: {@code <pasta do usuário>/ControleFinanceiro/controle_financeiro.db}.
     *
     * Fica fora da pasta do programa porque, instalado, o app não pode gravar em "Arquivos de Programas",
     * e assim os dados são os mesmos rodando pelo instalador, pelo JAR ou pela IDE.
     */
    public static synchronized Path arquivoBanco() {
        if (customUrl != null) return Path.of(customUrl.replaceFirst("^jdbc:sqlite:", "")).toAbsolutePath();
        if (arquivoPadrao == null) {
            String env = System.getenv(VARIAVEL_PASTA);
            Path pasta = env != null && !env.isBlank()
                    ? Path.of(env)
                    : Path.of(System.getProperty("user.home"), "ControleFinanceiro");
            arquivoPadrao = prepararArquivo(pasta, Path.of("").toAbsolutePath());
        }
        return arquivoPadrao;
    }

    /**
     * Garante a pasta de dados e devolve o caminho do banco nela. Se ainda não houver banco lá, mas existir
     * um {@value #NOME_ARQUIVO} na pasta atual (versões antigas gravavam ali), copia-o — o original fica intacto.
     */
    static Path prepararArquivo(Path pastaDados, Path pastaAtual) {
        Path destino = pastaDados.resolve(NOME_ARQUIVO).toAbsolutePath();
        try {
            Files.createDirectories(pastaDados);
            Path antigo = pastaAtual.resolve(NOME_ARQUIVO).toAbsolutePath();
            if (!Files.exists(destino) && Files.exists(antigo) && !antigo.equals(destino))
                Files.copy(antigo, destino, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível preparar a pasta de dados " + pastaDados + ": " + e.getMessage(), e);
        }
        return destino;
    }

    public static Connection getConnection() throws SQLException {
        // O SQLite só respeita os REFERENCES das tabelas com foreign_keys ligado em cada conexão.
        Properties props = new Properties();
        props.setProperty("foreign_keys", "true");
        return DriverManager.getConnection(url(), props);
    }

    public static void inicializar() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome             TEXT    NOT NULL,
                    data_nascimento  TEXT
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
                    nome TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    saldo_inicial REAL NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS receitas (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    origem    TEXT    NOT NULL,
                    valor     REAL    NOT NULL,
                    conta_id  INTEGER NOT NULL REFERENCES contas(id),
                    data      TEXT    NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS despesas (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    categoria_id  INTEGER NOT NULL REFERENCES categorias(id),
                    detalhamento  TEXT    NOT NULL,
                    valor         REAL    NOT NULL,
                    conta_id      INTEGER NOT NULL REFERENCES contas(id),
                    data          TEXT    NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS investimentos (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo      TEXT    NOT NULL,
                    valor     REAL    NOT NULL,
                    conta_id  INTEGER NOT NULL REFERENCES contas(id),
                    data      TEXT    NOT NULL
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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mapeamentos_descricao (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    padrao        TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                    categoria_id  INTEGER NOT NULL REFERENCES categorias(id),
                    detalhe       TEXT    NOT NULL )
            """);

            migrar(conn);

            // Consultas por período filtram pela data (texto ISO aaaa-mm-dd).
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_receitas_data ON receitas(data)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_despesas_data ON despesas(data)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_investimentos_data ON investimentos(data)");

            // Categorias padrão
            stmt.execute("""
                INSERT OR IGNORE INTO categorias (nome) VALUES
                    ('Alimentação'),('Moradia'),('Educação'),('Pet'),
                    ('Saúde'),('Transporte'),('Pessoais'),('Lazer'),('Financeiros')
            """);

            // Usuário padrão (se não existir)
            stmt.execute("INSERT OR IGNORE INTO usuarios (id, nome) VALUES (1, 'Usuário')");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Migrações
    // -------------------------------------------------------------------------

    /**
     * Atualiza bancos criados por versões anteriores do app. Antes de alterar o esquema, copia o
     * arquivo do banco para "controle_financeiro-backup-v{versão}-{data}.db" na mesma pasta.
     */
    private static void migrar(Connection conn) throws SQLException {
        int versao;
        try (ResultSet rs = conn.createStatement().executeQuery("PRAGMA user_version")) {
            versao = rs.next() ? rs.getInt(1) : 0;
        }
        if (versao >= VERSAO_ESQUEMA) return;

        boolean alteraV1 = versao < 1 && (
                temColuna(conn, "receitas", "mes") || temColuna(conn, "despesas", "mes")
                || temColuna(conn, "investimentos", "mes") || temColuna(conn, "usuarios", "xp"));
        boolean alteraV2 = versao < 2 && !temColuna(conn, "contas", "saldo_inicial");
        if (alteraV1 || alteraV2) fazerBackup(versao);

        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            if (versao < 1) {
                // v1: mês e ano passam a ser derivados da data; XP passa a ser calculado pelos lançamentos.
                for (String tabela : List.of("receitas", "despesas", "investimentos")) {
                    removerColuna(stmt, conn, tabela, "mes");
                    removerColuna(stmt, conn, tabela, "ano");
                }
                for (String coluna : List.of("nivel", "xp", "xp_proximo_nivel"))
                    removerColuna(stmt, conn, "usuarios", coluna);
            }
            if (alteraV2) {
                // v2: saldo inicial de cada conta, para o saldo por conta bater com o do banco.
                stmt.execute("ALTER TABLE contas ADD COLUMN saldo_inicial REAL NOT NULL DEFAULT 0");
            }
            stmt.execute("PRAGMA user_version = " + VERSAO_ESQUEMA);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Falha ao migrar o banco para a versão " + VERSAO_ESQUEMA
                    + " (nenhuma alteração foi aplicada): " + e.getMessage(), e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static boolean temColuna(Connection conn, String tabela, String coluna) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(" + tabela + ")")) {
            while (rs.next()) if (rs.getString("name").equalsIgnoreCase(coluna)) return true;
        }
        return false;
    }

    private static void removerColuna(Statement stmt, Connection conn, String tabela, String coluna) throws SQLException {
        if (temColuna(conn, tabela, coluna))
            stmt.execute("ALTER TABLE " + tabela + " DROP COLUMN " + coluna);
    }

    private static void fazerBackup(int versao) throws SQLException {
        String u = url();
        if (!u.startsWith("jdbc:sqlite:") || u.contains(":memory:")) return;
        Path arquivo = Path.of(u.substring("jdbc:sqlite:".length()));
        if (!Files.exists(arquivo)) return;
        String sufixo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String nome = arquivo.getFileName().toString().replaceFirst("\\.db$", "");
        Path destino = arquivo.resolveSibling(nome + "-backup-v" + versao + "-" + sufixo + ".db");
        try {
            Files.copy(arquivo, destino, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new SQLException("Não foi possível criar o backup do banco antes da migração: " + e.getMessage(), e);
        }
    }
}
