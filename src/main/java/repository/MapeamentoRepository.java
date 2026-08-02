package repository;

import db.DatabaseManager;
import model.MapeamentoDescricao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MapeamentoRepository {

    public void salvar(MapeamentoDescricao m) {
        String sql = "INSERT OR REPLACE INTO mapeamentos_descricao (padrao, categoria_id, detalhe) VALUES (?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getPadrao());
            ps.setInt(2, m.getCategoriaId());
            ps.setString(3, m.getDetalhe());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar mapeamento: " + e.getMessage(), e);
        }
    }

    /**
     * Busca o mapeamento cujo padrão está contido no título informado.
     * Ex: padrão "Vindi *Leds" encontra "Vindi *Leds - Parcela 2/10"
     * Ordena por LENGTH(padrao) DESC para preferir o match mais específico.
     */
    public MapeamentoDescricao buscarPorTitulo(String titulo) {
        String sql = """
            SELECT m.id, m.padrao, m.categoria_id, c.nome AS categoria_nome, m.detalhe
            FROM mapeamentos_descricao m
            JOIN categorias c ON c.id = m.categoria_id
            WHERE ? LIKE '%' || m.padrao || '%' COLLATE NOCASE
            ORDER BY LENGTH(m.padrao) DESC
            LIMIT 1
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titulo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new MapeamentoDescricao(
                        rs.getInt("id"), rs.getString("padrao"),
                        rs.getInt("categoria_id"), rs.getString("categoria_nome"),
                        rs.getString("detalhe"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar mapeamento: " + e.getMessage(), e);
        }
        return null;
    }

    public List<MapeamentoDescricao> listarTodos() {
        List<MapeamentoDescricao> lista = new ArrayList<>();
        String sql = """
            SELECT m.id, m.padrao, m.categoria_id, c.nome AS categoria_nome, m.detalhe
            FROM mapeamentos_descricao m
            JOIN categorias c ON c.id = m.categoria_id
            ORDER BY m.padrao
        """;
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new MapeamentoDescricao(
                        rs.getInt("id"), rs.getString("padrao"),
                        rs.getInt("categoria_id"), rs.getString("categoria_nome"),
                        rs.getString("detalhe")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar mapeamentos: " + e.getMessage(), e);
        }
        return lista;
    }
}