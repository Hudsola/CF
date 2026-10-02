package repository;

import db.DatabaseManager;
import model.Usuario;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acesso à tabela usuarios (não é filtrado por usuário: é usado no login e no cadastro). */
public class UsuarioRepository {

    private static final String SELECT = "SELECT id, nome, data_nascimento, usuario, email, "
            + "senha_hash IS NOT NULL AS tem_senha, google_id IS NOT NULL AS tem_google FROM usuarios ";

    public Optional<Usuario> buscarPorId(int id) {
        return primeiro(SELECT + "WHERE id = ?", id);
    }

    /** Login local: aceita o nome de usuário ou o e-mail (sem diferenciar maiúsculas). */
    public Optional<Usuario> buscarPorLogin(String login) {
        return primeiro(SELECT + "WHERE usuario = ? COLLATE NOCASE OR email = ? COLLATE NOCASE", login, login);
    }

    public Optional<Usuario> buscarPorGoogleId(String googleId) {
        return primeiro(SELECT + "WHERE google_id = ?", googleId);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return primeiro(SELECT + "WHERE email = ? COLLATE NOCASE", email);
    }

    public boolean usuarioEmUso(String usuario, int exceto) {
        return Consultas.existe("SELECT 1 FROM usuarios WHERE usuario = ? COLLATE NOCASE AND id <> ?", usuario, exceto);
    }

    public boolean emailEmUso(String email, int exceto) {
        return Consultas.existe("SELECT 1 FROM usuarios WHERE email = ? COLLATE NOCASE AND id <> ?", email, exceto);
    }

    /** Perfil de antes do login (dados migrados), ainda sem senha nem Google. */
    public Optional<Usuario> buscarSemAcesso() {
        return primeiro(SELECT + "WHERE senha_hash IS NULL AND google_id IS NULL ORDER BY id LIMIT 1");
    }

    public Optional<String> hashSenha(int id) {
        List<String> r = Consultas.listar("SELECT senha_hash FROM usuarios WHERE id = ?", List.of(id),
                rs -> rs.getString(1), "usuários");
        return r.isEmpty() ? Optional.empty() : Optional.ofNullable(r.get(0));
    }

    /**
     * Cria o usuário e as categorias padrão dele numa única transação. Devolve o id.
     * {@code senhaHash} e {@code googleId} podem ser nulos (um dos dois deve existir para conseguir entrar).
     */
    public int criar(String nome, String usuario, String email, LocalDate nascimento, String senhaHash, String googleId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO usuarios (nome, usuario, email, data_nascimento, senha_hash, google_id, criado_em) "
                                + "VALUES (?,?,?,?,?,?, datetime('now','localtime'))", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nome); ps.setString(2, usuario); ps.setString(3, email);
                    ps.setString(4, nascimento != null ? nascimento.toString() : null);
                    ps.setString(5, senhaHash); ps.setString(6, googleId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                }
                new CategoriaRepository(id).criarPadroes(conn);
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(mensagemUnicidade(e, "Erro ao criar usuário"), e);
        }
    }

    public void atualizarPerfil(int id, String nome, String email, LocalDate nascimento) {
        executar("UPDATE usuarios SET nome=?, email=?, data_nascimento=? WHERE id=?",
                nome, email, nascimento != null ? nascimento.toString() : null, id);
    }

    /** Define login e senha (cadastro do acesso do perfil antigo ou troca de senha). */
    public void definirAcessoLocal(int id, String usuario, String senhaHash) {
        executar("UPDATE usuarios SET usuario=?, senha_hash=? WHERE id=?", usuario, senhaHash, id);
    }

    public void vincularGoogle(int id, String googleId, String email) {
        executar("UPDATE usuarios SET google_id=?, email=COALESCE(email, ?) WHERE id=?", googleId, email, id);
    }

    public void registrarLogin(int id) {
        executar("UPDATE usuarios SET ultimo_login = datetime('now','localtime') WHERE id=?", id);
    }

    private void executar(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Usuário não encontrado.");
        } catch (SQLException e) {
            throw new RuntimeException(mensagemUnicidade(e, "Erro ao atualizar usuário"), e);
        }
    }

    private static String mensagemUnicidade(SQLException e, String padrao) {
        String m = e.getMessage();
        if (m != null && m.contains("UNIQUE")) {
            if (m.contains("usuarios.usuario")) return "Esse nome de usuário já está em uso.";
            if (m.contains("usuarios.email")) return "Esse e-mail já está em uso.";
            if (m.contains("usuarios.google_id")) return "Essa conta Google já está vinculada a outro usuário.";
        }
        return padrao + ": " + m;
    }

    private Optional<Usuario> primeiro(String sql, Object... params) {
        List<Usuario> r = Consultas.listar(sql, List.of(params), rs -> {
            String nasc = rs.getString("data_nascimento");
            return new Usuario(rs.getInt("id"), rs.getString("nome"), nasc != null ? LocalDate.parse(nasc) : null,
                    rs.getString("usuario"), rs.getString("email"), rs.getBoolean("tem_senha"), rs.getBoolean("tem_google"));
        }, "usuários");
        return r.stream().findFirst();
    }
}
