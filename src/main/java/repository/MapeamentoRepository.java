package repository;

import db.DatabaseManager;
import model.MapeamentoDescricao;

import java.sql.*;
import java.util.List;

/** Regras aprendidas na importação de CSV, separadas por usuário. */
public class MapeamentoRepository {

    private final int usuarioId;

    public MapeamentoRepository(int usuarioId) { this.usuarioId = usuarioId; }

    public void salvar(MapeamentoDescricao m) {
        try (Connection conn = DatabaseManager.getConnection()) {
            salvar(conn, m);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar mapeamento: " + e.getMessage(), e);
        }
    }

    /** Insere ou substitui o mapeamento de mesmo padrão deste usuário, usando uma conexão já aberta. */
    public void salvar(Connection conn, MapeamentoDescricao m) throws SQLException {
        String sql = "INSERT OR REPLACE INTO mapeamentos_descricao (usuario_id, padrao, categoria_id, detalhe) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, m.getPadrao());
            ps.setInt(3, m.getCategoriaId());
            ps.setString(4, m.getDetalhe());
            ps.executeUpdate();
        }
    }

    public List<MapeamentoDescricao> listarTodos() {
        String sql = """
            SELECT m.id, m.padrao, m.categoria_id, c.nome AS categoria_nome, m.detalhe
            FROM mapeamentos_descricao m
            JOIN categorias c ON c.id = m.categoria_id
            WHERE m.usuario_id = ?
            ORDER BY m.padrao
        """;
        return Consultas.listar(sql, List.of(usuarioId), rs -> new MapeamentoDescricao(
                rs.getInt("id"), rs.getString("padrao"), rs.getInt("categoria_id"),
                rs.getString("categoria_nome"), rs.getString("detalhe")), "mapeamentos");
    }
}
