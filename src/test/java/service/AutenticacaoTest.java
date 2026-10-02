package service;

import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.CategoriaRepository;
import repository.UsuarioRepository;
import util.DatabaseTestHelper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AutenticacaoTest {

    private final Autenticacao auth = new Autenticacao();

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();   // já existe o usuário "teste"
    }

    private Autenticacao.Cadastro cadastro(String usuario, String email, String senha) {
        return new Autenticacao.Cadastro("Ana Silva", usuario, email, LocalDate.of(1990, 3, 4), senha);
    }

    @Test
    void senhaEhGuardadaComoHashComSaltEConfereSoComASenhaCerta() {
        String h1 = Senhas.gerarHash("minhaSenha123");
        String h2 = Senhas.gerarHash("minhaSenha123");
        assertTrue(h1.startsWith("pbkdf2_sha256$" + Senhas.ITERACOES + "$"));
        assertNotEquals(h1, h2, "salt diferente a cada hash");
        assertFalse(h1.contains("minhaSenha123"));
        assertTrue(Senhas.confere("minhaSenha123", h1));
        assertFalse(Senhas.confere("minhaSenha124", h1));
        assertFalse(Senhas.confere("x", "lixo"));
        assertFalse(Senhas.confere("x", null));
    }

    @Test
    void deveCadastrarEEntrarPorUsuarioOuEmail() {
        Usuario u = auth.cadastrar(cadastro("ana", "ana@x.com", "senha-forte"));
        assertEquals("Ana Silva", u.getNome());
        assertTrue(u.temSenha());
        assertEquals(CategoriaRepository.PADRAO.size(), new CategoriaRepository(u.getId()).listarTodos().size());

        assertEquals(u.getId(), auth.entrar("ana", "senha-forte").getId());
        assertEquals(u.getId(), auth.entrar("ANA@X.COM", "senha-forte").getId());
    }

    @Test
    void loginErradoNaoRevelaSeOUsuarioExiste() {
        auth.cadastrar(cadastro("ana", "ana@x.com", "senha-forte"));
        String senhaErrada = assertThrows(IllegalArgumentException.class, () -> auth.entrar("ana", "errada123")).getMessage();
        String semUsuario = assertThrows(IllegalArgumentException.class, () -> auth.entrar("ninguem", "errada123")).getMessage();
        assertEquals("Usuário ou senha incorretos.", senhaErrada);
        assertEquals(senhaErrada, semUsuario);
    }

    @Test
    void deveValidarCadastro() {
        assertThrows(IllegalArgumentException.class, () -> auth.cadastrar(cadastro("ana", "ana@x.com", "curta")));
        assertThrows(IllegalArgumentException.class, () -> auth.cadastrar(cadastro("a b", "ana@x.com", "senha-forte")));
        assertThrows(IllegalArgumentException.class, () -> auth.cadastrar(cadastro("ana", "sem-arroba", "senha-forte")));
        assertThrows(IllegalArgumentException.class, () -> auth.cadastrar(cadastro("TESTE", "ana@x.com", "senha-forte")),
                "usuário já usado (sem diferenciar maiúsculas)");
        assertThrows(IllegalArgumentException.class, () -> auth.cadastrar(cadastro("ana", "teste@teste.com", "senha-forte")),
                "e-mail já usado");
    }

    @Test
    void deveCriarAcessoParaOPerfilAntigoMantendoOsDados() {
        // perfil "de antes do login": sem senha e sem Google, com uma conta
        int antigo = new UsuarioRepository().criar("Hudsola", null, null, LocalDate.of(2001, 1, 1), null, null);
        new ControleFinanceiro(antigo).salvarConta(new model.Conta("Nubank"));
        assertEquals(antigo, auth.perfilSemAcesso().orElseThrow().getId());

        Usuario u = auth.criarAcessoLocal(antigo, new Autenticacao.Cadastro("Hudson", "hudson", "h@x.com",
                LocalDate.of(2001, 1, 1), "senha-forte"));

        assertEquals(antigo, u.getId());
        assertEquals("Hudson", u.getNome());
        assertTrue(auth.perfilSemAcesso().isEmpty());
        assertEquals("Nubank", new ControleFinanceiro(u.getId()).getContas().get(0).getNome());
        assertEquals(antigo, auth.entrar("hudson", "senha-forte").getId());
    }

    @Test
    void primeiroLoginGoogleCriaUsuarioSemSenhaEOsSeguintesReutilizam() {
        var g = new Autenticacao.PerfilGoogle("google-sub-1", "Maria.Souza@gmail.com", true, "Maria Souza");

        Usuario u = auth.entrarComGoogle(g, null);
        assertEquals("Maria Souza", u.getNome());
        assertEquals("maria.souza@gmail.com", u.getEmail());
        assertEquals("maria.souza", u.getUsuario());
        assertFalse(u.temSenha());
        assertTrue(u.isGoogleVinculado());
        assertEquals(CategoriaRepository.PADRAO.size(), new CategoriaRepository(u.getId()).listarTodos().size());

        assertEquals(u.getId(), auth.entrarComGoogle(g, null).getId());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> auth.entrar("maria.souza", "qualquer1"));
        assertTrue(ex.getMessage().contains("Google"));
    }

    @Test
    void loginGoogleComEmailDeContaLocalExistentePedeParaVincular() {
        var g = new Autenticacao.PerfilGoogle("google-sub-2", "teste@teste.com", true, "Teste");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> auth.entrarComGoogle(g, null));
        assertTrue(ex.getMessage().contains("Vincular conta Google"));
    }

    @Test
    void deveVincularGoogleAUmUsuarioExistenteEDepoisEntrarPorEle() {
        int id = new UsuarioRepository().buscarPorLogin("teste").orElseThrow().getId();
        var g = new Autenticacao.PerfilGoogle("google-sub-3", "teste@teste.com", true, "Teste");

        auth.entrarComGoogle(g, id);

        assertEquals(id, auth.entrarComGoogle(g, null).getId());
        int outro = DatabaseTestHelper.novoUsuario("outro");
        assertThrows(IllegalArgumentException.class, () -> auth.entrarComGoogle(g, outro),
                "a mesma conta Google não pode ficar em dois usuários");
    }

    @Test
    void usuarioDoGoogleSemSenhaPodeCriarUmaDepois() {
        Usuario u = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("google-sub-4", "z@gmail.com", true, "Zé"), null);
        auth.definirSenha(u.getId(), null, "nova-senha-1", null);
        assertEquals("zusuario", u.getUsuario(), "login gerado do e-mail, completado até o mínimo de 3 letras");
        assertEquals(u.getId(), auth.entrar(u.getUsuario(), "nova-senha-1").getId());

        assertThrows(IllegalArgumentException.class, () -> auth.definirSenha(u.getId(), "errada", "outra-senha-2", null),
                "com senha definida, trocar exige a senha atual");
        auth.definirSenha(u.getId(), "nova-senha-1", "outra-senha-2", null);
        assertEquals(u.getId(), auth.entrar(u.getUsuario(), "outra-senha-2").getId());
    }
}
