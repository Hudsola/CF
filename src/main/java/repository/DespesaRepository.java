package repository;

import db.DatabaseManager;
import model.Despesa;
import model.Dinheiro;
import model.Periodo;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DespesaRepository {

    /** Restringe UPDATE/DELETE a lançamentos cuja conta é do usuário. */
    private static final String DO_USUARIO = " AND conta_id IN (SELECT id FROM contas WHERE usuario_id = ?)";

    private final int usuarioId;

    public DespesaRepository(int usuarioId) { this.usuarioId = usuarioId; }

    private static final String SELECT_BASE =
        "SELECT d.id, d.categoria_id, cat.nome AS categoria_nome, d.detalhamento, d.valor, " +
        "d.conta_id, c.nome AS conta_nome, d.data " +
        "FROM despesas d " +
        "JOIN categorias cat ON cat.id = d.categoria_id " +
        "JOIN contas c ON c.id = d.conta_id " +
        "WHERE c.usuario_id = ? ";

    public void salvar(Despesa d) {
        try (Connection conn = DatabaseManager.getConnection()) {
            salvar(conn, d);
        } catch (SQLException e) { throw new RuntimeException("Erro ao salvar despesa: " + e.getMessage(), e); }
    }

    /** Salva usando uma conexão já aberta (permite agrupar vários inserts numa transação). */
    public void salvar(Connection conn, Despesa d) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO despesas (categoria_id, detalhamento, valor, conta_id, data) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, d.getCategoriaId()); ps.setString(2, d.getDetalhamento());
            ps.setDouble(3, d.getValor().doubleValue()); ps.setInt(4, d.getContaId());
            ps.setString(5, d.getData().toString());
            ps.executeUpdate();
        }
    }

    public void atualizar(Despesa d) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE despesas SET categoria_id=?, detalhamento=?, valor=?, conta_id=?, data=? WHERE id=?" + DO_USUARIO)) {
            ps.setInt(1, d.getCategoriaId()); ps.setString(2, d.getDetalhamento());
            ps.setDouble(3, d.getValor().doubleValue()); ps.setInt(4, d.getContaId());
            ps.setString(5, d.getData().toString()); ps.setInt(6, d.getId()); ps.setInt(7, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Despesa não encontrada com ID " + d.getId());
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar despesa: " + e.getMessage(), e); }
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM despesas WHERE id=?" + DO_USUARIO)) {
            ps.setInt(1, id); ps.setInt(2, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Despesa não encontrada com ID " + id);
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir despesa: " + e.getMessage(), e); }
    }

    public List<Despesa> listarTodos() {
        return Consultas.listar(SELECT_BASE + "ORDER BY d.data DESC, d.id DESC", List.of(usuarioId), this::map, "despesas");
    }

    public List<Despesa> listarPorPeriodo(Periodo p) {
        return pesquisar(null, p);
    }

    /** Despesas do período, opcionalmente de uma categoria (categoriaId nulo = todas). */
    public List<Despesa> pesquisar(Integer categoriaId, Periodo p) {
        String sql = SELECT_BASE + "AND d.data BETWEEN ? AND ? "
                + (categoriaId != null ? "AND d.categoria_id = ? " : "")
                + "ORDER BY d.data, d.id";
        List<Object> params = new ArrayList<>(List.of(usuarioId, p.inicio().toString(), p.fim().toString()));
        if (categoriaId != null) params.add(categoriaId);
        return Consultas.listar(sql, params, this::map, "despesas");
    }

    public List<Despesa> listarPorContaEPeriodo(int contaId, Periodo p) {
        return Consultas.listar(SELECT_BASE + "AND d.conta_id=? AND d.data BETWEEN ? AND ? ORDER BY d.data",
                List.of(usuarioId, contaId, p.inicio().toString(), p.fim().toString()), this::map, "despesas");
    }

    public Set<Integer> anos() {
        return Consultas.anos("despesas", usuarioId);
    }

    private Despesa map(ResultSet rs) throws SQLException {
        return new Despesa(rs.getInt("id"), rs.getInt("categoria_id"), rs.getString("categoria_nome"),
            rs.getString("detalhamento"), Dinheiro.de(rs.getDouble("valor")),
            rs.getInt("conta_id"), rs.getString("conta_nome"), LocalDate.parse(rs.getString("data")));
    }
}
