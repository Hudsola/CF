package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dinheiro aplicado: sai do saldo disponível e fica "guardado" até o resgate (não é despesa).
 *
 * O resgate é sempre total: o valor aplicado volta para o disponível (devolução, não é renda) e só a diferença
 * para o valor sacado vira lançamento — lucro como receita ou prejuízo como despesa (ver
 * {@code ControleFinanceiro.resgatarInvestimento}).
 */
public class Investimento {
    private int id;
    private final String tipo;
    private final BigDecimal valor;
    private final int contaId;
    private String contaNome;
    private final LocalDate data;

    private LocalDate dataResgate;
    private BigDecimal valorResgatado;
    private Integer receitaResultadoId;
    private Integer despesaResultadoId;

    public Investimento(String tipo, BigDecimal valor, int contaId, LocalDate data) {
        this.tipo = tipo; this.valor = Dinheiro.arredondar(valor);
        this.contaId = contaId; this.data = data;
    }

    public Investimento(int id, String tipo, BigDecimal valor, int contaId, String contaNome, LocalDate data) {
        this(tipo, valor, contaId, data);
        this.id = id; this.contaNome = contaNome;
    }

    /** Preenche os dados do resgate (lidos do banco). */
    public Investimento comResgate(LocalDate dataResgate, BigDecimal valorResgatado,
                                   Integer receitaResultadoId, Integer despesaResultadoId) {
        this.dataResgate = dataResgate;
        this.valorResgatado = valorResgatado == null ? null : Dinheiro.arredondar(valorResgatado);
        this.receitaResultadoId = receitaResultadoId;
        this.despesaResultadoId = despesaResultadoId;
        return this;
    }

    public int getId()           { return id; }
    public String getTipo()      { return tipo; }
    /** Valor aplicado. */
    public BigDecimal getValor() { return valor; }
    public int getContaId()      { return contaId; }
    public String getContaNome() { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public LocalDate getData()   { return data; }
    public String getMes()       { return Meses.nome(data); }
    public int getAno()          { return data.getYear(); }

    public LocalDate getDataResgate()       { return dataResgate; }
    public BigDecimal getValorResgatado()   { return valorResgatado; }
    public Integer getReceitaResultadoId()  { return receitaResultadoId; }
    public Integer getDespesaResultadoId()  { return despesaResultadoId; }

    public boolean isResgatado() { return dataResgate != null; }

    /** Lucro (positivo) ou prejuízo (negativo) do resgate; zero enquanto não foi resgatado. */
    public BigDecimal getResultado() {
        return isResgatado() ? valorResgatado.subtract(valor) : Dinheiro.ZERO;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s | %s", id, tipo, Dinheiro.formatar(valor), getContaNome(), data);
    }
}
