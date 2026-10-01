package repository;

import db.DatabaseManager;
import model.Dinheiro;
import model.Investimento;
import model.Periodo;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class InvestimentoRepository {

    private static final String SELECT_BASE =
        "SELECT i.id, i.tipo, i.valor, i.conta_id, c.nome AS conta_nome, i.data " +
        "FROM investimentos i JOIN contas c ON c.id = i.conta_id ";

    public void salvar(Investimento i) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO investimentos (tipo, valor, conta_id, data) VALUES (?,?,?,?)")) {
            ps.setString(1, i.getTipo()); ps.setDouble(2, i.getValor().doubleValue());
            ps.setInt(3, i.getContaId()); ps.setString(4, i.getData().toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao salvar investimento: " + e.getMessage(), e); }
    }

    public void atualizar(Investimento i) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE investimentos SET tipo=?, valor=?, conta_id=?, data=? WHERE id=?")) {
            ps.setString(1, i.getTipo()); ps.setDouble(2, i.getValor().doubleValue());
            ps.setInt(3, i.getContaId()); ps.setString(4, i.getData().toString()); ps.setInt(5, i.getId());
            if (ps.executeUpdate() == 0) throw new RuntimeException("Investimento não encontrado com ID " + i.getId());
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar investimento: " + e.getMessage(), e); }
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM investimentos WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Investimento não encontrado com ID " + id);
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir investimento: " + e.getMessage(), e); }
    }

    public List<Investimento> listarTodos() {
        return Consultas.listar(SELECT_BASE + "ORDER BY i.data DESC, i.id DESC", List.of(), this::map, "investimentos");
    }

    public List<Investimento> listarPorPeriodo(Periodo p) {
        return Consultas.listar(SELECT_BASE + "WHERE i.data BETWEEN ? AND ? ORDER BY i.data, i.id",
                List.of(p.inicio().toString(), p.fim().toString()), this::map, "investimentos");
    }

    public Set<Integer> anos() {
        return Consultas.anos("investimentos");
    }

    private Investimento map(ResultSet rs) throws SQLException {
        return new Investimento(rs.getInt("id"), rs.getString("tipo"), Dinheiro.de(rs.getDouble("valor")),
            rs.getInt("conta_id"), rs.getString("conta_nome"), LocalDate.parse(rs.getString("data")));
    }
}
