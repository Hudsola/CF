package db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.DespesaRepository;
import repository.ReceitaRepository;
import repository.UsuarioRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Um banco criado pela versão anterior do app (com colunas mes/ano e XP) deve ser migrado sem perder dados. */
class MigracaoTest {

    @TempDir Path dir;

    private Path criarBancoV0() throws Exception {
        Path arquivo = dir.resolve("controle_financeiro.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, data_nascimento TEXT, "
                    + "nivel INTEGER NOT NULL DEFAULT 1, xp INTEGER NOT NULL DEFAULT 0, xp_proximo_nivel INTEGER NOT NULL DEFAULT 100)");
            s.execute("CREATE TABLE categorias (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL UNIQUE COLLATE NOCASE)");
            s.execute("CREATE TABLE contas (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL UNIQUE COLLATE NOCASE)");
            s.execute("CREATE TABLE receitas (id INTEGER PRIMARY KEY AUTOINCREMENT, origem TEXT NOT NULL, valor REAL NOT NULL, "
                    + "conta_id INTEGER NOT NULL REFERENCES contas(id), data TEXT NOT NULL, mes TEXT NOT NULL, ano INTEGER NOT NULL)");
            s.execute("CREATE TABLE despesas (id INTEGER PRIMARY KEY AUTOINCREMENT, categoria_id INTEGER NOT NULL REFERENCES categorias(id), "
                    + "detalhamento TEXT NOT NULL, valor REAL NOT NULL, conta_id INTEGER NOT NULL REFERENCES contas(id), "
                    + "data TEXT NOT NULL, mes TEXT NOT NULL, ano INTEGER NOT NULL)");
            s.execute("CREATE TABLE investimentos (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, valor REAL NOT NULL, "
                    + "conta_id INTEGER NOT NULL REFERENCES contas(id), data TEXT NOT NULL, mes TEXT NOT NULL, ano INTEGER NOT NULL)");
            s.execute("INSERT INTO usuarios (id, nome, data_nascimento, nivel, xp) VALUES (1, 'Ana', '1990-01-02', 3, 250)");
            s.execute("INSERT INTO categorias (nome) VALUES ('Moradia')");
            s.execute("INSERT INTO contas (nome) VALUES ('Nubank')");
            s.execute("INSERT INTO receitas (origem, valor, conta_id, data, mes, ano) VALUES ('Salário', 2700.0, 1, '2026-07-05', 'JULHO', 2026)");
            s.execute("INSERT INTO despesas (categoria_id, detalhamento, valor, conta_id, data, mes, ano) VALUES (1, 'Aluguel', 636.02, 1, '2026-08-10', 'AGOSTO', 2026)");
        }
        return arquivo;
    }

    private static List<String> colunas(Path arquivo, String tabela) throws Exception {
        List<String> nomes = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             ResultSet rs = c.createStatement().executeQuery("PRAGMA table_info(" + tabela + ")")) {
            while (rs.next()) nomes.add(rs.getString("name"));
        }
        return nomes;
    }

    @Test
    void deveMigrarBancoAntigoPreservandoDadosEFazendoBackup() throws Exception {
        Path arquivo = criarBancoV0();
        DatabaseManager.setUrl("jdbc:sqlite:" + arquivo);

        DatabaseManager.inicializar();

        for (String t : List.of("receitas", "despesas", "investimentos")) {
            assertFalse(colunas(arquivo, t).contains("mes"), t);
            assertFalse(colunas(arquivo, t).contains("ano"), t);
        }
        assertEquals(List.of("id", "nome", "data_nascimento"), colunas(arquivo, "usuarios"));
        assertEquals(List.of("id", "nome", "saldo_inicial"), colunas(arquivo, "contas"));

        var receita = new ReceitaRepository().listarTodos().get(0);
        assertEquals("Salário", receita.getOrigem());
        assertEquals("JULHO", receita.getMes());
        var despesa = new DespesaRepository().listarTodos().get(0);
        assertEquals(new java.math.BigDecimal("636.02"), despesa.getValor());
        assertEquals(LocalDate.of(2026, 8, 10), despesa.getData());
        assertEquals("Ana", new UsuarioRepository().buscarPrincipal().getNome());

        try (Stream<Path> arquivos = Files.list(dir)) {
            List<Path> backups = arquivos.filter(p -> p.getFileName().toString().startsWith("controle_financeiro-backup-v0-")).toList();
            assertEquals(1, backups.size());
            assertTrue(colunas(backups.get(0), "receitas").contains("mes"), "o backup deve ter o esquema antigo");
        }
    }

    @Test
    void segundaInicializacaoNaoMigraNemFazNovoBackup() throws Exception {
        Path arquivo = criarBancoV0();
        DatabaseManager.setUrl("jdbc:sqlite:" + arquivo);
        DatabaseManager.inicializar();
        DatabaseManager.inicializar();

        try (Stream<Path> arquivos = Files.list(dir)) {
            assertEquals(1, arquivos.filter(p -> p.getFileName().toString().contains("-backup-")).count());
        }
        assertEquals(1, new ReceitaRepository().listarTodos().size());
    }

    @Test
    void deveMigrarDaVersao1ParaAVersao2AdicionandoSaldoInicialZerado() throws Exception {
        Path arquivo = dir.resolve("controle_financeiro.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE contas (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL UNIQUE COLLATE NOCASE)");
            s.execute("INSERT INTO contas (nome) VALUES ('Nubank')");
            s.execute("PRAGMA user_version = 1");
        }
        DatabaseManager.setUrl("jdbc:sqlite:" + arquivo);

        DatabaseManager.inicializar();

        var contas = new repository.ContaRepository().listarTodos();
        assertEquals(1, contas.size());
        assertEquals(new java.math.BigDecimal("0.00"), contas.get(0).getSaldoInicial());
        try (Stream<Path> arquivos = Files.list(dir)) {
            assertEquals(1, arquivos.filter(p -> p.getFileName().toString().startsWith("controle_financeiro-backup-v1-")).count());
        }
    }

    @Test
    void bancoNovoJaNasceNaVersaoAtual() throws Exception {
        Path arquivo = dir.resolve("novo.db");
        DatabaseManager.setUrl("jdbc:sqlite:" + arquivo);
        DatabaseManager.inicializar();

        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             ResultSet rs = c.createStatement().executeQuery("PRAGMA user_version")) {
            assertEquals(DatabaseManager.VERSAO_ESQUEMA, rs.getInt(1));
        }
        try (Stream<Path> arquivos = Files.list(dir)) {
            assertEquals(1, arquivos.count(), "banco novo não precisa de backup");
        }
    }
}
