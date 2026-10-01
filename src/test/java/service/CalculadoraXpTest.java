package service;

import model.Despesa;
import model.Investimento;
import model.Progresso;
import model.Receita;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraXpTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);

    private static Receita rec(String valor, LocalDate d) { return new Receita("R", new BigDecimal(valor), 1, d); }
    private static Despesa desp(String valor, LocalDate d) { return new Despesa(1, "D", new BigDecimal(valor), 1, d); }
    private static Investimento inv(String valor, LocalDate d) { return new Investimento("I", new BigDecimal(valor), 1, d); }

    @Test
    void semLancamentosComecaNoNivel1() {
        Progresso p = CalculadoraXp.calcular(List.of(), List.of(), List.of(), HOJE);
        assertEquals(new Progresso(1, 0, 0, 100), p);
        assertEquals(0.0, p.fracaoNivel());
    }

    @Test
    void deveSomarXpPorLancamentoMesPositivoEMesComInvestimento() {
        LocalDate agosto = LocalDate.of(2026, 8, 5);
        LocalDate setembro = LocalDate.of(2026, 9, 5);
        LocalDate outubro = LocalDate.of(2026, 10, 5);
        Progresso p = CalculadoraXp.calcular(
                List.of(rec("3000", agosto), rec("3000", setembro), rec("3000", outubro)),
                List.of(desp("1000", agosto), desp("5000", setembro)),     // setembro fecha negativo
                List.of(inv("500", agosto)),
                HOJE);

        // 6 lançamentos × 10 + agosto positivo 50 + 1 mês com investimento 30.
        // Outubro tem saldo positivo, mas ainda não terminou.
        assertEquals(60 + 50 + 30, p.xpTotal());
    }

    @Test
    void niveisExigemCadaVezMaisXp() {
        assertEquals(new Progresso(1, 99, 99, 100), CalculadoraXp.progressoPara(99));
        assertEquals(new Progresso(2, 100, 0, 200), CalculadoraXp.progressoPara(100));
        assertEquals(new Progresso(2, 299, 199, 200), CalculadoraXp.progressoPara(299));
        assertEquals(new Progresso(3, 300, 0, 300), CalculadoraXp.progressoPara(300));
        assertEquals(0.5, CalculadoraXp.progressoPara(450).fracaoNivel(), 1e-9);   // nível 3: 150 de 300
    }
}
