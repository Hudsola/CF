package repository;

import db.DatabaseManager;
import model.CategoriaMissao;
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
        List<Missao> missoes = new ArrayList<>(Consultas.listar("SELECT * FROM missoes WHERE usuario_id = ?",
                List.of(usuarioId), rs -> ler(rs, datas.getOrDefault(rs.getInt("id"), new TreeSet<>())), "missões"));
        missoes.sort(Comparator.comparing(Missao::titulo, ORDEM_ALFABETICA));
        return missoes;
    }

    /** Ordem alfabética do português: ignora maiúsculas e acentos ("Água" vem antes de "Fogo"), o que o SQLite não faz. */
    private static final Comparator<String> ORDEM_ALFABETICA = ordemAlfabetica();

    private static Comparator<String> ordemAlfabetica() {
        java.text.Collator c = java.text.Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        c.setStrength(java.text.Collator.PRIMARY);
        return c::compare;
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
                            + "concluida_em, categoria, missao_pai_id, criada_em) VALUES (?,?,?,?,?,?,?,?,?,?,?, datetime('now','localtime'))",
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
                    + "dias_semana=?, dia_mes=?, data_inicio=?, data_fim=?, concluida_em=?, categoria=?, missao_pai_id=? "
                    + "WHERE id=? AND usuario_id=?");
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

    /** Exclui a missão, as sub-missões dela e os dias e as conclusões de todas, numa única transação. */
    public void excluir(int id) {
        if (!pertence(id)) throw new RuntimeException("Missão não encontrada com ID " + id);
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            String daFamilia = " IN (SELECT id FROM missoes WHERE (id = ? OR missao_pai_id = ?) AND usuario_id = ?)";
            try (PreparedStatement d1 = conn.prepareStatement("DELETE FROM missao_datas WHERE missao_id" + daFamilia);
                 PreparedStatement d2 = conn.prepareStatement("DELETE FROM missao_conclusoes WHERE missao_id" + daFamilia);
                 PreparedStatement d3 = conn.prepareStatement("DELETE FROM missoes WHERE missao_pai_id=? AND usuario_id=?");
                 PreparedStatement d4 = conn.prepareStatement("DELETE FROM missoes WHERE id=? AND usuario_id=?")) {
                for (PreparedStatement ps : List.of(d1, d2)) {
                    ps.setInt(1, id); ps.setInt(2, id); ps.setInt(3, usuarioId);
                    ps.executeUpdate();
                }
                d3.setInt(1, id); d3.setInt(2, usuarioId); d3.executeUpdate();
                d4.setInt(1, id); d4.setInt(2, usuarioId); d4.executeUpdate();
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

    /** Quantas ocorrências de missões o usuário já cumpriu, por categoria (para o XP). */
    public Map<CategoriaMissao, Integer> totalConclusoes() {
        return porCategoria("SELECT m.categoria, COUNT(*) FROM missao_conclusoes c JOIN missoes m ON m.id = c.missao_id "
                + "WHERE m.usuario_id = ? GROUP BY m.categoria", List.of(usuarioId));
    }

    /** Quantas missões sem prazo o usuário já cumpriu, por categoria (para o XP). */
    public Map<CategoriaMissao, Integer> totalSemPrazoConcluidas() {
        return porCategoria("SELECT categoria, COUNT(*) FROM missoes WHERE usuario_id = ? AND tipo = ? "
                + "AND concluida_em IS NOT NULL GROUP BY categoria", List.of(usuarioId, TipoMissao.SEM_PRAZO.name()));
    }

    private static Map<CategoriaMissao, Integer> porCategoria(String sql, List<Object> params) {
        Map<CategoriaMissao, Integer> mapa = new EnumMap<>(CategoriaMissao.class);
        for (CategoriaMissao c : CategoriaMissao.values()) mapa.put(c, 0);
        Consultas.listar(sql, params, rs -> mapa.put(CategoriaMissao.valueOf(rs.getString(1)), rs.getInt(2)), "conclusões");
        return mapa;
    }

    /** Sub-missões acompanham a categoria da mãe. */
    public void definirCategoriaDasSubMissoes(int paiId, CategoriaMissao categoria) {
        executar("UPDATE missoes SET categoria = ? WHERE missao_pai_id = ? AND usuario_id = ?",
                categoria.name(), paiId, usuarioId);
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

    /** Preenche titulo..missao_pai_id a partir da posição {@code i}; devolve a próxima posição livre. */
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
        ps.setString(i++, m.categoria().name());
        if (m.missaoPaiId() != null) ps.setInt(i++, m.missaoPaiId()); else ps.setNull(i++, Types.INTEGER);
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
        int paiId = rs.getInt("missao_pai_id");
        Integer pai = rs.wasNull() ? null : paiId;
        return new Missao(rs.getInt("id"), rs.getString("titulo"), rs.getString("descricao"),
                TipoMissao.valueOf(rs.getString("tipo")), datas, diasSemana, dia,
                data(rs.getString("data_inicio")), data(rs.getString("data_fim")), data(rs.getString("concluida_em")),
                CategoriaMissao.valueOf(rs.getString("categoria")), pai);
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
