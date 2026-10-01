package service;

import model.Dinheiro;
import model.Meses;
import model.Periodo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DinheiroEPeriodoTest {

    @Test
    void deveFormatarEmReais() {
        assertEquals("R$ 1.234,56", Dinheiro.formatar(new BigDecimal("1234.56")));
        assertEquals("-R$ 10,00", Dinheiro.formatar(new BigDecimal("-10")));
        assertEquals("1.234,50", Dinheiro.formatarSemSimbolo(new BigDecimal("1234.5")));
        assertEquals("25,5%", Dinheiro.formatarPercentual(new BigDecimal("0.255")));
    }

    @Test
    void deveLerValorDoBancoSemErroDeDouble() {
        assertEquals(new BigDecimal("0.30"), Dinheiro.de(0.1 + 0.2));
        assertEquals(new BigDecimal("636.02"), Dinheiro.de(636.02));
    }

    @Test
    void periodoDeMesOuAno() {
        assertEquals(new Periodo(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)), Periodo.de(2024, "fevereiro"));
        assertEquals(Periodo.doAno(2025), Periodo.de(2025, Meses.TODOS));
        assertTrue(Periodo.de(2025, "MARÇO").contem(LocalDate.of(2025, 3, 31)));
        assertFalse(Periodo.de(2025, "MARÇO").contem(LocalDate.of(2025, 4, 1)));
        assertThrows(IllegalArgumentException.class, () -> Periodo.de(2025, "Marco"));
    }
}
