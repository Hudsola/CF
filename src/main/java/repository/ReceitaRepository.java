package repository;

import db.DatabaseManager;
import model.Dinheiro;
import model.Periodo;
import model.Receita;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class ReceitaRepository {

    /** Restringe UPDATE/DELETE a lançamentos cuja conta é do usuário. */
    private static final String DO_USUARIO = " AND conta_id IN (SELECT id FROM contas WHERE usuario_id = ?)";

    private final int usuarioId;

    public ReceitaRepository(int usuarioId) { this.usuarioId = usuarioId; }

    private static final String SELECT_BASE =
        "SELECT r.id, r.origem, r.valor, r.conta_id, c.nome AS conta_nome, r.data " +
        "FROM receitas r JOIN contas c ON c.id = r.conta_id " +
        "WHERE c.usuario_id = ? ";

    public void salvar(Receita r) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO receitas (origem, valor, conta_id, data) VALUES (?,?,?,?)")) {
            ps.setString(1, r.getOrigem()); ps.setDouble(2, r.getValor().doubleValue());
            ps.setInt(3, r.getContaId()); ps.setString(4, r.getData().toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao salvar receita: " + e.getMessage(), e); }
    }

    public void atualizar(Receita r) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE receitas SET origem=?, valor=?, conta_id=?, data=? WHERE id=?" + DO_USUARIO)) {
            ps.setString(1, r.getOrigem()); ps.setDouble(2, r.getValor().doubleValue());
            ps.setInt(3, r.getContaId()); ps.setString(4, r.getData().toString()); ps.setInt(5, r.getId()); ps.setInt(6, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Receita não encontrada com ID " + r.getId());
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar receita: " + e.getMessage(), e); }
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM receitas WHERE id=?" + DO_USUARIO)) {
            ps.setInt(1, id); ps.setInt(2, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Receita não encontrada com ID " + id);
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir receita: " + e.getMessage(), e); }
    }

    public List<Receita> listarTodos() {
        return Consultas.listar(SELECT_BASE + "ORDER BY r.data DESC, r.id DESC", List.of(usuarioId), this::map, "receitas");
    }

    public List<Receita> listarPorPeriodo(Periodo p) {
        return Consultas.listar(SELECT_BASE + "AND r.data BETWEEN ? AND ? ORDER BY r.data, r.id",
                List.of(usuarioId, p.inicio().toString(), p.fim().toString()), this::map, "receitas");
    }

    public Set<Integer> anos() {
        return Consultas.anos("receitas", usuarioId);
    }

    private Receita map(ResultSet rs) throws SQLException {
        return new Receita(rs.getInt("id"), rs.getString("origem"), Dinheiro.de(rs.getDouble("valor")),
            rs.getInt("conta_id"), rs.getString("conta_nome"), LocalDate.parse(rs.getString("data")));
    }
}
