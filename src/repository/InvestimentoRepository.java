package repository;

import db.DatabaseManager;
import model.Investimento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvestimentoRepository {

    private static final String SELECT_BASE =
            "SELECT i.id, i.tipo, i.valor, i.conta_id, c.nome AS conta_nome, i.data, i.mes, i.ano " +
            "FROM investimentos i JOIN contas c ON c.id = i.conta_id ";

    public void salvar(Investimento i) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO investimentos (tipo, valor, conta_id, data, mes, ano) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, i.getTipo());
            ps.setDouble(2, i.getValor());
            ps.setInt(3, i.getContaId());
            ps.setString(4, i.getData().toString());
            ps.setString(5, i.getMes());
            ps.setInt(6, i.getAno());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar investimento: " + e.getMessage(), e);
        }
    }

    public void atualizar(Investimento i) {
        String sql = "UPDATE investimentos SET tipo=?, valor=?, conta_id=?, data=?, mes=?, ano=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, i.getTipo());
            ps.setDouble(2, i.getValor());
            ps.setInt(3, i.getContaId());
            ps.setString(4, i.getData().toString());
            ps.setString(5, i.getMes());
            ps.setInt(6, i.getAno());
            ps.setInt(7, i.getId());
            if (ps.executeUpdate() == 0) throw new RuntimeException("Investimento não encontrado com ID " + i.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar investimento: " + e.getMessage(), e);
        }
    }

    public List<Investimento> listarTodos() {
        return query(SELECT_BASE + "ORDER BY i.data", null);
    }

    public List<Investimento> listarPorAno(int ano) {
        return query(SELECT_BASE + "WHERE i.ano = ? ORDER BY i.data", ano);
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM investimentos WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Investimento não encontrado com ID " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir investimento: " + e.getMessage(), e);
        }
    }

    private List<Investimento> query(String sql, Integer ano) {
        List<Investimento> lista = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs;
            if (ano != null) {
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, ano);
                rs = ps.executeQuery();
            } else {
                rs = conn.createStatement().executeQuery(sql);
            }
            while (rs.next()) lista.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar investimentos: " + e.getMessage(), e);
        }
        return lista;
    }

    private Investimento map(ResultSet rs) throws SQLException {
        return new Investimento(rs.getInt("id"), rs.getString("tipo"), rs.getDouble("valor"),
                rs.getInt("conta_id"), rs.getString("conta_nome"),
                LocalDate.parse(rs.getString("data")), rs.getString("mes"), rs.getInt("ano"));
    }
}
