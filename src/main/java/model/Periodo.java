package model;

import java.time.LocalDate;
import java.time.YearMonth;

/** Intervalo de datas fechado [inicio, fim] usado nas consultas e relatórios. */
public record Periodo(LocalDate inicio, LocalDate fim) {

    public Periodo {
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("Fim do período antes do início.");
    }

    public static Periodo doAno(int ano) {
        return new Periodo(LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31));
    }

    public static Periodo doMes(YearMonth ym) {
        return new Periodo(ym.atDay(1), ym.atEndOfMonth());
    }

    /** Mês do ano, ou o ano inteiro quando {@code mes} é "Todos" ou nulo. */
    public static Periodo de(int ano, String mes) {
        return Meses.ehTodos(mes) ? doAno(ano) : doMes(YearMonth.of(ano, Meses.numero(mes)));
    }

    public boolean contem(LocalDate data) {
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }
}
