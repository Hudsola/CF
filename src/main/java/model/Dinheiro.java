package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Regras de dinheiro do app: valores em {@link BigDecimal} com 2 casas.
 *
 * No banco os valores ficam em colunas REAL (legíveis no DB Browser). Como só gravamos valores
 * com 2 casas, arredondar na leitura devolve exatamente o valor gravado, e todas as contas
 * passam a ser feitas em BigDecimal, sem os erros de arredondamento do double.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Dinheiro() {}

    /** Valor lido do banco (REAL) arredondado para centavos. */
    public static BigDecimal de(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal de(String valor) {
        return new BigDecimal(valor).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** "R$ 1.234,56" (ou "-R$ 1.234,56"). */
    public static String formatar(BigDecimal valor) {
        NumberFormat f = NumberFormat.getCurrencyInstance(PT_BR);
        return f.format(valor).replace(' ', ' ');
    }

    /** "1.234,56" — para preencher campos de formulário. */
    public static String formatarSemSimbolo(BigDecimal valor) {
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(valor);
    }

    /** Fração (0.255) formatada como "25,5%". */
    public static String formatarPercentual(BigDecimal fracao) {
        NumberFormat f = NumberFormat.getPercentInstance(PT_BR);
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(1);
        return f.format(fracao).replace(' ', ' ');
    }

    /** parte / total como fração com 4 casas; zero quando o total é zero. */
    public static BigDecimal fracao(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) return BigDecimal.ZERO.setScale(4);
        return parte.divide(total, 4, RoundingMode.HALF_EVEN);
    }
}
