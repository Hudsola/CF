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

    /** Restringe UPDATE/DELETE a lançamentos cuja conta é do usuário. */
    private static final String DO_USUARIO = " AND conta_id IN (SELECT id FROM contas WHERE usuario_id = ?)";

    private final int usuarioId;

    public InvestimentoRepository(int usuarioId) { this.usuarioId = usuarioId; }

    private static final String SELECT_BASE =
        "SELECT i.id, i.tipo, i.valor, i.conta_id, c.nome AS conta_nome, i.data, " +
        "i.data_resgate, i.valor_resgatado, i.receita_resultado_id, i.despesa_resultado_id " +
        "FROM investimentos i JOIN contas c ON c.id = i.conta_id " +
        "WHERE c.usuario_id = ? ";

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
                 "UPDATE investimentos SET tipo=?, valor=?, conta_id=?, data=? WHERE id=?" + DO_USUARIO)) {
            ps.setString(1, i.getTipo()); ps.setDouble(2, i.getValor().doubleValue());
            ps.setInt(3, i.getContaId()); ps.setString(4, i.getData().toString()); ps.setInt(5, i.getId()); ps.setInt(6, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Investimento não encontrado com ID " + i.getId());
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar investimento: " + e.getMessage(), e); }
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM investimentos WHERE id=?" + DO_USUARIO)) {
            ps.setInt(1, id); ps.setInt(2, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Investimento não encontrado com ID " + id);
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir investimento: " + e.getMessage(), e); }
    }

    public List<Investimento> listarTodos() {
        return Consultas.listar(SELECT_BASE + "ORDER BY i.data DESC, i.id DESC", List.of(usuarioId), this::map, "investimentos");
    }

    /** Investimentos resgatados dentro do período (pela data do resgate). */
    public List<Investimento> listarResgatadosNoPeriodo(Periodo p) {
        return Consultas.listar(SELECT_BASE + "AND i.data_resgate BETWEEN ? AND ? ORDER BY i.data_resgate, i.id",
                List.of(usuarioId, p.inicio().toString(), p.fim().toString()), this::map, "investimentos");
    }

    /** Investimentos aplicados dentro do período (pela data da aplicação). */
    public List<Investimento> listarPorPeriodo(Periodo p) {
        return Consultas.listar(SELECT_BASE + "AND i.data BETWEEN ? AND ? ORDER BY i.data, i.id",
                List.of(usuarioId, p.inicio().toString(), p.fim().toString()), this::map, "investimentos");
    }

    public Set<Integer> anos() {
        return Consultas.anos("investimentos", usuarioId);
    }

    /** Grava o resgate (total) e o lançamento de resultado criado para ele (receita de lucro ou despesa de prejuízo). */
    public void registrarResgate(int id, LocalDate data, java.math.BigDecimal valorResgatado,
                                 Integer receitaResultadoId, Integer despesaResultadoId) {
        executar("UPDATE investimentos SET data_resgate=?, valor_resgatado=?, receita_resultado_id=?, despesa_resultado_id=? "
                + "WHERE id=?" + DO_USUARIO, data.toString(), valorResgatado.doubleValue(), receitaResultadoId,
                despesaResultadoId, id, usuarioId);
    }

    /** Volta o investimento para "guardado". */
    public void limparResgate(int id) {
        executar("UPDATE investimentos SET data_resgate=NULL, valor_resgatado=NULL, receita_resultado_id=NULL, "
                + "despesa_resultado_id=NULL WHERE id=?" + DO_USUARIO, id, usuarioId);
    }

    private static void executar(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Investimento não encontrado.");
        } catch (SQLException e) { throw new RuntimeException("Erro ao gravar o resgate: " + e.getMessage(), e); }
    }

    private Investimento map(ResultSet rs) throws SQLException {
        Investimento i = new Investimento(rs.getInt("id"), rs.getString("tipo"), Dinheiro.de(rs.getDouble("valor")),
            rs.getInt("conta_id"), rs.getString("conta_nome"), LocalDate.parse(rs.getString("data")));
        String resgate = rs.getString("data_resgate");
        if (resgate == null) return i;
        int rec = rs.getInt("receita_resultado_id");
        Integer receitaId = rs.wasNull() ? null : rec;
        int desp = rs.getInt("despesa_resultado_id");
        Integer despesaId = rs.wasNull() ? null : desp;
        return i.comResgate(LocalDate.parse(resgate), Dinheiro.de(rs.getDouble("valor_resgatado")), receitaId, despesaId);
    }
}
