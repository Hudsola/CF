package repository;

import model.Usuario;
import org.junit.jupiter.api.*;
import util.DatabaseTestHelper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsuarioRepositoryTest {

    private final UsuarioRepository repo = new UsuarioRepository();

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
    }

    @Test
    @Order(1)
    void deveBuscarUsuarioPrincipal() {
        Usuario usuario = repo.buscarPrincipal();

        assertNotNull(usuario);
        assertEquals(1, usuario.getId());
        assertEquals("Usuário", usuario.getNome());
        assertNull(usuario.getDataNascimento());
        assertEquals(1, usuario.getNivel());
        assertEquals(0, usuario.getXp());
        assertEquals(100, usuario.getXpProximoNivel());
    }

    @Test
    @Order(2)
    void deveAtualizarUsuario() {
        Usuario atualizado = new Usuario(
                1,
                "Hudson",
                LocalDate.of(1995, 5, 10),
                3,
                250,
                500
        );

        repo.atualizar(atualizado);

        Usuario resultado = repo.buscarPrincipal();

        assertEquals(1, resultado.getId());
        assertEquals("Hudson", resultado.getNome());
        assertEquals(
                LocalDate.of(1995, 5, 10),
                resultado.getDataNascimento()
        );
        assertEquals(3, resultado.getNivel());
        assertEquals(250, resultado.getXp());
        assertEquals(500, resultado.getXpProximoNivel());
    }
}