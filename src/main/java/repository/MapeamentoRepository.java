package repository;

import db.DatabaseManager;
import model.MapeamentoDescricao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MapeamentoRepository {

    public void salvar(MapeamentoDescricao m) {
        try (Connection conn = DatabaseManager.getConnection()) {
            salvar(conn, m);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar mapeamento: " + e.getMessage(), e);
        }
    }

    /** Insere ou substitui o mapeamento de mesmo padrão, usando uma conexão já aberta. */
    public void salvar(Connection conn, MapeamentoDescricao m) throws SQLException {
        String sql = "INSERT OR REPLACE INTO mapeamentos_descricao (padrao, categoria_id, detalhe) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getPadrao());
            ps.setInt(2, m.getCategoriaId());
            ps.setString(3, m.getDetalhe());
            ps.executeUpdate();
        }
    }

    public List<MapeamentoDescricao> listarTodos() {
        List<MapeamentoDescricao> lista = new ArrayList<>();
        String sql = """
            SELECT m.id, m.padrao, m.categoria_id, c.nome AS categoria_nome, m.detalhe
            FROM mapeamentos_descricao m
            JOIN categorias c ON c.id = m.categoria_id
            ORDER BY m.padrao
        """;
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new MapeamentoDescricao(
                        rs.getInt("id"), rs.getString("padrao"),
                        rs.getInt("categoria_id"), rs.getString("categoria_nome"),
                        rs.getString("detalhe")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar mapeamentos: " + e.getMessage(), e);
        }
        return lista;
    }
}
