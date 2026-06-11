package repository;

import db.DatabaseManager;
import model.Conta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContaRepository {

    public void salvar(Conta c) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO contas (nome) VALUES (?)")) {
            ps.setString(1, c.getNome());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE"))
                throw new RuntimeException("Já existe uma conta com o nome \"" + c.getNome() + "\".");
            throw new RuntimeException("Erro ao salvar conta: " + e.getMessage(), e);
        }
    }

    public List<Conta> listarTodos() {
        List<Conta> lista = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM contas ORDER BY nome")) {
            while (rs.next()) lista.add(new Conta(rs.getInt("id"), rs.getString("nome")));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar contas: " + e.getMessage(), e);
        }
        return lista;
    }

    public void excluir(int id) {
        String[] tabelas = {"receitas", "despesas", "investimentos", "lancamentos_fixos"};
        try (Connection conn = DatabaseManager.getConnection()) {
            for (String tabela : tabelas) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM " + tabela + " WHERE conta_id = ?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0)
                    throw new RuntimeException(
                            "Não é possível excluir: existem registros em \"" + tabela + "\" vinculados a essa conta.");
            }
            PreparedStatement del = conn.prepareStatement("DELETE FROM contas WHERE id = ?");
            del.setInt(1, id);
            if (del.executeUpdate() == 0) throw new RuntimeException("Conta não encontrada com ID " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir conta: " + e.getMessage(), e);
        }
    }
}
