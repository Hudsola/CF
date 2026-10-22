package repository;

import db.DatabaseManager;
import model.Produto;
import model.UnidadeMedida;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

/** Produtos do estoque do usuário informado no construtor; nenhuma operação enxerga produtos de outros usuários. */
public class ProdutoRepository {

    private final int usuarioId;

    public ProdutoRepository(int usuarioId) { this.usuarioId = usuarioId; }

    public List<Produto> listarTodos() {
        return Consultas.listar("SELECT * FROM produtos WHERE usuario_id = ?", List.of(usuarioId),
                ProdutoRepository::ler, "produtos");
    }

    public Optional<Produto> buscar(int id) {
        return Consultas.listar("SELECT * FROM produtos WHERE id = ? AND usuario_id = ?", List.of(id, usuarioId),
                ProdutoRepository::ler, "produtos").stream().findFirst();
    }

    /** true se outro produto deste usuário já usa o código (sem diferenciar maiúsculas). */
    public boolean codigoEmUso(String codigo, int exceto) {
        return Consultas.existe("SELECT 1 FROM produtos WHERE usuario_id = ? AND codigo = ? COLLATE NOCASE AND id <> ?",
                usuarioId, codigo, exceto);
    }

    /** Códigos no formato P0001 já usados, para sugerir o próximo. */
    public List<String> codigosAutomaticos() {
        return Consultas.listar("SELECT codigo FROM produtos WHERE usuario_id = ? AND codigo GLOB 'P[0-9]*'",
                List.of(usuarioId), rs -> rs.getString(1), "códigos");
    }

    public int salvar(Produto p) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO produtos (usuario_id, codigo, descricao, quantidade, unidade) VALUES (?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, p.codigo()); ps.setString(3, p.descricao());
            ps.setString(4, p.quantidade().toPlainString()); ps.setString(5, p.unidade().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) {
            throw new RuntimeException(mensagem(e, p.codigo(), "Erro ao salvar produto"), e);
        }
    }

    public void atualizar(Produto p) {
        executar("UPDATE produtos SET codigo=?, descricao=?, quantidade=?, unidade=? WHERE id=? AND usuario_id=?",
                p.codigo(), p.descricao(), p.quantidade().toPlainString(), p.unidade().name(), p.id(), usuarioId);
    }

    public void definirQuantidade(int id, BigDecimal quantidade) {
        executar("UPDATE produtos SET quantidade=? WHERE id=? AND usuario_id=?", quantidade.toPlainString(), id, usuarioId);
    }

    public void excluir(int id) {
        executar("DELETE FROM produtos WHERE id=? AND usuario_id=?", id, usuarioId);
    }

    private void executar(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Produto não encontrado.");
        } catch (SQLException e) {
            throw new RuntimeException(mensagem(e, String.valueOf(params[0]), "Erro ao gravar produto"), e);
        }
    }

    private static String mensagem(SQLException e, String codigo, String padrao) {
        return e.getMessage() != null && e.getMessage().contains("UNIQUE")
                ? "Já existe um produto com o código " + codigo + "." : padrao + ": " + e.getMessage();
    }

    private static Produto ler(ResultSet rs) throws SQLException {
        return new Produto(rs.getInt("id"), rs.getString("codigo"), rs.getString("descricao"),
                new BigDecimal(rs.getString("quantidade")), UnidadeMedida.valueOf(rs.getString("unidade")));
    }
}
