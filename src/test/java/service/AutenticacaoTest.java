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

    // --- Casos de erro e limites ---

    @Test
    void entrarSemUsuarioOuSemSenhaPedeOsDois() {
        auth.cadastrar(cadastro("ana", "ana@x.com", "senha-forte"));
        for (String[] par : new String[][]{{null, "senha-forte"}, {"   ", "senha-forte"}, {"ana", null}, {"ana", ""}}) {
            var ex = assertThrows(IllegalArgumentException.class, () -> auth.entrar(par[0], par[1]));
            assertEquals("Informe o usuário (ou e-mail) e a senha.", ex.getMessage());
        }
    }

    @Test
    void entrarIgnoraEspacosEmVoltaDoLogin() {
        Usuario u = auth.cadastrar(cadastro("ana", "ana@x.com", "senha-forte"));
        assertEquals(u.getId(), auth.entrar("  ana  ", "senha-forte").getId());
    }

    @Test
    void perfilSemSenhaNemGoogleNaoEntraComSenha() {
        new UsuarioRepository().criar("Antigo", "antigo", null, null, null, null);
        var ex = assertThrows(IllegalArgumentException.class, () -> auth.entrar("antigo", "qualquer-senha"));
        assertEquals("Usuário ou senha incorretos.", ex.getMessage());
    }

    @Test
    void cadastroRecusaCadaCampoInvalidoComAMensagemCerta() {
        LocalDate amanha = LocalDate.now().plusDays(1);
        assertMensagem("Informe o nome.", new Autenticacao.Cadastro(null, "ana", "ana@x.com", null, "senha-forte"));
        assertMensagem("Informe o nome.", new Autenticacao.Cadastro("  ", "ana", "ana@x.com", null, "senha-forte"));
        assertMensagem("Usuário deve ter", new Autenticacao.Cadastro("Ana", null, "ana@x.com", null, "senha-forte"));
        assertMensagem("Usuário deve ter", new Autenticacao.Cadastro("Ana", "ab", "ana@x.com", null, "senha-forte"));
        assertMensagem("Usuário deve ter", new Autenticacao.Cadastro("Ana", "a".repeat(31), "ana@x.com", null, "senha-forte"));
        assertMensagem("Usuário deve ter", new Autenticacao.Cadastro("Ana", "ana!", "ana@x.com", null, "senha-forte"));
        assertMensagem("Informe um e-mail válido.", new Autenticacao.Cadastro("Ana", "ana", null, null, "senha-forte"));
        assertMensagem("Informe um e-mail válido.", new Autenticacao.Cadastro("Ana", "ana", "ana@x", null, "senha-forte"));
        assertMensagem("Esse e-mail já está em uso.", new Autenticacao.Cadastro("Ana", "ana", "TESTE@teste.com", null, "senha-forte"));
        assertMensagem("Data de nascimento no futuro.", new Autenticacao.Cadastro("Ana", "ana", "ana@x.com", amanha, "senha-forte"));
        assertMensagem("A senha deve ter", new Autenticacao.Cadastro("Ana", "ana", "ana@x.com", null, null));
        assertMensagem("A senha deve ter", new Autenticacao.Cadastro("Ana", "ana", "ana@x.com", null, "1234567"));
        assertTrue(new UsuarioRepository().buscarPorLogin("ana").isEmpty(), "nenhum cadastro inválido foi gravado");

        // limites aceitos: usuário com 3 e 30 caracteres, senha com exatamente 8, nascimento hoje
        auth.cadastrar(new Autenticacao.Cadastro("Ana", "a.b", "a1@x.com", LocalDate.now(), "12345678"));
        auth.cadastrar(new Autenticacao.Cadastro("Bia", "b".repeat(30), "b1@x.com", null, "12345678"));
    }

    private void assertMensagem(String inicio, Autenticacao.Cadastro c) {
        var ex = assertThrows(IllegalArgumentException.class, () -> auth.cadastrar(c));
        assertTrue(ex.getMessage().startsWith(inicio), "esperado \"" + inicio + "…\", veio \"" + ex.getMessage() + "\"");
    }

    @Test
    void criarAcessoParaPerfilQueJaTemAcessoEhRecusado() {
        int id = new UsuarioRepository().buscarPorLogin("teste").orElseThrow().getId();
        assertThrows(IllegalStateException.class, () -> auth.criarAcessoLocal(id, cadastro("novo", "n@x.com", "senha-forte")));
    }

    @Test
    void criarAcessoDoPerfilAntigoValidaOsDados() {
        int antigo = new UsuarioRepository().criar("Antigo", null, null, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> auth.criarAcessoLocal(antigo, cadastro("teste", "n@x.com", "senha-forte")), "usuário já em uso");
        assertTrue(auth.perfilSemAcesso().isPresent(), "continua sem acesso depois do erro");
    }

    @Test
    void googleSemIdentificadorEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.entrarComGoogle(new Autenticacao.PerfilGoogle(null, "a@x.com", true, "A"), null));
        assertThrows(IllegalArgumentException.class,
                () -> auth.entrarComGoogle(new Autenticacao.PerfilGoogle(" ", "a@x.com", true, "A"), null));
    }

    @Test
    void googleComEmailNaoVerificadoNaoGuardaOEmail() {
        Usuario u = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("g-nv", "teste@teste.com", false, "Fulano"), null);
        assertNull(u.getEmail(), "e-mail não verificado pelo Google não é usado (nem conflita com outra conta)");
        assertEquals("fulano", u.getUsuario());
    }

    @Test
    void googleSemNomeUsaOEmailESemNadaUsaNomePadrao() {
        Usuario comEmail = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("g-1", "joao@x.com", true, " "), null);
        assertEquals("joao@x.com", comEmail.getNome());
        Usuario semNada = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("g-2", null, false, null), null);
        assertEquals("Usuário Google", semNada.getNome());
        assertTrue(semNada.getUsuario().startsWith("usuariogoogle"));
    }

    @Test
    void googleGeraUsuarioLivreSemAcentoEQuandoJaExisteAcrescentaNumero() {
        auth.cadastrar(cadastro("jose.maria", "outro@x.com", "senha-forte"));
        Usuario u = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("g-3", "José.María@x.com", true, "José"), null);
        assertEquals("jose.maria2", u.getUsuario());
        Usuario longo = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("g-4", "a".repeat(40) + "@x.com", true, "A"), null);
        assertEquals(26, longo.getUsuario().length());
    }

    @Test
    void vincularGoogleNaoTomaOEmailDeOutroUsuario() {
        int id = DatabaseTestHelper.novoUsuario("dono");
        Usuario u = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("g-5", "teste@teste.com", true, "X"), id);
        assertEquals("dono@teste.com", u.getEmail(), "mantém o e-mail próprio; o do Google pertence ao usuário \"teste\"");
        assertTrue(u.isGoogleVinculado());
    }

    @Test
    void atualizarPerfilValidaETrataEmailVazioComoSemEmail() {
        int id = new UsuarioRepository().buscarPorLogin("teste").orElseThrow().getId();
        int outro = DatabaseTestHelper.novoUsuario("outro");
        assertThrows(IllegalArgumentException.class, () -> auth.atualizarPerfil(id, " ", null, null));
        assertThrows(IllegalArgumentException.class, () -> auth.atualizarPerfil(id, "T", "invalido", null));
        assertThrows(IllegalArgumentException.class, () -> auth.atualizarPerfil(id, "T", "outro@teste.com", null));
        assertThrows(IllegalArgumentException.class, () -> auth.atualizarPerfil(id, "T", null, LocalDate.now().plusDays(1)));

        Usuario u = auth.atualizarPerfil(id, "  Novo Nome ", "  ", LocalDate.of(2000, 2, 29));
        assertEquals("Novo Nome", u.getNome());
        assertNull(u.getEmail());
        assertEquals(LocalDate.of(2000, 2, 29), u.getDataNascimento());
        assertEquals("outro@teste.com", new UsuarioRepository().buscarPorId(outro).orElseThrow().getEmail());
    }

    @Test
    void definirSenhaValidaSenhaCurtaEUsuarioDeQuemNaoTem() {
        Usuario g = auth.entrarComGoogle(new Autenticacao.PerfilGoogle("g-6", null, false, "ab"), null);
        new UsuarioRepository().definirAcessoLocal(g.getId(), null, null);   // Google sem nome de usuário
        assertThrows(IllegalArgumentException.class, () -> auth.definirSenha(g.getId(), null, "curta", "novo.login"));
        assertThrows(IllegalArgumentException.class, () -> auth.definirSenha(g.getId(), null, "senha-longa-1", null),
                "quem não tem usuário precisa escolher um");
        assertThrows(IllegalArgumentException.class, () -> auth.definirSenha(g.getId(), null, "senha-longa-1", "teste"),
                "usuário já em uso");

        auth.definirSenha(g.getId(), null, "senha-longa-1", " novo.login ");
        assertEquals(g.getId(), auth.entrar("novo.login", "senha-longa-1").getId());
    }
}
