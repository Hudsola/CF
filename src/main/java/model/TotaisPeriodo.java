package model;

import java.math.BigDecimal;

/** Somas de um período e os indicadores derivados delas. */
public record TotaisPeriodo(BigDecimal receitas, BigDecimal despesas, BigDecimal investimentos) {

    /** Saldo em conta: receitas − despesas − investimentos. */
    public BigDecimal saldo() {
        return receitas.subtract(despesas).subtract(investimentos);
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
