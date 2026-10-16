package service;

import model.Missao;
import model.OcorrenciaMissao;
import model.TipoMissao;
import repository.MissaoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Missões do usuário: cadastro com validação, ocorrências por dia (o que aparece na Agenda) e conclusão.
 * Cada ocorrência cumprida e cada missão sem prazo cumprida rendem XP (ver {@link CalculadoraXp}).
 */
public class Missoes {

    public static final int TAMANHO_MAXIMO_TITULO = 100;
    /** Maior período consultado de uma vez (a Agenda pede no máximo ~6 semanas). */
    static final int DIAS_MAXIMOS_CONSULTA = 366;

    /** Contagens para o card de Missões na Home. */
    public record Resumo(int pendentesHoje, int feitasHoje, int atrasadas, int semPrazoEmAndamento) {}

    private final MissaoRepository repo;
    private final Clock relogio;

    public Missoes(int usuarioId) { this(usuarioId, Clock.systemDefaultZone()); }

    /** O relógio define o que é "hoje" (configurável nos testes). */
    public Missoes(int usuarioId, Clock relogio) {
        this.repo = new MissaoRepository(usuarioId);
        this.relogio = relogio;
    }

    public LocalDate hoje() { return LocalDate.now(relogio); }

    // --- Cadastro ---

    public List<Missao> listar() { return repo.listarTodas(); }

    public Optional<Missao> buscar(int id) { return repo.buscar(id); }

    /** Valida e grava uma missão nova; devolve ela com o id. */
    public Missao criar(Missao m) {
        validar(m);
        return m.comConclusao(null).comId(repo.salvar(m.comConclusao(null)));
    }

    /** Altera título, descrição, tipo e datas. A conclusão de uma missão sem prazo é mantida. */
    public void atualizar(Missao m) {
        Missao atual = repo.buscar(m.id()).orElseThrow(() -> new IllegalArgumentException("Missão não encontrada."));
        validar(m);
        LocalDate conclusao = m.tipo() == TipoMissao.SEM_PRAZO && atual.tipo() == TipoMissao.SEM_PRAZO ? atual.concluidaEm() : null;
        repo.atualizar(m.comConclusao(conclusao));
    }

    public void excluir(int id) { repo.excluir(id); }

    // --- Ocorrências (Agenda) ---

