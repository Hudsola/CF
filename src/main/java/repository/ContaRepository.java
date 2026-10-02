package repository;

import db.DatabaseManager;
import model.Conta;
import model.Dinheiro;

import java.sql.*;
import java.util.List;

/** Contas do usuário informado no construtor; nenhuma operação enxerga contas de outros usuários. */
public class ContaRepository {

    private final int usuarioId;

    public ContaRepository(int usuarioId) { this.usuarioId = usuarioId; }

    public void salvar(Conta c) {
        verificarDuplicado(c.getNome(), null);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO contas (usuario_id, nome, saldo_inicial) VALUES (?,?,?)")) {
            ps.setInt(1, usuarioId); ps.setString(2, c.getNome()); ps.setDouble(3, c.getSaldoInicial().doubleValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE"))
                throw new RuntimeException("Já existe uma conta com o nome \"" + c.getNome() + "\".");
            throw new RuntimeException("Erro ao salvar conta: " + e.getMessage(), e);
        }
    }

    public void atualizar(Conta c) {
        verificarDuplicado(c.getNome(), c.getId());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE contas SET nome=?, saldo_inicial=? WHERE id=? AND usuario_id=?")) {
            ps.setString(1, c.getNome()); ps.setDouble(2, c.getSaldoInicial().doubleValue());
            ps.setInt(3, c.getId()); ps.setInt(4, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Conta não encontrada com ID " + c.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar conta: " + e.getMessage(), e);
        }
    }

    public List<Conta> listarTodos() {
        return Consultas.listar("SELECT * FROM contas WHERE usuario_id=? ORDER BY nome", List.of(usuarioId),
                rs -> new Conta(rs.getInt("id"), rs.getString("nome"), Dinheiro.de(rs.getDouble("saldo_inicial"))),
                "contas");
    }

    /** true se a conta existe e pertence a este usuário. */
    public boolean pertence(int contaId) {
        return Consultas.existe("SELECT 1 FROM contas WHERE id=? AND usuario_id=?", contaId, usuarioId);
    }

    public void excluir(int id) {
        if (!pertence(id)) throw new RuntimeException("Conta não encontrada com ID " + id);
        String[] tabelas = {"receitas", "despesas", "investimentos", "lancamentos_fixos"};
        try (Connection conn = DatabaseManager.getConnection()) {
            for (String tabela : tabelas) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + tabela + " WHERE conta_id=?")) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0)
                        throw new RuntimeException("Não é possível excluir: registros em \"" + tabela + "\" vinculados a essa conta.");
                }
            }
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM contas WHERE id=? AND usuario_id=?")) {
                del.setInt(1, id); del.setInt(2, usuarioId);
                del.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir conta: " + e.getMessage(), e);
        }
    }

    /** Compara em Java (equalsIgnoreCase trata acentos: "Itaú" = "ITAÚ"; o NOCASE do SQLite só trata ASCII). */
    private void verificarDuplicado(String nome, Integer idExcluir) {
        for (Conta c : listarTodos())
            if ((idExcluir == null || c.getId() != idExcluir) && c.getNome().equalsIgnoreCase(nome))
                throw new RuntimeException("Já existe uma conta chamada \"" + c.getNome() + "\".");
    }
}
