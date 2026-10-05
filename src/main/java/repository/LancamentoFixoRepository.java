package repository;

import db.DatabaseManager;
import model.Dinheiro;
import model.LancamentoFixo;
import model.LancamentoFixo.Tipo;

import java.sql.*;
import java.util.List;

/** Lançamentos fixos do usuário (o dono é o da conta do lançamento). */
public class LancamentoFixoRepository {

    private static final String SELECT_BASE =
        "SELECT lf.id, lf.tipo, lf.descricao, lf.categoria_id, COALESCE(cat.nome,'') AS categoria_nome, " +
        "lf.valor, lf.conta_id, c.nome AS conta_nome, lf.dia_vencimento, lf.ativo " +
        "FROM lancamentos_fixos lf " +
        "LEFT JOIN categorias cat ON cat.id = lf.categoria_id " +
        "JOIN contas c ON c.id = lf.conta_id " +
        "WHERE c.usuario_id = ? ";

    /** Restringe UPDATE/DELETE a lançamentos cuja conta é do usuário. */
    private static final String DO_USUARIO = " AND conta_id IN (SELECT id FROM contas WHERE usuario_id = ?)";

    private final int usuarioId;

    public LancamentoFixoRepository(int usuarioId) { this.usuarioId = usuarioId; }

    public void salvar(LancamentoFixo lf) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO lancamentos_fixos (tipo, descricao, categoria_id, valor, conta_id, dia_vencimento, ativo) VALUES (?,?,?,?,?,?,1)")) {
            ps.setString(1, lf.getTipo().name()); ps.setString(2, lf.getDescricao());
            ps.setInt(3, lf.getCategoriaId()); ps.setDouble(4, lf.getValor().doubleValue());
            ps.setInt(5, lf.getContaId()); ps.setInt(6, lf.getDiaVencimento());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao salvar lançamento fixo: " + e.getMessage(), e); }
    }

    public void atualizar(LancamentoFixo lf) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE lancamentos_fixos SET tipo=?, descricao=?, categoria_id=?, valor=?, conta_id=?, dia_vencimento=? WHERE id=?" + DO_USUARIO)) {
            ps.setString(1, lf.getTipo().name()); ps.setString(2, lf.getDescricao());
            ps.setInt(3, lf.getCategoriaId()); ps.setDouble(4, lf.getValor().doubleValue());
            ps.setInt(5, lf.getContaId()); ps.setInt(6, lf.getDiaVencimento()); ps.setInt(7, lf.getId());
            ps.setInt(8, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Lançamento fixo não encontrado com ID " + lf.getId());
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar lançamento fixo: " + e.getMessage(), e); }
    }

    public List<LancamentoFixo> listarTodos() {
        return query(SELECT_BASE + "ORDER BY lf.tipo, lf.descricao");
    }

    public List<LancamentoFixo> listarAtivos() {
        return query(SELECT_BASE + "AND lf.ativo=1 ORDER BY lf.tipo, lf.descricao");
    }

    public void alternarAtivo(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE lancamentos_fixos SET ativo = CASE WHEN ativo=1 THEN 0 ELSE 1 END WHERE id=?" + DO_USUARIO)) {
            ps.setInt(1, id); ps.setInt(2, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Lançamento fixo não encontrado com ID " + id);
        } catch (SQLException e) { throw new RuntimeException("Erro ao alternar status: " + e.getMessage(), e); }
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p2 = conn.prepareStatement("DELETE FROM lancamentos_fixos WHERE id=?" + DO_USUARIO);
                 PreparedStatement p1 = conn.prepareStatement("DELETE FROM aplicacoes_fixos WHERE lancamento_fixo_id=?")) {
                p2.setInt(1, id); p2.setInt(2, usuarioId);
                if (p2.executeUpdate() == 0) {
                    conn.rollback();
                    throw new RuntimeException("Lançamento fixo não encontrado com ID " + id);
                }
                p1.setInt(1, id); p1.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir lançamento fixo: " + e.getMessage(), e); }
    }

    public boolean jaAplicado(int fixoId, String mes, int ano) {
        return Consultas.existe("SELECT 1 FROM aplicacoes_fixos WHERE lancamento_fixo_id=? AND mes=? AND ano=?",
                fixoId, mes, ano);
    }

    public void registrarAplicacao(int fixoId, String mes, int ano) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO aplicacoes_fixos (lancamento_fixo_id, mes, ano) VALUES (?,?,?)")) {
            ps.setInt(1, fixoId); ps.setString(2, mes); ps.setInt(3, ano);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao registrar aplicação: " + e.getMessage(), e); }
    }

    private List<LancamentoFixo> query(String sql) {
        return Consultas.listar(sql, List.of(usuarioId), rs -> new LancamentoFixo(
                rs.getInt("id"), Tipo.valueOf(rs.getString("tipo")), rs.getString("descricao"),
                rs.getInt("categoria_id"), rs.getString("categoria_nome"),
                Dinheiro.de(rs.getDouble("valor")), rs.getInt("conta_id"), rs.getString("conta_nome"),
                rs.getInt("dia_vencimento"), rs.getInt("ativo") == 1), "lançamentos fixos");
    }
}
