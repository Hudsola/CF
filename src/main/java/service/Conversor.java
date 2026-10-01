package service;

import model.Dinheiro;
import model.Meses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Pattern;

/** Conversões de texto digitado pelo usuário ou lido de CSV (valores em R$, datas, nome do mês). */
public final class Conversor {

    private static final Pattern NUMERO = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern MILHAR_COM_PONTO = Pattern.compile("-?\\d{1,3}(\\.\\d{3})+");

    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT));

    private Conversor() {}

    /**
     * Converte "1.500,00", "1500,00", "1500.00", "1.500", "R$ 1.500,00" ou "-12,30" em valor com 2 casas.
     * O separador que aparece por último (vírgula ou ponto) é tratado como decimal.
     */
    public static BigDecimal parseValor(String texto) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Informe um valor.");
        String s = texto.replace("R$", "").replace("\"", "").replaceAll("[\\s\\u00A0]", "");
        int virgula = s.lastIndexOf(','), ponto = s.lastIndexOf('.');
        if (virgula >= 0 && ponto >= 0) {
            s = virgula > ponto ? s.replace(".", "").replace(',', '.') : s.replace(",", "");
        } else if (virgula >= 0) {
            s = s.replace(',', '.');
        } else if (MILHAR_COM_PONTO.matcher(s).matches()) {
            s = s.replace(".", "");          // "1.500" ou "1.500.000" = separador de milhar
        }
        if (!NUMERO.matcher(s).matches())
            throw new IllegalArgumentException("Valor inválido: \"" + texto.trim() + "\". Use o formato 1.500,00.");
        BigDecimal valor = new BigDecimal(s);
        if (valor.scale() > 2)
            throw new IllegalArgumentException("Valor inválido: \"" + texto.trim() + "\". Use no máximo 2 casas decimais.");
        return Dinheiro.arredondar(valor);
    }

    /** Igual a {@link #parseValor(String)}, mas exige valor maior que zero. */
    public static BigDecimal parseValorPositivo(String texto) {
        BigDecimal v = parseValor(texto);
        if (v.signum() <= 0) throw new IllegalArgumentException("Valor deve ser maior que zero.");
        return v;
    }

    /** Aceita yyyy-MM-dd, dd/MM/yyyy e dd-MM-yyyy. */
    public static LocalDate parseData(String texto) {
        String s = texto == null ? "" : texto.trim().replace("\"", "");
        for (DateTimeFormatter f : FORMATOS_DATA) {
            try { return LocalDate.parse(s, f); } catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("Data inválida: \"" + s + "\". Use dd/mm/aaaa ou aaaa-mm-dd.");
    }

    /** Nome do mês em maiúsculas (ex: "MARÇO"). */
    public static String nomeMes(LocalDate data) {
        return Meses.nome(data);
    }
}
