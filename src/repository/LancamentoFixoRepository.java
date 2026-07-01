package repository;

import db.DatabaseManager;
import model.LancamentoFixo;
import model.LancamentoFixo.Tipo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LancamentoFixoRepository {

    private static final String SELECT_BASE =
            "SELECT lf.id, lf.tipo, lf.descricao, lf.categoria_id, COALESCE(cat.nome,'') AS categoria_nome, " +
            "lf.valor, lf.conta_id, c.nome AS conta_nome, lf.dia_vencimento, lf.ativo " +
            "FROM lancamentos_fixos lf " +
            "LEFT JOIN categorias cat ON cat.id = lf.categoria_id " +
            "JOIN contas c ON c.id = lf.conta_id ";

    public void salvar(LancamentoFixo lf) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO lancamentos_fixos (tipo, descricao, categoria_id, valor, conta_id, dia_vencimento, ativo) VALUES (?,?,?,?,?,?,1)")) {
            ps.setString(1, lf.getTipo().name());
            ps.setString(2, lf.getDescricao());
            ps.setInt(3, lf.getCategoriaId());
            ps.setDouble(4, lf.getValor());
            ps.setInt(5, lf.getContaId());
            ps.setInt(6, lf.getDiaVencimento());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar lançamento fixo: " + e.getMessage(), e);
        }
    }

    public void atualizar(LancamentoFixo lf) {
        String sql = "UPDATE lancamentos_fixos SET tipo=?, descricao=?, categoria_id=?, valor=?, conta_id=?, dia_vencimento=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lf.getTipo().name());
            ps.setString(2, lf.getDescricao());
            ps.setInt(3, lf.getCategoriaId());
            ps.setDouble(4, lf.getValor());
            ps.setInt(5, lf.getContaId());
            ps.setInt(6, lf.getDiaVencimento());
            ps.setInt(7, lf.getId());
            if (ps.executeUpdate() == 0) throw new RuntimeException("Lançamento fixo não encontrado com ID " + lf.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar lançamento fixo: " + e.getMessage(), e);
        }
    }

    public List<LancamentoFixo> listarTodos() {
        return query(SELECT_BASE + "ORDER BY lf.tipo, lf.descricao");
    }

    public List<LancamentoFixo> listarAtivos() {
        return query(SELECT_BASE + "WHERE lf.ativo = 1 ORDER BY lf.tipo, lf.descricao");
    }

    public void alternarAtivo(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE lancamentos_fixos SET ativo = CASE WHEN ativo=1 THEN 0 ELSE 1 END WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Lançamento fixo não encontrado com ID " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao alternar status: " + e.getMessage(), e);
        }
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement p1 = conn.prepareStatement("DELETE FROM aplicacoes_fixos WHERE lancamento_fixo_id = ?");
            p1.setInt(1, id); p1.executeUpdate();
            PreparedStatement p2 = conn.prepareStatement("DELETE FROM lancamentos_fixos WHERE id = ?");
            p2.setInt(1, id);
            if (p2.executeUpdate() == 0) throw new RuntimeException("Lançamento fixo não encontrado com ID " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir lançamento fixo: " + e.getMessage(), e);
        }
    }

    public boolean jaAplicado(int fixoId, String mes, int ano) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM aplicacoes_fixos WHERE lancamento_fixo_id=? AND mes=? AND ano=?")) {
            ps.setInt(1, fixoId); ps.setString(2, mes); ps.setInt(3, ano);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar aplicação: " + e.getMessage(), e);
        }
    }

    public void registrarAplicacao(int fixoId, String mes, int ano) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO aplicacoes_fixos (lancamento_fixo_id, mes, ano) VALUES (?,?,?)")) {
            ps.setInt(1, fixoId); ps.setString(2, mes); ps.setInt(3, ano);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao registrar aplicação: " + e.getMessage(), e);
        }
    }

    private List<LancamentoFixo> query(String sql) {
        List<LancamentoFixo> lista = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new LancamentoFixo(
                        rs.getInt("id"), Tipo.valueOf(rs.getString("tipo")), rs.getString("descricao"),
                        rs.getInt("categoria_id"), rs.getString("categoria_nome"),
                        rs.getDouble("valor"), rs.getInt("conta_id"), rs.getString("conta_nome"),
                        rs.getInt("dia_vencimento"), rs.getInt("ativo") == 1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar lançamentos fixos: " + e.getMessage(), e);
        }
        return lista;
    }
}
