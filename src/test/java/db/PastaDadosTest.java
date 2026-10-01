package db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PastaDadosTest {

    @TempDir Path dir;

    @Test
    void deveCriarPastaDeDadosEApontarParaOBancoDentroDela() {
        Path dados = dir.resolve("ControleFinanceiro");
        Path banco = DatabaseManager.prepararArquivo(dados, dir.resolve("programa"));

        assertTrue(Files.isDirectory(dados));
        assertEquals(dados.resolve("controle_financeiro.db").toAbsolutePath(), banco);
        assertFalse(Files.exists(banco), "sem banco antigo, nada é copiado");
    }

    @Test
    void deveCopiarBancoDaPastaAtualNaPrimeiraVezSemApagarOOriginal() throws Exception {
        Path atual = Files.createDirectories(dir.resolve("projeto"));
        Files.writeString(atual.resolve("controle_financeiro.db"), "dados antigos");
        Path dados = dir.resolve("ControleFinanceiro");

        Path banco = DatabaseManager.prepararArquivo(dados, atual);

        assertEquals("dados antigos", Files.readString(banco));
        assertTrue(Files.exists(atual.resolve("controle_financeiro.db")));
    }

    @Test
    void naoDeveSobrescreverBancoQueJaExisteNaPastaDeDados() throws Exception {
        Path atual = Files.createDirectories(dir.resolve("projeto"));
        Files.writeString(atual.resolve("controle_financeiro.db"), "antigo");
        Path dados = Files.createDirectories(dir.resolve("ControleFinanceiro"));
        Files.writeString(dados.resolve("controle_financeiro.db"), "atual");

        Path banco = DatabaseManager.prepararArquivo(dados, atual);

        assertEquals("atual", Files.readString(banco));
    }
}
