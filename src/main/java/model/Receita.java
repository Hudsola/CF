package model;

import java.time.LocalDate;

public class Receita {
    private int id;
    private String origem;
    private double valor;
    private int contaId;
    private String contaNome;
    private LocalDate data;
    private String mes;
    private int ano;

    public Receita(String origem, double valor, int contaId, LocalDate data, String mes, int ano) {
        this.origem = origem; this.valor = valor; this.contaId = contaId;
        this.data = data; this.mes = mes; this.ano = ano;
    }

    public Receita(int id, String origem, double valor, int contaId, String contaNome,
                   LocalDate data, String mes, int ano) {
        this(origem, valor, contaId, data, mes, ano);
        this.id = id; this.contaNome = contaNome;
    }

    public int getId()           { return id; }
    public String getOrigem()    { return origem; }
    public double getValor()     { return valor; }
    public int getContaId()      { return contaId; }
    public String getContaNome() { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public LocalDate getData()   { return data; }
    public String getMes()       { return mes; }
    public int getAno()          { return ano; }

    @Override
    public String toString() {
        return String.format("[%d] %-25s | R$ %9.2f | Conta: %-12s | %s (%s/%d)",
                id, origem, valor, getContaNome(), data, mes, ano);
    }
}
