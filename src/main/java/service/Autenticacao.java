package service;

import model.Usuario;
import repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Cadastro e login de usuários: local (usuário/e-mail + senha) ou com conta Google. */
public class Autenticacao {

    public static final int TAMANHO_MINIMO_SENHA = 8;
    private static final Pattern USUARIO = Pattern.compile("[A-Za-z0-9._-]{3,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    /** Dados do formulário de cadastro local. */
    public record Cadastro(String nome, String usuario, String email, LocalDate nascimento, String senha) {}

    /** Dados devolvidos pelo Google após o login (ver {@link GoogleOAuth}). */
    public record PerfilGoogle(String id, String email, boolean emailVerificado, String nome) {}

    private final UsuarioRepository repo = new UsuarioRepository();

    // --- Login local ---

    public Usuario cadastrar(Cadastro c) {
        validar(c, 0);
        int id = repo.criar(c.nome().trim(), c.usuario().trim(), c.email().trim(), c.nascimento(),
                Senhas.gerarHash(c.senha()), null);
        repo.registrarLogin(id);
        return repo.buscarPorId(id).orElseThrow();
    }

    /** Login com nome de usuário ou e-mail. A mensagem de erro não diz qual dos dois estava errado. */
    public Usuario entrar(String login, String senha) {
        String l = login == null ? "" : login.trim();
        if (l.isEmpty() || senha == null || senha.isEmpty())
            throw new IllegalArgumentException("Informe o usuário (ou e-mail) e a senha.");
        Optional<Usuario> u = repo.buscarPorLogin(l);
        if (u.isPresent() && !u.get().temSenha() && u.get().isGoogleVinculado())
            throw new IllegalArgumentException("Esta conta entra com o Google. Use o botão \"Entrar com Google\".");
        Optional<String> hash = u.flatMap(x -> repo.hashSenha(x.getId()));
        // Calcula o hash mesmo sem usuário, para o tempo de resposta não revelar se o login existe.
        boolean ok = Senhas.confere(senha, hash.orElse(HASH_FALSO));
        if (u.isEmpty() || hash.isEmpty() || !ok)
            throw new IllegalArgumentException("Usuário ou senha incorretos.");
        repo.registrarLogin(u.get().getId());
        return u.get();
    }

    private static final String HASH_FALSO = Senhas.gerarHash("senha-inexistente");

    // --- Perfil de antes do login (dados migrados) ---

    /** Perfil com dados de antes do login que ainda precisa ganhar um acesso, se existir. */
    public Optional<Usuario> perfilSemAcesso() {
        return repo.buscarSemAcesso();
    }

    /** Cria usuário/senha para o perfil antigo, mantendo os dados dele. */
    public Usuario criarAcessoLocal(int perfilId, Cadastro c) {
        Usuario perfil = repo.buscarPorId(perfilId).orElseThrow();
        if (!perfil.semAcesso()) throw new IllegalStateException("Este perfil já tem acesso.");
        validar(c, perfilId);
        repo.atualizarPerfil(perfilId, c.nome().trim(), c.email().trim(), c.nascimento());
        repo.definirAcessoLocal(perfilId, c.usuario().trim(), Senhas.gerarHash(c.senha()));
        repo.registrarLogin(perfilId);
        return repo.buscarPorId(perfilId).orElseThrow();
    }

    // --- Google ---

    /**
     * Entra com a conta Google. Na primeira vez cria o usuário (sem senha local); se {@code vincularA}
     * for informado, liga a conta Google a esse usuário (perfil antigo ou usuário logado) em vez de criar outro.
     */
    public Usuario entrarComGoogle(PerfilGoogle g, Integer vincularA) {
        if (g.id() == null || g.id().isBlank()) throw new IllegalArgumentException("Resposta do Google sem identificador.");
        String email = g.emailVerificado() && g.email() != null ? g.email().trim().toLowerCase(Locale.ROOT) : null;
        Optional<Usuario> jaVinculado = repo.buscarPorGoogleId(g.id());

        if (vincularA != null) {
            if (jaVinculado.isPresent() && jaVinculado.get().getId() != vincularA)
                throw new IllegalArgumentException("Essa conta Google já está ligada a outro usuário do app.");
            if (email != null && repo.emailEmUso(email, vincularA)) email = null;   // não rouba e-mail de outro usuário
            repo.vincularGoogle(vincularA, g.id(), email);
            repo.registrarLogin(vincularA);
            return repo.buscarPorId(vincularA).orElseThrow();
        }

        if (jaVinculado.isPresent()) {
            repo.registrarLogin(jaVinculado.get().getId());
            return jaVinculado.get();
        }
        if (email != null && repo.buscarPorEmail(email).isPresent())
            throw new IllegalArgumentException("Já existe uma conta com o e-mail " + email + ". Entre com usuário e senha "
                    + "e use \"Vincular conta Google\" no seu perfil.");

        String nome = g.nome() != null && !g.nome().isBlank() ? g.nome().trim() : (email != null ? email : "Usuário Google");
        int id = repo.criar(nome, usuarioDisponivel(email != null ? email : nome), email, null, null, g.id());
        repo.registrarLogin(id);
        return repo.buscarPorId(id).orElseThrow();
    }

    // --- Perfil ---

    public Usuario atualizarPerfil(int id, String nome, String email, LocalDate nascimento) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Informe o nome.");
        String e = email == null || email.isBlank() ? null : email.trim();
        if (e != null && !EMAIL.matcher(e).matches()) throw new IllegalArgumentException("E-mail inválido.");
        if (e != null && repo.emailEmUso(e, id)) throw new IllegalArgumentException("Esse e-mail já está em uso.");
        if (nascimento != null && nascimento.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Data de nascimento no futuro.");
        repo.atualizarPerfil(id, nome.trim(), e, nascimento);
        return repo.buscarPorId(id).orElseThrow();
    }

    /**
     * Troca a senha (ou cria a primeira, para quem só usava o Google; nesse caso também define o usuário).
     * Quem já tem senha precisa informar a atual.
     */
    public void definirSenha(int id, String senhaAtual, String novaSenha, String usuarioSeNaoTiver) {
        Usuario u = repo.buscarPorId(id).orElseThrow();
        if (u.temSenha() && !Senhas.confere(senhaAtual, repo.hashSenha(id).orElse(null)))
            throw new IllegalArgumentException("Senha atual incorreta.");
        validarSenha(novaSenha);
        String login = u.getUsuario();
        if (login == null || login.isBlank()) {
            login = usuarioSeNaoTiver == null ? "" : usuarioSeNaoTiver.trim();
            validarUsuario(login, id);
        }
        repo.definirAcessoLocal(id, login, Senhas.gerarHash(novaSenha));
    }

    // --- Validações ---

    private void validar(Cadastro c, int idAtual) {
        if (c.nome() == null || c.nome().isBlank()) throw new IllegalArgumentException("Informe o nome.");
        validarUsuario(c.usuario() == null ? "" : c.usuario().trim(), idAtual);
        String email = c.email() == null ? "" : c.email().trim();
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Informe um e-mail válido.");
        if (repo.emailEmUso(email, idAtual)) throw new IllegalArgumentException("Esse e-mail já está em uso.");
        if (c.nascimento() != null && c.nascimento().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Data de nascimento no futuro.");
        validarSenha(c.senha());
    }

    private void validarUsuario(String usuario, int idAtual) {
        if (!USUARIO.matcher(usuario).matches())
            throw new IllegalArgumentException("Usuário deve ter de 3 a 30 caracteres: letras, números, ponto, hífen ou _.");
        if (repo.usuarioEmUso(usuario, idAtual)) throw new IllegalArgumentException("Esse nome de usuário já está em uso.");
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA)
            throw new IllegalArgumentException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
    }

    /** Gera um nome de usuário livre a partir do e-mail ou nome (ex: "ana.silva", "ana.silva2"). */
    private String usuarioDisponivel(String base) {
        String b = base.contains("@") ? base.substring(0, base.indexOf('@')) : base;
        b = java.text.Normalizer.normalize(b, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (b.length() < 3) b = (b + "usuario").substring(0, Math.max(3, b.length() + 7));
        if (b.length() > 26) b = b.substring(0, 26);
        String candidato = b;
        for (int n = 2; repo.usuarioEmUso(candidato, 0); n++) candidato = b + n;
        return candidato;
    }
}
