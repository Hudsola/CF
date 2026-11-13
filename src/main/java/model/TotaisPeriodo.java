package model;

import java.math.BigDecimal;

/**
 * Somas de um período e os indicadores derivados delas.
 *
 * @param investimentos valor aplicado no período (sai do disponível e fica guardado)
 * @param resgatados    valor aplicado que voltou ao disponível por resgates no período (devolução, não é renda);
 *                      o lucro ou prejuízo do resgate já está em {@code receitas} ou {@code despesas}
 */
public record TotaisPeriodo(BigDecimal receitas, BigDecimal despesas, BigDecimal investimentos, BigDecimal resgatados) {

    /** Sem resgates no período. */
    public TotaisPeriodo(BigDecimal receitas, BigDecimal despesas, BigDecimal investimentos) {
        this(receitas, despesas, investimentos, Dinheiro.ZERO);
    }

    /** Variação do saldo disponível: receitas − despesas − aplicado + resgatado. */
    public BigDecimal saldo() {
        return receitas.subtract(despesas).subtract(investimentos).add(resgatados);
    }

    /** Fração da renda gasta em despesas (0.25 = 25%). */
    public BigDecimal percentualGasto() {
        return Dinheiro.fracao(despesas, receitas);
    }

    /** Fração da renda destinada a investimentos (0.10 = 10%). */
    public BigDecimal percentualInvestido() {
        return Dinheiro.fracao(investimentos, receitas);
    }
}
