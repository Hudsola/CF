package repository;

import db.DatabaseManager;
import model.Usuario;

import java.sql.*;
import java.time.LocalDate;

public class UsuarioRepository {

    public Usuario buscarPrincipal() {
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM usuarios WHERE id=1")) {
            if (rs.next()) {
                String dataNasc = rs.getString("data_nascimento");
                return new Usuario(rs.getInt("id"), rs.getString("nome"),
                    dataNasc != null ? LocalDate.parse(dataNasc) : null);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário: " + e.getMessage(), e);
        }
        return new Usuario(1, "Usuário", null);
    }

    public void atualizar(Usuario u) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE usuarios SET nome=?, data_nascimento=? WHERE id=?")) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getDataNascimento() != null ? u.getDataNascimento().toString() : null);
            ps.setInt(3, u.getId());
            if (ps.executeUpdate() == 0) throw new RuntimeException("Usuário não encontrado com ID " + u.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage(), e);
        }
    }
}
