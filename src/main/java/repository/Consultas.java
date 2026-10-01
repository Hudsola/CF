package repository;

import db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Código de consulta repetido entre os repositórios. */
final class Consultas {

    @FunctionalInterface
    interface Mapeador<T> { T map(ResultSet rs) throws SQLException; }

    private Consultas() {}

    static <T> List<T> listar(String sql, List<Object> parametros, Mapeador<T> mapeador, String oQue) {
        List<T> lista = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) ps.setObject(i + 1, parametros.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapeador.map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar " + oQue + ": " + e.getMessage(), e);
        }
        return lista;
    }

    /** Anos que têm lançamentos na tabela (a data é texto ISO, então o ano são os 4 primeiros caracteres). */
    static Set<Integer> anos(String tabela) {
        Set<Integer> anos = new TreeSet<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                     "SELECT DISTINCT CAST(substr(data, 1, 4) AS INTEGER) FROM " + tabela)) {
            while (rs.next()) anos.add(rs.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar anos de " + tabela + ": " + e.getMessage(), e);
        }
        return anos;
    }
}
