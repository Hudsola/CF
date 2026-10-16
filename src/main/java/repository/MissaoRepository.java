package repository;

import db.DatabaseManager;
import model.Missao;
import model.TipoMissao;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Missões do usuário informado no construtor; nenhuma operação enxerga missões de outros usuários. */
public class MissaoRepository {

    private final int usuarioId;

    public MissaoRepository(int usuarioId) { this.usuarioId = usuarioId; }

    public List<Missao> listarTodas() {
        Map<Integer, SortedSet<LocalDate>> datas = new HashMap<>();
        Consultas.listar("SELECT d.missao_id, d.data FROM missao_datas d JOIN missoes m ON m.id = d.missao_id "
                        + "WHERE m.usuario_id = ?", List.of(usuarioId), rs -> {
                    datas.computeIfAbsent(rs.getInt(1), k -> new TreeSet<>()).add(LocalDate.parse(rs.getString(2)));
                    return null;
                }, "datas das missões");
        return Consultas.listar("SELECT * FROM missoes WHERE usuario_id = ? ORDER BY titulo COLLATE NOCASE",
                List.of(usuarioId), rs -> ler(rs, datas.getOrDefault(rs.getInt("id"), new TreeSet<>())), "missões");
    }

    public Optional<Missao> buscar(int id) {
        return listarTodas().stream().filter(m -> m.id() == id).findFirst();
    }

    public boolean pertence(int missaoId) {
        return Consultas.existe("SELECT 1 FROM missoes WHERE id = ? AND usuario_id = ?", missaoId, usuarioId);
    }

