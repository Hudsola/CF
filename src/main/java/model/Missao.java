package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Missão do usuário. Conforme o {@link TipoMissao}, só alguns campos são usados:
 * <ul>
 *   <li>DIARIA: {@code datas} (um ou mais dias escolhidos);</li>
 *   <li>SEMANAL: {@code diasSemana}, {@code inicio} e, opcionalmente, {@code fim};</li>
 *   <li>MENSAL: {@code diaMes} (1 a 31), {@code inicio} e, opcionalmente, {@code fim};</li>
 *   <li>SEM_PRAZO: nenhum; {@code concluidaEm} diz quando foi cumprida.</li>
 * </ul>
 * Use as fábricas ({@link #diaria}, {@link #semanal}, {@link #mensal}, {@link #semPrazo}), que criam missões
 * secundárias e sem missão-mãe, e ajuste com {@link #comCategoria} e {@link #comMissaoPai}. A validação fica
 * em {@code service.Missoes}.
 *
 * @param missaoPaiId missão-mãe, se esta for uma sub-missão (um nível só). Sub-missões sem prazo são os
 *                    requisitos da mãe: ela só pode ser concluída depois deles.
 */
public record Missao(int id, String titulo, String descricao, TipoMissao tipo,
                     SortedSet<LocalDate> datas, Set<DayOfWeek> diasSemana, Integer diaMes,
                     LocalDate inicio, LocalDate fim, LocalDate concluidaEm,
                     CategoriaMissao categoria, Integer missaoPaiId) {

    private static final Locale PT = Locale.forLanguageTag("pt-BR");

    /** Construtor sem categoria nem missão-mãe (missão secundária independente). */
    public Missao(int id, String titulo, String descricao, TipoMissao tipo,
                  SortedSet<LocalDate> datas, Set<DayOfWeek> diasSemana, Integer diaMes,
                  LocalDate inicio, LocalDate fim, LocalDate concluidaEm) {
        this(id, titulo, descricao, tipo, datas, diasSemana, diaMes, inicio, fim, concluidaEm, null, null);
    }

    public Missao {
        categoria = categoria == null ? CategoriaMissao.SECUNDARIA : categoria;
        titulo = titulo == null ? "" : titulo.trim();
        descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
        datas = Collections.unmodifiableSortedSet(new TreeSet<>(datas == null ? Set.of() : datas));
        diasSemana = diasSemana == null || diasSemana.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(DayOfWeek.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(diasSemana));
    }

    public static Missao diaria(String titulo, String descricao, Collection<LocalDate> datas) {
        return new Missao(0, titulo, descricao, TipoMissao.DIARIA, new TreeSet<>(datas), null, null, null, null, null);
    }

    public static Missao semanal(String titulo, String descricao, Set<DayOfWeek> dias, LocalDate inicio, LocalDate fim) {
        return new Missao(0, titulo, descricao, TipoMissao.SEMANAL, null, dias, null, inicio, fim, null);
    }

    public static Missao mensal(String titulo, String descricao, int diaMes, LocalDate inicio, LocalDate fim) {
        return new Missao(0, titulo, descricao, TipoMissao.MENSAL, null, null, diaMes, inicio, fim, null);
    }

    public static Missao semPrazo(String titulo, String descricao) {
        return new Missao(0, titulo, descricao, TipoMissao.SEM_PRAZO, null, null, null, null, null, null);
    }

    public Missao comId(int novoId) {
        return new Missao(novoId, titulo, descricao, tipo, datas, diasSemana, diaMes, inicio, fim, concluidaEm,
                categoria, missaoPaiId);
    }

    public Missao comConclusao(LocalDate data) {
        return new Missao(id, titulo, descricao, tipo, datas, diasSemana, diaMes, inicio, fim, data, categoria, missaoPaiId);
    }

    public Missao comCategoria(CategoriaMissao nova) {
        return new Missao(id, titulo, descricao, tipo, datas, diasSemana, diaMes, inicio, fim, concluidaEm, nova, missaoPaiId);
    }

    /** Liga a missão a uma missão-mãe (nulo = missão independente). */
    public Missao comMissaoPai(Integer paiId) {
        return new Missao(id, titulo, descricao, tipo, datas, diasSemana, diaMes, inicio, fim, concluidaEm, categoria, paiId);
    }

    public boolean principal() { return categoria == CategoriaMissao.PRINCIPAL; }

    public boolean subMissao() { return missaoPaiId != null; }

    /** Sub-missão sem prazo: um requisito que a mãe precisa antes de ser concluída. */
    public boolean requisito() { return subMissao() && tipo == TipoMissao.SEM_PRAZO; }

    /** true se a missão deve ser cumprida nesse dia (missões sem prazo não têm dia). */
    public boolean ocorreEm(LocalDate dia) {
        return switch (tipo) {
            case DIARIA -> datas.contains(dia);
            case SEMANAL -> dentroDoPeriodo(dia) && diasSemana.contains(dia.getDayOfWeek());
            case MENSAL -> dentroDoPeriodo(dia) && diaMes != null
                    && dia.getDayOfMonth() == Math.min(diaMes, dia.lengthOfMonth());   // dia 31 em fevereiro = dia 28/29
            case SEM_PRAZO -> false;
        };
    }

    private boolean dentroDoPeriodo(LocalDate dia) {
        return (inicio == null || !dia.isBefore(inicio)) && (fim == null || !dia.isAfter(fim));
    }

    /** Primeiro dia, a partir de {@code desde} (inclusive), em que a missão ocorre; vazio se não houver. */
    public Optional<LocalDate> proximaOcorrencia(LocalDate desde) {
        if (tipo == TipoMissao.SEM_PRAZO) return Optional.empty();
        if (tipo == TipoMissao.DIARIA) {
            SortedSet<LocalDate> seguintes = datas.tailSet(desde);
            return seguintes.isEmpty() ? Optional.empty() : Optional.of(seguintes.first());
        }
        LocalDate dia = inicio != null && inicio.isAfter(desde) ? inicio : desde;
        for (int i = 0; i < 400; i++, dia = dia.plusDays(1)) {   // cobre qualquer dia do mês dentro de 1 ano
            if (fim != null && dia.isAfter(fim)) return Optional.empty();
            if (ocorreEm(dia)) return Optional.of(dia);
        }
        return Optional.empty();
    }

    /** Texto curto de quando a missão acontece, ex: "seg, qua, sex", "todo dia 5", "17/10/2026 e 20/10/2026". */
    public String quando() {
        return switch (tipo) {
            case DIARIA -> datas.stream().map(d -> String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear()))
                    .collect(Collectors.joining(", "));
            case SEMANAL -> diasSemana.stream().sorted()
                    .map(d -> d.getDisplayName(TextStyle.SHORT, PT).replace(".", "")).collect(Collectors.joining(", "));
            case MENSAL -> "todo dia " + diaMes;
            case SEM_PRAZO -> "sem prazo";
        };
    }

    @Override public String toString() { return titulo; }
}
