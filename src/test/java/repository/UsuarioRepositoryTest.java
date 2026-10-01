package repository;

import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRepositoryTest {

    private final UsuarioRepository repo = new UsuarioRepository();

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
    }

    @Test
    void deveBuscarUsuarioPrincipal() {
        Usuario usuario = repo.buscarPrincipal();
        assertEquals(1, usuario.getId());
        assertEquals("Usuário", usuario.getNome());
        assertNull(usuario.getDataNascimento());
        assertEquals(0, usuario.getIdade());
    }

    @Test
    void deveAtualizarUsuario() {
        repo.atualizar(new Usuario(1, "Hudson", LocalDate.of(1995, 5, 10)));

        Usuario resultado = repo.buscarPrincipal();
        assertEquals("Hudson", resultado.getNome());
        assertEquals(LocalDate.of(1995, 5, 10), resultado.getDataNascimento());
        assertTrue(resultado.getIdade() >= 30);
    }
}
