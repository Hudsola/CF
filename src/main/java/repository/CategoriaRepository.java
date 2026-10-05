package repository;

import db.DatabaseManager;
import model.Categoria;

import java.sql.*;
import java.util.List;

/** Categorias do usuário informado no construtor. */
public class CategoriaRepository {

    /** Criadas para cada usuário novo. */
    public static final List<String> PADRAO = List.of(
            "Alimentação", "Moradia", "Educação", "Pet", "Saúde", "Transporte", "Pessoais", "Lazer", "Financeiros");

    private final int usuarioId;

    public CategoriaRepository(int usuarioId) { this.usuarioId = usuarioId; }

    public void salvar(Categoria c) {
        verificarDuplicado(c.getNome(), null);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO categorias (usuario_id, nome) VALUES (?,?)")) {
            ps.setInt(1, usuarioId); ps.setString(2, c.getNome());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE"))
                throw new RuntimeException("Já existe uma categoria com o nome \"" + c.getNome() + "\".");
            throw new RuntimeException("Erro ao salvar categoria: " + e.getMessage(), e);
        }
    }

    /** Cria as categorias padrão que o usuário ainda não tem, usando uma conexão já aberta. */
    public void criarPadroes(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO categorias (usuario_id, nome) VALUES (?,?)")) {
            for (String nome : PADRAO) {
                ps.setInt(1, usuarioId); ps.setString(2, nome);
                ps.executeUpdate();
            }
        }
    }

    public void atualizar(Categoria c) {
        verificarDuplicado(c.getNome(), c.getId());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE categorias SET nome=? WHERE id=? AND usuario_id=?")) {
            ps.setString(1, c.getNome()); ps.setInt(2, c.getId()); ps.setInt(3, usuarioId);
            if (ps.executeUpdate() == 0) throw new RuntimeException("Categoria não encontrada com ID " + c.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar categoria: " + e.getMessage(), e);
        }
    }

    public List<Categoria> listarTodos() {
        return Consultas.listar("SELECT id, nome FROM categorias WHERE usuario_id=? ORDER BY nome", List.of(usuarioId),
                rs -> new Categoria(rs.getInt("id"), rs.getString("nome")), "categorias");
    }

    /** true se a categoria existe e pertence a este usuário. */
    public boolean pertence(int categoriaId) {
        return Consultas.existe("SELECT 1 FROM categorias WHERE id=? AND usuario_id=?", categoriaId, usuarioId);
    }

    public void excluir(int id) {
        if (!pertence(id)) throw new RuntimeException("Categoria não encontrada com ID " + id);
        if (Consultas.existe("SELECT 1 FROM despesas WHERE categoria_id=?", id))
            throw new RuntimeException("Não é possível excluir: existem despesas vinculadas a essa categoria.");
        if (Consultas.existe("SELECT 1 FROM lancamentos_fixos WHERE tipo='DESPESA' AND categoria_id=?", id))
            throw new RuntimeException("Não é possível excluir: existem lançamentos fixos usando essa categoria.");
        try (Connection conn = DatabaseManager.getConnection()) {
            // Mapeamentos de importação apontam para a categoria; sem ela não servem mais.
            conn.setAutoCommit(false);
            try (PreparedStatement delMap = conn.prepareStatement("DELETE FROM mapeamentos_descricao WHERE categoria_id=?");
                 PreparedStatement del = conn.prepareStatement("DELETE FROM categorias WHERE id=? AND usuario_id=?")) {
                delMap.setInt(1, id);
                delMap.executeUpdate();
                del.setInt(1, id); del.setInt(2, usuarioId);
                del.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir categoria: " + e.getMessage(), e);
        }
    }

    private void verificarDuplicado(String nome, Integer idExcluir) {
        for (Categoria c : listarTodos())
            if ((idExcluir == null || c.getId() != idExcluir) && c.getNome().equalsIgnoreCase(nome))
                throw new RuntimeException("Já existe uma categoria chamada \"" + c.getNome() + "\".");
    }
}
