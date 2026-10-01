package util;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class Assercoes {

    private Assercoes() {}

    /** Compara valores ignorando a escala ("10.0" == "10.00"). */
    public static void assertValor(String esperado, BigDecimal atual) {
        assertNotNull(atual, "valor nulo");
        assertEquals(0, new BigDecimal(esperado).compareTo(atual),
                () -> "esperado <" + esperado + "> mas foi <" + atual.toPlainString() + ">");
    }

    public static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }
}
