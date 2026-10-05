package service;

import db.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FotoPerfilTest {

    @TempDir Path pasta;

    @BeforeEach
    void setUp() {
        DatabaseManager.setUrl("jdbc:sqlite:" + pasta.resolve("teste.db"));
    }

    @Test
    void semFotoNaoDevolveArquivo() {
        assertTrue(FotoPerfil.arquivo(1).isEmpty());
        assertFalse(FotoPerfil.temFotoEnviada(1));
    }

    @Test
    void fotoEnviadaECopiadaEPorUsuario() throws Exception {
        Path origem = Files.write(pasta.resolve("eu.PNG"), new byte[]{1, 2, 3});
        FotoPerfil.salvarEnviada(1, origem);

        Path salva = FotoPerfil.arquivo(1).orElseThrow();
        assertEquals("usuario-1.png", salva.getFileName().toString());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(salva));
        assertTrue(FotoPerfil.arquivo(2).isEmpty());
    }

    @Test
    void novaFotoSubstituiAAnteriorMesmoComOutraExtensao() throws Exception {
        FotoPerfil.salvarEnviada(1, Files.write(pasta.resolve("a.png"), new byte[]{1}));
        FotoPerfil.salvarEnviada(1, Files.write(pasta.resolve("b.jpg"), new byte[]{2}));

        assertEquals("usuario-1.jpg", FotoPerfil.arquivo(1).orElseThrow().getFileName().toString());
        assertFalse(Files.exists(pasta.resolve("fotos").resolve("usuario-1.png")));
    }

    @Test
    void fotoEnviadaTemPrioridadeSobreADoGoogle() throws Exception {
        Files.createDirectories(pasta.resolve("fotos"));
        Path google = Files.write(pasta.resolve("fotos").resolve("google-1.img"), new byte[]{9});
        assertEquals(google, FotoPerfil.arquivo(1).orElseThrow());

        FotoPerfil.salvarEnviada(1, Files.write(pasta.resolve("eu.png"), new byte[]{1}));
        assertEquals("usuario-1.png", FotoPerfil.arquivo(1).orElseThrow().getFileName().toString());

        FotoPerfil.removerEnviada(1);
        assertEquals(google, FotoPerfil.arquivo(1).orElseThrow());
    }

    @Test
    void recusaFormatoNaoSuportado() throws Exception {
        Path origem = Files.write(pasta.resolve("doc.pdf"), new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> FotoPerfil.salvarEnviada(1, origem));
    }

    @Test
    void urlDoGoogleInvalidaEIgnorada() {
        FotoPerfil.baixarGoogle(1, null);
        FotoPerfil.baixarGoogle(1, "http://inseguro/foto.png");
        assertTrue(FotoPerfil.arquivo(1).isEmpty());
    }
}
