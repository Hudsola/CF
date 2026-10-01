package service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ConversorTest {

    @Test
    void deveConverterValoresNoFormatoBrasileiroEAmericano() {
        assertEquals(1500.00, Conversor.parseValor("1.500,00"), 0.001);
        assertEquals(1500.00, Conversor.parseValor("1500,00"), 0.001);
        assertEquals(1500.00, Conversor.parseValor("1500.00"), 0.001);
        assertEquals(1500.00, Conversor.parseValor("1.500"), 0.001);
        assertEquals(1500.00, Conversor.parseValor("R$ 1.500,00"), 0.001);
        assertEquals(1234567.89, Conversor.parseValor("1,234,567.89"), 0.001);
        assertEquals(-12.30, Conversor.parseValor("-12,30"), 0.001);
        assertEquals(15.5, Conversor.parseValor("\"15.5\""), 0.001);
    }

    @Test
    void deveRejeitarValoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseValor(""));
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseValor("abc"));
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseValor("NaN"));
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseValor("1e5"));
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseValorPositivo("0"));
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseValorPositivo("-5,00"));
    }

    @Test
    void deveConverterDatas() {
        assertEquals(LocalDate.of(2026, 7, 30), Conversor.parseData("2026-07-30"));
        assertEquals(LocalDate.of(2026, 7, 30), Conversor.parseData("30/07/2026"));
        assertEquals(LocalDate.of(2026, 7, 3), Conversor.parseData("3/7/2026"));
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseData("31/02/2026"));
        assertThrows(IllegalArgumentException.class, () -> Conversor.parseData("ontem"));
    }

    @Test
    void deveRetornarNomeDoMesComoGravadoNoBanco() {
        assertEquals("MARÇO", Conversor.nomeMes(LocalDate.of(2025, 3, 1)));
        assertEquals("DEZEMBRO", Conversor.nomeMes(LocalDate.of(2025, 12, 31)));
    }
}