    /** Grava uma missão nova e devolve o id. */
    public int salvar(Missao m) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO missoes (usuario_id, titulo, descricao, tipo, dias_semana, dia_mes, data_inicio, data_fim, "
                            + "concluida_em, criada_em) VALUES (?,?,?,?,?,?,?,?,?, datetime('now','localtime'))",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, usuarioId);
                preencher(ps, 2, m);
                ps.executeUpdate();
                int id;
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                gravarDatas(conn, id, m.datas());
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar missão: " + e.getMessage(), e);
        }
    }

    /** Altera a missão. Conclusões de dias que deixaram de fazer parte dela são apagadas. */
    public void atualizar(Missao m) {
        if (!pertence(m.id())) throw new RuntimeException("Missão não encontrada com ID " + m.id());
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE missoes SET titulo=?, descricao=?, tipo=?, "
                    + "dias_semana=?, dia_mes=?, data_inicio=?, data_fim=?, concluida_em=? WHERE id=? AND usuario_id=?");
                 PreparedStatement del = conn.prepareStatement("DELETE FROM missao_datas WHERE missao_id=?");
                 PreparedStatement conclusoes = conn.prepareStatement("SELECT data FROM missao_conclusoes WHERE missao_id=?");
                 PreparedStatement delConclusao = conn.prepareStatement(
                         "DELETE FROM missao_conclusoes WHERE missao_id=? AND data=?")) {
                int i = preencher(ps, 1, m);
                ps.setInt(i, m.id()); ps.setInt(i + 1, usuarioId);
                ps.executeUpdate();
                del.setInt(1, m.id());
                del.executeUpdate();
                gravarDatas(conn, m.id(), m.datas());
                conclusoes.setInt(1, m.id());
                List<String> orfas = new ArrayList<>();
                try (ResultSet rs = conclusoes.executeQuery()) {
                    while (rs.next()) if (!m.ocorreEm(LocalDate.parse(rs.getString(1)))) orfas.add(rs.getString(1));
                }
                for (String data : orfas) {
                    delConclusao.setInt(1, m.id()); delConclusao.setString(2, data);
                    delConclusao.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar missão: " + e.getMessage(), e);
        }
    }

    /** Exclui a missão com os dias e as conclusões dela. */
    public void excluir(int id) {
        if (!pertence(id)) throw new RuntimeException("Missão não encontrada com ID " + id);
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement d1 = conn.prepareStatement("DELETE FROM missao_datas WHERE missao_id=?");
                 PreparedStatement d2 = conn.prepareStatement("DELETE FROM missao_conclusoes WHERE missao_id=?");
                 PreparedStatement d3 = conn.prepareStatement("DELETE FROM missoes WHERE id=? AND usuario_id=?")) {
                d1.setInt(1, id); d1.executeUpdate();
                d2.setInt(1, id); d2.executeUpdate();
                d3.setInt(1, id); d3.setInt(2, usuarioId); d3.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir missão: " + e.getMessage(), e);
        }
    }

    // --- Conclusões ---

    /** Dias cumpridos de cada missão do usuário entre {@code de} e {@code ate} (inclusive). */
    public Map<Integer, Set<LocalDate>> conclusoes(LocalDate de, LocalDate ate) {
        Map<Integer, Set<LocalDate>> mapa = new HashMap<>();
        Consultas.listar("SELECT c.missao_id, c.data FROM missao_conclusoes c JOIN missoes m ON m.id = c.missao_id "
                        + "WHERE m.usuario_id = ? AND c.data BETWEEN ? AND ?",
                List.of(usuarioId, de.toString(), ate.toString()), rs -> {
                    mapa.computeIfAbsent(rs.getInt(1), k -> new HashSet<>()).add(LocalDate.parse(rs.getString(2)));
                    return null;
                }, "conclusões");
        return mapa;
    }

    /** Quantas ocorrências de missões o usuário já cumpriu (para o XP). */
    public int totalConclusoes() {
        return Consultas.listar("SELECT COUNT(*) FROM missao_conclusoes c JOIN missoes m ON m.id = c.missao_id "
                + "WHERE m.usuario_id = ?", List.of(usuarioId), rs -> rs.getInt(1), "conclusões").get(0);
    }

    /** Quantas missões sem prazo o usuário já cumpriu (para o XP). */
    public int totalSemPrazoConcluidas() {
        return Consultas.listar("SELECT COUNT(*) FROM missoes WHERE usuario_id = ? AND tipo = ? AND concluida_em IS NOT NULL",
                List.of(usuarioId, TipoMissao.SEM_PRAZO.name()), rs -> rs.getInt(1), "missões").get(0);
    }

    public void marcarConcluida(int missaoId, LocalDate dia) {
        executar("INSERT OR IGNORE INTO missao_conclusoes (missao_id, data, concluida_em) "
                + "SELECT id, ?, datetime('now','localtime') FROM missoes WHERE id = ? AND usuario_id = ?",
                dia.toString(), missaoId, usuarioId);
    }

    public void desmarcarConcluida(int missaoId, LocalDate dia) {
        executar("DELETE FROM missao_conclusoes WHERE data = ? AND missao_id IN "
                + "(SELECT id FROM missoes WHERE id = ? AND usuario_id = ?)", dia.toString(), missaoId, usuarioId);
    }

    /** Missão sem prazo: grava (ou apaga, com {@code dia} nulo) a data em que foi cumprida. */
    public void definirConclusaoSemPrazo(int missaoId, LocalDate dia) {
        executar("UPDATE missoes SET concluida_em = ? WHERE id = ? AND usuario_id = ?",
                dia != null ? dia.toString() : null, missaoId, usuarioId);
    }

    // --- Auxiliares ---

    /** Preenche titulo..concluida_em a partir da posição {@code i}; devolve a próxima posição livre. */
    private static int preencher(PreparedStatement ps, int i, Missao m) throws SQLException {
        ps.setString(i++, m.titulo());
        ps.setString(i++, m.descricao());
        ps.setString(i++, m.tipo().name());
        ps.setString(i++, m.diasSemana().isEmpty() ? null
                : m.diasSemana().stream().sorted().map(d -> String.valueOf(d.getValue())).collect(Collectors.joining(",")));
        if (m.diaMes() != null) ps.setInt(i++, m.diaMes()); else ps.setNull(i++, Types.INTEGER);
        ps.setString(i++, m.inicio() != null ? m.inicio().toString() : null);
        ps.setString(i++, m.fim() != null ? m.fim().toString() : null);
        ps.setString(i++, m.concluidaEm() != null ? m.concluidaEm().toString() : null);
        return i;
    }

    private static void gravarDatas(Connection conn, int missaoId, Set<LocalDate> datas) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO missao_datas (missao_id, data) VALUES (?,?)")) {
            for (LocalDate d : datas) {
                ps.setInt(1, missaoId); ps.setString(2, d.toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static Missao ler(ResultSet rs, SortedSet<LocalDate> datas) throws SQLException {
        String dias = rs.getString("dias_semana");
        Set<DayOfWeek> diasSemana = dias == null || dias.isBlank() ? Set.of()
                : Arrays.stream(dias.split(",")).map(s -> DayOfWeek.of(Integer.parseInt(s.trim()))).collect(Collectors.toSet());
        int diaMes = rs.getInt("dia_mes");
        Integer dia = rs.wasNull() ? null : diaMes;
        return new Missao(rs.getInt("id"), rs.getString("titulo"), rs.getString("descricao"),
                TipoMissao.valueOf(rs.getString("tipo")), datas, diasSemana, dia,
                data(rs.getString("data_inicio")), data(rs.getString("data_fim")), data(rs.getString("concluida_em")));
    }

    private static LocalDate data(String s) {
        return s == null || s.isBlank() ? null : LocalDate.parse(s);
    }

    private static void executar(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao gravar missão: " + e.getMessage(), e);
        }
    }
}
