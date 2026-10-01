package repository;

import db.DatabaseManager;
import model.Despesa;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DespesaRepository {

    private static final String SELECT_BASE =
        "SELECT d.id, d.categoria_id, cat.nome AS categoria_nome, d.detalhamento, d.valor, " +
        "d.conta_id, c.nome AS conta_nome, d.data, d.mes, d.ano " +
        "FROM despesas d " +
        "LEFT JOIN categorias cat ON cat.id = d.categoria_id " +
        "LEFT JOIN contas c ON c.id = d.conta_id ";

    public void salvar(Despesa d) {
        try (Connection conn = DatabaseManager.getConnection()) {
            salvar(conn, d);
        } catch (SQLException e) { throw new RuntimeException("Erro ao salvar despesa: " + e.getMessage(), e); }
    }

    /** Salva usando uma conexão já aberta (permite agrupar vários inserts numa transação). */
    public void salvar(Connection conn, Despesa d) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO despesas (categoria_id, detalhamento, valor, conta_id, data, mes, ano) VALUES (?,?,?,?,?,?,?)")) {
            ps.setInt(1, d.getCategoriaId()); ps.setString(2, d.getDetalhamento());
            ps.setDouble(3, d.getValor()); ps.setInt(4, d.getContaId());
            ps.setString(5, d.getData().toString()); ps.setString(6, d.getMes()); ps.setInt(7, d.getAno());
            ps.executeUpdate();
        }
    }

    public List<Despesa> listarPorContaEPeriodo(int contaId, LocalDate inicio, LocalDate fim) {
        List<Despesa> lista = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE d.conta_id=? AND d.data BETWEEN ? AND ? ORDER BY d.data";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contaId); ps.setString(2, inicio.toString()); ps.setString(3, fim.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException("Erro ao listar despesas: " + e.getMessage(), e); }
        return lista;
    }

    public void atualizar(Despesa d) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE despesas SET categoria_id=?, detalhamento=?, valor=?, conta_id=?, data=?, mes=?, ano=? WHERE id=?")) {
            ps.setInt(1, d.getCategoriaId()); ps.setString(2, d.getDetalhamento());
            ps.setDouble(3, d.getValor()); ps.setInt(4, d.getContaId());
            ps.setString(5, d.getData().toString()); ps.setString(6, d.getMes());
            ps.setInt(7, d.getAno()); ps.setInt(8, d.getId());
            if (ps.executeUpdate() == 0) throw new RuntimeException("Despesa não encontrada com ID " + d.getId());
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar despesa: " + e.getMessage(), e); }
    }

    public List<Despesa> listarTodos() { return query(SELECT_BASE + "ORDER BY d.data", null); }

    public List<Despesa> listarPorAno(int ano) {
        return query(SELECT_BASE + "WHERE d.ano=? ORDER BY d.data", ano);
    }

    public List<Despesa> listarPorMesAno(String mes, int ano) {
        List<Despesa> lista = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE d.mes=? AND d.ano=? ORDER BY d.data";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mes); ps.setInt(2, ano);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException("Erro ao listar despesas: " + e.getMessage(), e); }
        return lista;
    }

    public void excluir(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM despesas WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Despesa não encontrada com ID " + id);
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir despesa: " + e.getMessage(), e); }
    }

    private List<Despesa> query(String sql, Integer ano) {
        List<Despesa> lista = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs;
            if (ano != null) {
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, ano); rs = ps.executeQuery();
            } else { rs = conn.createStatement().executeQuery(sql); }
            while (rs.next()) lista.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException("Erro ao listar despesas: " + e.getMessage(), e); }
        return lista;
    }

    public List<Despesa> listarFiltrado(Integer categoriaId, String mes, int ano) {

        StringBuilder sql = new StringBuilder(SELECT_BASE)
                .append("WHERE d.ano=? ");

        List<Object> parametros = new ArrayList<>();
        parametros.add(ano);

        if (!"Todos".equalsIgnoreCase(mes)) {
            sql.append("AND d.mes=? ");
            parametros.add(mes);
        }

        if (categoriaId != null) {
            sql.append("AND d.categoria_id=? ");
            parametros.add(categoriaId);
        }

        sql.append("ORDER BY d.data");

        List<Despesa> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar despesas: " + e.getMessage(), e);
        }

        return lista;
    }

    private Despesa map(ResultSet rs) throws SQLException {
        return new Despesa(rs.getInt("id"), rs.getInt("categoria_id"), rs.getString("categoria_nome"),
            rs.getString("detalhamento"), rs.getDouble("valor"),
            rs.getInt("conta_id"), rs.getString("conta_nome"),
            LocalDate.parse(rs.getString("data")), rs.getString("mes"), rs.getInt("ano"));
    }

    public List<Despesa> pesquisar(Integer categoriaId, String mes, Integer ano) {

        StringBuilder sql = new StringBuilder(SELECT_BASE + "WHERE 1=1 ");
        List<Object> parametros = new ArrayList<>();

        if (categoriaId != null) {
            sql.append("AND d.categoria_id = ? ");
            parametros.add(categoriaId);
        }

        if (mes != null && !mes.equalsIgnoreCase("Todos")) {
            sql.append("AND d.mes = ? ");
            parametros.add(mes);
        }

        if (ano != null) {
            sql.append("AND d.ano = ? ");
            parametros.add(ano);
        }

        sql.append("ORDER BY d.data DESC");

        List<Despesa> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao pesquisar despesas.", e);
        }

        return lista;
    }
}