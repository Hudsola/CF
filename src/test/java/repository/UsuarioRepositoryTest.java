package repository;

import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRepositoryTest {

    private final UsuarioRepository repo = new UsuarioRepository();
    private int usuarioId;

    @BeforeEach
    void setUp() {
        usuarioId = DatabaseTestHelper.setup();
    }

    @Test
    void deveCriarUsuarioComCategoriasPadrao() {
        Usuario u = repo.buscarPorId(usuarioId).orElseThrow();
        assertEquals("teste", u.getUsuario());
        assertEquals("teste@teste.com", u.getEmail());
        assertTrue(u.temSenha());
        assertFalse(u.isGoogleVinculado());
        assertEquals(CategoriaRepository.PADRAO.size(), new CategoriaRepository(usuarioId).listarTodos().size());
    }

    @Test
    void deveBuscarPorUsuarioOuEmailSemDiferenciarMaiusculas() {
        assertEquals(usuarioId, repo.buscarPorLogin("TESTE").orElseThrow().getId());
        assertEquals(usuarioId, repo.buscarPorLogin("Teste@Teste.com").orElseThrow().getId());
        assertTrue(repo.buscarPorLogin("outro").isEmpty());
    }

    @Test
    void deveAtualizarPerfil() {
        repo.atualizarPerfil(usuarioId, "Hudson", "h@x.com", LocalDate.of(1995, 5, 10));
        Usuario u = repo.buscarPorId(usuarioId).orElseThrow();
        assertEquals("Hudson", u.getNome());
        assertEquals("h@x.com", u.getEmail());
        assertEquals(LocalDate.of(1995, 5, 10), u.getDataNascimento());
        assertTrue(u.getIdade() >= 30);
    }

    @Test
    void naoDeveAceitarUsuarioOuEmailRepetido() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repo.criar("Outro", "TESTE", "novo@x.com", null, "h", null));
        assertTrue(ex.getMessage().contains("nome de usuário"));
        ex = assertThrows(RuntimeException.class, () -> repo.criar("Outro", "novo", "TESTE@teste.com", null, "h", null));
        assertTrue(ex.getMessage().contains("e-mail"));
    }

    @Test
    void googleSemSenhaNaoContaComoPerfilSemAcesso() {
        int id = repo.criar("G", "g", "g@x.com", null, null, "google-123");
        assertFalse(repo.buscarPorId(id).orElseThrow().semAcesso());
        assertTrue(repo.buscarSemAcesso().isEmpty());
        assertEquals(id, repo.buscarPorGoogleId("google-123").orElseThrow().getId());
    }
}
