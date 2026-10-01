package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Receita {
    private int id;
    private final String origem;
    private final BigDecimal valor;
    private final int contaId;
    private String contaNome;
    private final LocalDate data;

    public Receita(String origem, BigDecimal valor, int contaId, LocalDate data) {
        this.origem = origem; this.valor = Dinheiro.arredondar(valor);
        this.contaId = contaId; this.data = data;
    }

    public Receita(int id, String origem, BigDecimal valor, int contaId, String contaNome, LocalDate data) {
        this(origem, valor, contaId, data);
        this.id = id; this.contaNome = contaNome;
    }

    public int getId()           { return id; }
    public String getOrigem()    { return origem; }
    public BigDecimal getValor() { return valor; }
    public int getContaId()      { return contaId; }
    public String getContaNome() { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public LocalDate getData()   { return data; }
    public String getMes()       { return Meses.nome(data); }
    public int getAno()          { return data.getYear(); }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s | %s", id, origem, Dinheiro.formatar(valor), getContaNome(), data);
    }
}
