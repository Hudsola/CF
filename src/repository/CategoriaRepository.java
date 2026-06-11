package repository;

import db.DatabaseManager;
import model.Categoria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaRepository {

    public void salvar(Categoria c) {
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
}
