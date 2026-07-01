package repository;

import db.DatabaseManager;
import model.Receita;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReceitaRepository {

    private static final String SELECT_BASE =
            "SELECT r.id, r.origem, r.valor, r.conta_id, c.nome AS conta_nome, r.data, r.mes, r.ano " +
            "FROM receitas r JOIN contas c ON c.id = r.conta_id ";

    public void salvar(Receita r) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO receitas (origem, valor, conta_id, data, mes, ano) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, r.getOrigem());
            ps.setDouble(2, r.getValor());
            ps.setInt(3, r.getContaId());
            ps.setString(4, r.getData().toString());
            ps.setString(5, r.getMes());
            ps.setInt(6, r.getAno());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar receita: " + e.getMessage(), e);
        }
    }

    public void atualizar(Receita r) {
        String sql = "UPDATE receitas SET origem=?, valor=?, conta_id=?, data=?, mes=?, ano=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getOrigem());
            ps.setDouble(2, r.getValor());
            ps.setInt(3, r.getContaId());
            ps.setString(4, r.getData().toString());
            ps.setString(5, r.getMes());
            ps.setInt(6, r.getAno());
            ps.setInt(7, r.getId());
            if (ps.executeUpdate() == 0) throw new RuntimeException("Receita não encontrada com ID " + r.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar receita: " + e.getMessage(), e);
        }
    }

    public List<Receita> listarTodos() {
        return query(SELECT_BASE + "ORDER BY r.data", null);
    }

    public List<Receita> listarPorAno(int ano) {
        return query(SELECT_BASE + "WHERE r.ano = ? ORDER BY r.data", ano);
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM receitas WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Receita não encontrada com ID " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir receita: " + e.getMessage(), e);
        }
    }

    private List<Receita> query(String sql, Integer ano) {
        List<Receita> lista = new ArrayList<>();
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
            throw new RuntimeException("Erro ao listar receitas: " + e.getMessage(), e);
        }
        return lista;
    }

    private Receita map(ResultSet rs) throws SQLException {
        return new Receita(rs.getInt("id"), rs.getString("origem"), rs.getDouble("valor"),
                rs.getInt("conta_id"), rs.getString("conta_nome"),
                LocalDate.parse(rs.getString("data")), rs.getString("mes"), rs.getInt("ano"));
    }
}
