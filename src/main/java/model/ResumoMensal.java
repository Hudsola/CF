package model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Totais de um mês para a tabela "Resumo Mês a Mês". */
public class ResumoMensal {
    private final String mes;
    private BigDecimal receita = Dinheiro.ZERO;
    private BigDecimal investimentos = Dinheiro.ZERO;
    private BigDecimal despesaTotal = Dinheiro.ZERO;
    private BigDecimal saldo = Dinheiro.ZERO;
    private BigDecimal saldoAcumulado = Dinheiro.ZERO;
    /** Despesas do mês por nome de categoria (todas as categorias, inclusive as criadas pelo usuário). */
    private final Map<String, BigDecimal> despesasPorCategoria = new LinkedHashMap<>();

    public ResumoMensal(String mes) { this.mes = mes; }

    public String getMes()                      { return mes; }
    public BigDecimal getReceita()              { return receita; }
    public void setReceita(BigDecimal v)        { receita = v; }
    public BigDecimal getInvestimentos()        { return investimentos; }
    public void setInvestimentos(BigDecimal v)  { investimentos = v; }
    public BigDecimal getDespesaTotal()         { return despesaTotal; }
    public void setDespesaTotal(BigDecimal v)   { despesaTotal = v; }
    public BigDecimal getSaldo()                { return saldo; }
    public void setSaldo(BigDecimal v)          { saldo = v; }
    public BigDecimal getSaldoAcumulado()       { return saldoAcumulado; }
    public void setSaldoAcumulado(BigDecimal v) { saldoAcumulado = v; }
    public Map<String, BigDecimal> getDespesasPorCategoria() { return despesasPorCategoria; }

    public BigDecimal getDespesaCategoria(String categoria) {
        return despesasPorCategoria.getOrDefault(categoria, Dinheiro.ZERO);
    }

    public boolean temMovimento() {
        return receita.signum() != 0 || investimentos.signum() != 0 || despesaTotal.signum() != 0;
    }
}
