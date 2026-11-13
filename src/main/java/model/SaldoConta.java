package model;

import java.math.BigDecimal;

/**
 * Movimento de uma conta (no histórico inteiro ou num período).
 *
 * @param saldoInicial  saldo de abertura da conta; zero quando o cálculo é só do movimento de um período
 * @param investimentos valor aplicado a partir da conta
 * @param resgatados    valor aplicado que voltou para a conta em resgates (o lucro ou prejuízo está nas receitas
 *                      ou despesas)
 */
public record SaldoConta(Conta conta, BigDecimal saldoInicial, BigDecimal receitas,
                         BigDecimal despesas, BigDecimal investimentos, BigDecimal resgatados) {

    /** Sem resgates. */
    public SaldoConta(Conta conta, BigDecimal saldoInicial, BigDecimal receitas, BigDecimal despesas, BigDecimal investimentos) {
        this(conta, saldoInicial, receitas, despesas, investimentos, Dinheiro.ZERO);
    }

    /** Resultado dos lançamentos: receitas − despesas − aplicado + resgatado. */
    public BigDecimal movimento() {
        return receitas.subtract(despesas).subtract(investimentos).add(resgatados);
    }

    /** Saldo inicial + movimento. */
    public BigDecimal saldo() {
        return saldoInicial.add(movimento());
    }

    /** Aplicado que ainda não voltou: aplicado − resgatado (o que está guardado em investimentos). */
    public BigDecimal investidoLiquido() {
        return investimentos.subtract(resgatados);
    }

    public String getNome() { return conta.getNome(); }
}
