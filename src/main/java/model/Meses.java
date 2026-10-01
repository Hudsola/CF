package model;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Nomes dos meses como exibidos no app ("JANEIRO" … "DEZEMBRO"). */
public final class Meses {

    public static final List<String> NOMES = List.of(
            "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
            "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO");

    /** Valor usado nos filtros para "ano inteiro". */
    public static final String TODOS = "Todos";

    private Meses() {}

    public static String nome(LocalDate data) {
        return NOMES.get(data.getMonthValue() - 1);
    }

    public static String nome(int numeroMes) {
        return NOMES.get(numeroMes - 1);
    }

    /** 1 a 12 para "março", "Março" ou "MARÇO"; erro se não for um mês. */
    public static int numero(String nome) {
        String n = nome == null ? "" : nome.trim().toUpperCase(Locale.ROOT);
        int i = NOMES.indexOf(n);
        if (i < 0) throw new IllegalArgumentException("Mês inválido: \"" + nome + "\".");
        return i + 1;
    }

    public static boolean ehTodos(String mes) {
        return mes == null || TODOS.equalsIgnoreCase(mes.trim());
    }
}
