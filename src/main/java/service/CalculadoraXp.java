package service;

import model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Nível e XP do usuário, sempre recalculados a partir dos lançamentos (nada é gravado no banco,
 * então o XP nunca fica fora de sincronia com os dados).
 *
 * Regras:
 *  - +{@value #XP_POR_LANCAMENTO} XP por lançamento registrado (receita, despesa ou investimento);
 *  - +{@value #XP_MES_POSITIVO} XP por mês já encerrado que fechou com saldo positivo;
 *  - +{@value #XP_MES_COM_INVESTIMENTO} XP por mês com pelo menos um investimento.
 * Para sair do nível N para o N+1 são necessários N × {@value #XP_BASE_NIVEL} XP.
 */
public final class CalculadoraXp {

    public static final int XP_POR_LANCAMENTO = 10;
    public static final int XP_MES_POSITIVO = 50;
    public static final int XP_MES_COM_INVESTIMENTO = 30;
    public static final int XP_BASE_NIVEL = 100;

    private CalculadoraXp() {}

    public static Progresso calcular(List<Receita> receitas, List<Despesa> despesas,
                                     List<Investimento> investimentos, LocalDate hoje) {
        int xp = (receitas.size() + despesas.size() + investimentos.size()) * XP_POR_LANCAMENTO;

        Map<YearMonth, BigDecimal> saldoPorMes = new HashMap<>();
        receitas.forEach(r -> saldoPorMes.merge(YearMonth.from(r.getData()), r.getValor(), BigDecimal::add));
        despesas.forEach(d -> saldoPorMes.merge(YearMonth.from(d.getData()), d.getValor().negate(), BigDecimal::add));
        investimentos.forEach(i -> saldoPorMes.merge(YearMonth.from(i.getData()), i.getValor().negate(), BigDecimal::add));

        YearMonth mesAtual = YearMonth.from(hoje);
        for (Map.Entry<YearMonth, BigDecimal> e : saldoPorMes.entrySet())
            if (e.getKey().isBefore(mesAtual) && e.getValue().signum() > 0) xp += XP_MES_POSITIVO;

        Set<YearMonth> mesesComInvestimento = new HashSet<>();
        investimentos.forEach(i -> mesesComInvestimento.add(YearMonth.from(i.getData())));
        xp += mesesComInvestimento.size() * XP_MES_COM_INVESTIMENTO;

        return progressoPara(xp);
    }

    /** Converte XP total em nível: o nível N exige N × 100 XP para subir. */
    public static Progresso progressoPara(int xpTotal) {
        int nivel = 1, restante = xpTotal;
        while (restante >= nivel * XP_BASE_NIVEL) {
            restante -= nivel * XP_BASE_NIVEL;
            nivel++;
        }
        return new Progresso(nivel, xpTotal, restante, nivel * XP_BASE_NIVEL);
    }
}
