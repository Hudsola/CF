package db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PastaDadosTest {

    @TempDir Path dir;

    @Test
    void pastaDadosEhAPastaDoBancoEmUso() {
        Path banco = dir.resolve("sub").resolve("teste.db");
        DatabaseManager.setUrl("jdbc:sqlite:" + banco);
        assertEquals(banco.toAbsolutePath(), DatabaseManager.arquivoBanco());
        assertEquals(banco.getParent().toAbsolutePath(), DatabaseManager.pastaDados());
    }

    @Test
    void deveCriarPastaDeDadosEApontarParaOBancoDentroDela() {
        Path dados = dir.resolve("MyCareer");
        Path banco = DatabaseManager.prepararArquivo(dados, List.of(dir.resolve("ControleFinanceiro"), dir.resolve("programa")));

        assertTrue(Files.isDirectory(dados));
        assertEquals(dados.resolve("mycareer.db").toAbsolutePath(), banco);
        assertFalse(Files.exists(banco), "sem banco antigo, nada é copiado");
    }

    @Test
    void migraDaPastaDoControleFinanceiroComGoogleEFotosSemApagarNada() throws Exception {
        Path antiga = Files.createDirectories(dir.resolve("ControleFinanceiro"));
        Files.writeString(antiga.resolve("controle_financeiro.db"), "dados antigos");
        Files.writeString(antiga.resolve("google-oauth.json"), "{\"installed\":{}}");
        Files.createDirectories(antiga.resolve("fotos"));
        Files.writeString(antiga.resolve("fotos").resolve("usuario-1.png"), "foto");
        Files.writeString(antiga.resolve("controle_financeiro-backup-v2.db"), "backup");
        Path dados = dir.resolve("MyCareer");

        Path banco = DatabaseManager.prepararArquivo(dados, List.of(antiga));

        assertEquals("dados antigos", Files.readString(banco));
        assertEquals("{\"installed\":{}}", Files.readString(dados.resolve("google-oauth.json")));
        assertEquals("foto", Files.readString(dados.resolve("fotos").resolve("usuario-1.png")));
        assertFalse(Files.exists(dados.resolve("controle_financeiro-backup-v2.db")), "backups antigos ficam onde estão");
        assertTrue(Files.exists(antiga.resolve("controle_financeiro.db")), "o original fica intacto");
        assertTrue(Files.exists(antiga.resolve("fotos").resolve("usuario-1.png")));
    }

    @Test
    void bancoComONomeAntigoNaPropriaPastaEhCopiadoParaONomeNovo() throws Exception {
        Path dados = Files.createDirectories(dir.resolve("PastaDaVariavel"));
        Files.writeString(dados.resolve("controle_financeiro.db"), "dados");

        Path banco = DatabaseManager.prepararArquivo(dados, List.of(dir.resolve("ControleFinanceiro")));

        assertEquals(dados.resolve("mycareer.db").toAbsolutePath(), banco);
        assertEquals("dados", Files.readString(banco));
        assertTrue(Files.exists(dados.resolve("controle_financeiro.db")));
    }

    @Test
    void primeiraPastaAntigaComBancoTemPrioridade() throws Exception {
        Path antiga = Files.createDirectories(dir.resolve("ControleFinanceiro"));
        Files.writeString(antiga.resolve("controle_financeiro.db"), "pasta do usuário");
        Path projeto = Files.createDirectories(dir.resolve("projeto"));
        Files.writeString(projeto.resolve("controle_financeiro.db"), "pasta do programa");

        Path banco = DatabaseManager.prepararArquivo(dir.resolve("MyCareer"), List.of(antiga, projeto));

        assertEquals("pasta do usuário", Files.readString(banco));
    }

    @Test
    void deveCopiarBancoDaPastaDoProgramaQuandoNaoHaOutro() throws Exception {
        Path projeto = Files.createDirectories(dir.resolve("projeto"));
        Files.writeString(projeto.resolve("controle_financeiro.db"), "dados antigos");

        Path banco = DatabaseManager.prepararArquivo(dir.resolve("MyCareer"),
                List.of(dir.resolve("ControleFinanceiro"), projeto));

        assertEquals("dados antigos", Files.readString(banco));
        assertTrue(Files.exists(projeto.resolve("controle_financeiro.db")));
    }

    @Test
    void naoDeveSobrescreverBancoNemArquivosQueJaExistemNaPastaNova() throws Exception {
        Path antiga = Files.createDirectories(dir.resolve("ControleFinanceiro"));
        Files.writeString(antiga.resolve("controle_financeiro.db"), "antigo");
        Files.writeString(antiga.resolve("google-oauth.json"), "antigo");
        Path dados = Files.createDirectories(dir.resolve("MyCareer"));
        Files.writeString(dados.resolve("mycareer.db"), "atual");

        Path banco = DatabaseManager.prepararArquivo(dados, List.of(antiga));

        assertEquals("atual", Files.readString(banco));
        assertFalse(Files.exists(dados.resolve("google-oauth.json")), "com banco novo já existente, não migra nada");

        // banco novo ausente, mas google-oauth.json novo já presente: copia o banco e mantém o json novo
        Files.delete(dados.resolve("mycareer.db"));
        Files.writeString(dados.resolve("google-oauth.json"), "novo");
        DatabaseManager.prepararArquivo(dados, List.of(antiga));
        assertEquals("antigo", Files.readString(dados.resolve("mycareer.db")));
        assertEquals("novo", Files.readString(dados.resolve("google-oauth.json")));
    }
}
