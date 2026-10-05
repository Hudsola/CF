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
    /** Trava privada (não a da classe) para criar o caminho do banco uma única vez. */
    private static final Object TRAVA_ARQUIVO = new Object();

    /** Versão do esquema gravada em PRAGMA user_version. Suba ao criar uma nova migração. */
    static final int VERSAO_ESQUEMA = 3;

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
    public static Path arquivoBanco() {
        if (customUrl != null) return Path.of(customUrl.replaceFirst("^jdbc:sqlite:", "")).toAbsolutePath();
        synchronized (TRAVA_ARQUIVO) {
            if (arquivoPadrao == null) {
                String env = System.getenv(VARIAVEL_PASTA);
                Path pasta = env != null && !env.isBlank()
                        ? Path.of(env)
                        : Path.of(System.getProperty("user.home"), "ControleFinanceiro");
                arquivoPadrao = prepararArquivo(pasta, Path.of("").toAbsolutePath());
            }
            return arquivoPadrao;
        }
    }

    /** Pasta onde ficam o banco e os arquivos de configuração (ex: google-oauth.json). */
    public static Path pastaDados() {
        Path pasta = arquivoBanco().getParent();
        return pasta != null ? pasta : Path.of("").toAbsolutePath();
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

            // Bancos novos já nascem na estrutura atual; bancos existentes são ajustados em migrar().
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome             TEXT    NOT NULL,
                    data_nascimento  TEXT,
                    usuario          TEXT    COLLATE NOCASE,
                    email            TEXT    COLLATE NOCASE,
                    senha_hash       TEXT,
                    google_id        TEXT,
                    criado_em        TEXT,
                    ultimo_login     TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categorias (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
                    nome       TEXT    NOT NULL COLLATE NOCASE,
                    UNIQUE(usuario_id, nome)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS contas (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id    INTEGER NOT NULL REFERENCES usuarios(id),
                    nome          TEXT    NOT NULL COLLATE NOCASE,
                    saldo_inicial REAL    NOT NULL DEFAULT 0,
                    UNIQUE(usuario_id, nome)
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
                    usuario_id    INTEGER NOT NULL REFERENCES usuarios(id),
                    padrao        TEXT    NOT NULL COLLATE NOCASE,
                    categoria_id  INTEGER NOT NULL REFERENCES categorias(id),
                    detalhe       TEXT    NOT NULL,
                    UNIQUE(usuario_id, padrao)
                )
            """);

            migrar(conn);

            // Login: usuário, e-mail e conta Google não podem se repetir (vários NULL são permitidos).
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_usuario ON usuarios(usuario)");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_email ON usuarios(email)");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_google ON usuarios(google_id)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_contas_usuario ON contas(usuario_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_categorias_usuario ON categorias(usuario_id)");
            // Consultas por período filtram pela data (texto ISO aaaa-mm-dd).
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_receitas_data ON receitas(data)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_despesas_data ON despesas(data)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_investimentos_data ON investimentos(data)");

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
     * Se qualquer passo falhar, nada é alterado.
     */
    private static void migrar(Connection conn) throws SQLException {
        int versao;
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("PRAGMA user_version")) {
            versao = rs.next() ? rs.getInt(1) : 0;
        }
        if (versao >= VERSAO_ESQUEMA) return;

        boolean alteraV1 = versao < 1 && (
                temColuna(conn, "receitas", "mes") || temColuna(conn, "despesas", "mes")
                || temColuna(conn, "investimentos", "mes") || temColuna(conn, "usuarios", "xp"));
        boolean alteraV2 = versao < 2 && !temColuna(conn, "contas", "saldo_inicial");
        boolean alteraV3 = versao < 3 && !temColuna(conn, "contas", "usuario_id");
        if (alteraV1 || alteraV2 || alteraV3) fazerBackup(versao);

        // Recriar tabelas referenciadas por outras (contas, categorias) exige desligar a checagem de
        // chaves estrangeiras durante a troca; ela é refeita com PRAGMA foreign_key_check antes do commit.
        try (Statement s = conn.createStatement()) { s.execute("PRAGMA foreign_keys = OFF"); }
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
            if (alteraV3) migrarParaV3(stmt, conn);

            try (ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {
                if (rs.next())
                    throw new SQLException("referência inválida na tabela " + rs.getString("table"));
            }
            stmt.execute("PRAGMA user_version = " + VERSAO_ESQUEMA);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Falha ao migrar o banco para a versão " + VERSAO_ESQUEMA
                    + " (nenhuma alteração foi aplicada): " + e.getMessage(), e);
        } finally {
            conn.setAutoCommit(true);
            try (Statement s = conn.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        }
    }

    /**
     * v3: login de usuários. Os dados que existiam antes (de quando o app tinha um único perfil) ficam
     * com o usuário 1, que ainda não tem usuário/senha nem Google: a tela de login pede para criar esse acesso.
     */
    private static void migrarParaV3(Statement stmt, Connection conn) throws SQLException {
        for (String coluna : List.of("usuario TEXT COLLATE NOCASE", "email TEXT COLLATE NOCASE",
                "senha_hash TEXT", "google_id TEXT", "criado_em TEXT", "ultimo_login TEXT")) {
            if (!temColuna(conn, "usuarios", coluna.substring(0, coluna.indexOf(' '))))
                stmt.execute("ALTER TABLE usuarios ADD COLUMN " + coluna);
        }
        stmt.execute("INSERT OR IGNORE INTO usuarios (id, nome) VALUES (1, 'Usuário')");
        stmt.execute("UPDATE usuarios SET criado_em = datetime('now', 'localtime') WHERE criado_em IS NULL");

        // contas e categorias: nome passa a ser único por usuário (antes era único no banco todo).
        stmt.execute("""
            CREATE TABLE contas_v3 (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id    INTEGER NOT NULL REFERENCES usuarios(id),
                nome          TEXT    NOT NULL COLLATE NOCASE,
                saldo_inicial REAL    NOT NULL DEFAULT 0,
                UNIQUE(usuario_id, nome)
            )
        """);
        stmt.execute("INSERT INTO contas_v3 (id, usuario_id, nome, saldo_inicial) SELECT id, 1, nome, saldo_inicial FROM contas");
        stmt.execute("DROP TABLE contas");
        stmt.execute("ALTER TABLE contas_v3 RENAME TO contas");

        stmt.execute("""
            CREATE TABLE categorias_v3 (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
                nome       TEXT    NOT NULL COLLATE NOCASE,
                UNIQUE(usuario_id, nome)
            )
        """);
        stmt.execute("INSERT INTO categorias_v3 (id, usuario_id, nome) SELECT id, 1, nome FROM categorias");
        stmt.execute("DROP TABLE categorias");
        stmt.execute("ALTER TABLE categorias_v3 RENAME TO categorias");

        stmt.execute("""
            CREATE TABLE mapeamentos_v3 (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id    INTEGER NOT NULL REFERENCES usuarios(id),
                padrao        TEXT    NOT NULL COLLATE NOCASE,
                categoria_id  INTEGER NOT NULL REFERENCES categorias(id),
                detalhe       TEXT    NOT NULL,
                UNIQUE(usuario_id, padrao)
            )
        """);
        stmt.execute("INSERT INTO mapeamentos_v3 (id, usuario_id, padrao, categoria_id, detalhe) "
                + "SELECT id, 1, padrao, categoria_id, detalhe FROM mapeamentos_descricao");
        stmt.execute("DROP TABLE mapeamentos_descricao");
        stmt.execute("ALTER TABLE mapeamentos_v3 RENAME TO mapeamentos_descricao");
    }

    private static boolean temColuna(Connection conn, String tabela, String coluna) throws SQLException {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info(" + tabela + ")")) {
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
        String nome = String.valueOf(arquivo.getFileName()).replaceFirst("\\.db$", "");
        Path destino = arquivo.resolveSibling(nome + "-backup-v" + versao + "-" + sufixo + ".db");
        try {
            Files.copy(arquivo, destino, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new SQLException("Não foi possível criar o backup do banco antes da migração: " + e.getMessage(), e);
        }
    }
}
