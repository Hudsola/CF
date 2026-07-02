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
                return new Usuario(
                    rs.getInt("id"), rs.getString("nome"),
                    dataNasc != null ? LocalDate.parse(dataNasc) : null,
                    rs.getInt("nivel"), rs.getInt("xp"), rs.getInt("xp_proximo_nivel"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário: " + e.getMessage(), e);
        }
        return new Usuario("Usuário", null);
    }

    public void atualizar(Usuario u) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE usuarios SET nome=?, data_nascimento=?, nivel=?, xp=?, xp_proximo_nivel=? WHERE id=?")) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getDataNascimento() != null ? u.getDataNascimento().toString() : null);
            ps.setInt(3, u.getNivel()); ps.setInt(4, u.getXp());
            ps.setInt(5, u.getXpProximoNivel()); ps.setInt(6, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage(), e);
        }
    }
}
