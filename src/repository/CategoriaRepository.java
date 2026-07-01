package repository;

import db.DatabaseManager;
import model.Categoria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaRepository {

    public void salvar(Categoria c) {
        verificarDuplicado(c.getNome(), null);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO categorias (nome) VALUES (?)")) {
            ps.setString(1, c.getNome());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE"))
                throw new RuntimeException("Já existe uma categoria com o nome \"" + c.getNome() + "\".");
            throw new RuntimeException("Erro ao salvar categoria: " + e.getMessage(), e);
        }
    }

    public void atualizar(Categoria c) {
        verificarDuplicado(c.getNome(), c.getId());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE categorias SET nome = ? WHERE id = ?")) {
            ps.setString(1, c.getNome());
            ps.setInt(2, c.getId());
            if (ps.executeUpdate() == 0) throw new RuntimeException("Categoria não encontrada com ID " + c.getId());
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE"))
                throw new RuntimeException("Já existe uma categoria com o nome \"" + c.getNome() + "\".");
            throw new RuntimeException("Erro ao atualizar categoria: " + e.getMessage(), e);
        }
    }

    public List<Categoria> listarTodos() {
        List<Categoria> lista = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM categorias ORDER BY nome")) {
            while (rs.next()) lista.add(new Categoria(rs.getInt("id"), rs.getString("nome")));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar categorias: " + e.getMessage(), e);
        }
        return lista;
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM despesas WHERE categoria_id = ?");
            check.setInt(1, id);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0)
                throw new RuntimeException("Não é possível excluir: existem despesas vinculadas a essa categoria.");

            PreparedStatement del = conn.prepareStatement("DELETE FROM categorias WHERE id = ?");
            del.setInt(1, id);
            if (del.executeUpdate() == 0) throw new RuntimeException("Categoria não encontrada com ID " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir categoria: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se já existe uma categoria com o mesmo nome (case-insensitive),
     * ignorando o próprio registro quando for uma atualização (idExcluir != null).
     * Esta é uma camada de proteção adicional à constraint UNIQUE COLLATE NOCASE
     * do banco — permite dar uma mensagem de erro amigável antes do INSERT/UPDATE.
     */
    private void verificarDuplicado(String nome, Integer idExcluir) {
        String sql = "SELECT id, nome FROM categorias WHERE nome = ? COLLATE NOCASE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int idEncontrado = rs.getInt("id");
                if (idExcluir == null || idEncontrado != idExcluir) {
                    throw new RuntimeException("Já existe uma categoria chamada \"" + rs.getString("nome")
                            + "\" (você digitou \"" + nome + "\").");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar duplicidade: " + e.getMessage(), e);
        }
    }
}