    /** Ocorrências de todas as missões entre {@code de} e {@code ate} (inclusive), por data e título. */
    public List<OcorrenciaMissao> ocorrencias(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) throw new IllegalArgumentException("Período inválido: o fim é antes do início.");
        if (ChronoUnit.DAYS.between(de, ate) >= DIAS_MAXIMOS_CONSULTA)
            throw new IllegalArgumentException("Período longo demais (máximo de " + DIAS_MAXIMOS_CONSULTA + " dias).");
        List<Missao> missoes = repo.listarTodas();
        Map<Integer, Set<LocalDate>> feitas = repo.conclusoes(de, ate);
        List<OcorrenciaMissao> lista = new ArrayList<>();
        for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1))
            for (Missao m : missoes)
                if (m.ocorreEm(dia))
                    lista.add(new OcorrenciaMissao(m, dia, feitas.getOrDefault(m.id(), Set.of()).contains(dia)));
        lista.sort(Comparator.comparing(OcorrenciaMissao::data)
                .thenComparing(o -> o.missao().titulo(), String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    public List<OcorrenciaMissao> doDia(LocalDate dia) { return ocorrencias(dia, dia); }

    /** Ocorrências dos últimos {@code dias} dias (sem contar hoje) que não foram cumpridas. */
    public List<OcorrenciaMissao> atrasadas(int dias) {
        LocalDate ontem = hoje().minusDays(1);
        return ocorrencias(ontem.minusDays(dias - 1L), ontem).stream().filter(o -> !o.concluida()).toList();
    }

    /** Missões sem prazo ainda não cumpridas. */
    public List<Missao> semPrazoEmAndamento() {
        return repo.listarTodas().stream()
                .filter(m -> m.tipo() == TipoMissao.SEM_PRAZO && m.concluidaEm() == null).toList();
    }

    public Resumo resumo() {
        List<OcorrenciaMissao> deHoje = doDia(hoje());
        int feitas = (int) deHoje.stream().filter(OcorrenciaMissao::concluida).count();
        return new Resumo(deHoje.size() - feitas, feitas, atrasadas(7).size(), semPrazoEmAndamento().size());
    }

    // --- Conclusão ---

    /** Marca a missão como cumprida nesse dia (que não pode ser futuro). */
    public void concluir(int missaoId, LocalDate dia) {
        Missao m = repo.buscar(missaoId).orElseThrow(() -> new IllegalArgumentException("Missão não encontrada."));
        if (m.tipo() == TipoMissao.SEM_PRAZO)
            throw new IllegalArgumentException("Missão sem prazo: use \"Concluir missão\".");
        if (!m.ocorreEm(dia))
            throw new IllegalArgumentException("A missão \"" + m.titulo() + "\" não acontece em " + Datas.formatar(dia) + ".");
        if (dia.isAfter(hoje()))
            throw new IllegalArgumentException("Não dá para concluir uma missão de um dia que ainda não chegou.");
        repo.marcarConcluida(missaoId, dia);
    }

    public void desfazerConclusao(int missaoId, LocalDate dia) { repo.desmarcarConcluida(missaoId, dia); }

    /** Cumpre uma missão sem prazo (hoje). */
    public void concluirSemPrazo(int missaoId) {
        Missao m = semPrazo(missaoId);
        if (m.concluidaEm() != null) throw new IllegalArgumentException("Essa missão já foi concluída.");
        repo.definirConclusaoSemPrazo(missaoId, hoje());
    }

    public void reabrirSemPrazo(int missaoId) {
        semPrazo(missaoId);
        repo.definirConclusaoSemPrazo(missaoId, null);
    }

    private Missao semPrazo(int missaoId) {
        Missao m = repo.buscar(missaoId).orElseThrow(() -> new IllegalArgumentException("Missão não encontrada."));
        if (m.tipo() != TipoMissao.SEM_PRAZO)
            throw new IllegalArgumentException("Só missões sem prazo são concluídas de uma vez; as outras, por dia.");
        return m;
    }

    // --- Validação ---

    static void validar(Missao m) {
        if (m.titulo().isEmpty()) throw new IllegalArgumentException("Informe o título da missão.");
        if (m.titulo().length() > TAMANHO_MAXIMO_TITULO)
            throw new IllegalArgumentException("Título com mais de " + TAMANHO_MAXIMO_TITULO + " caracteres.");
        if (m.tipo() == null) throw new IllegalArgumentException("Escolha o tipo da missão.");
        switch (m.tipo()) {
            case DIARIA -> {
                if (m.datas().isEmpty()) throw new IllegalArgumentException("Escolha pelo menos um dia para a missão diária.");
            }
            case SEMANAL -> {
                if (m.diasSemana().isEmpty()) throw new IllegalArgumentException("Escolha pelo menos um dia da semana.");
                validarPeriodo(m);
            }
            case MENSAL -> {
                if (m.diaMes() == null || m.diaMes() < 1 || m.diaMes() > 31)
                    throw new IllegalArgumentException("O dia do mês deve ser de 1 a 31.");
                validarPeriodo(m);
            }
            case SEM_PRAZO -> { }
        }
    }

    private static void validarPeriodo(Missao m) {
        if (m.inicio() == null) throw new IllegalArgumentException("Informe a partir de quando a missão vale.");
        if (m.fim() != null && m.fim().isBefore(m.inicio()))
            throw new IllegalArgumentException("A data final é antes da data de início.");
    }

    /** Formato de data usado nas mensagens. */
    static final class Datas {
        private Datas() {}
        static String formatar(LocalDate d) {
            return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
        }
    }
}
