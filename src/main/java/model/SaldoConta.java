package model;

import java.math.BigDecimal;

/**
 * Movimento de uma conta (no histórico inteiro ou num período).
 *
 * @param saldoInicial saldo de abertura da conta; zero quando o cálculo é só do movimento de um período
 */
public record SaldoConta(Conta conta, BigDecimal saldoInicial, BigDecimal receitas,
                         BigDecimal despesas, BigDecimal investimentos) {

    /** Resultado dos lançamentos: receitas − despesas − investimentos. */
    public BigDecimal movimento() {
        return receitas.subtract(despesas).subtract(investimentos);
    }

    /** Saldo inicial + movimento. */
    public BigDecimal saldo() {
        return saldoInicial.add(movimento());
    }

    public String getNome() { return conta.getNome(); }
}
