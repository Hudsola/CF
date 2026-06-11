package model;

import java.time.LocalDate;

public class Investimento {
    private int id;
    private String tipo;
    private double valor;
    private int contaId;
    private String contaNome;
    private LocalDate data;
    private String mes;
    private int ano;

    public Investimento(String tipo, double valor, int contaId, LocalDate data, String mes, int ano) {
        this.tipo    = tipo;
        this.valor   = valor;
        this.contaId = contaId;
        this.data    = data;
        this.mes     = mes;
        this.ano     = ano;
    }

    public Investimento(int id, String tipo, double valor, int contaId, String contaNome,
                        LocalDate data, String mes, int ano) {
        this(tipo, valor, contaId, data, mes, ano);
        this.id        = id;
        this.contaNome = contaNome;
    }

    public int getId()           { return id; }
    public String getTipo()      { return tipo; }
    public double getValor()     { return valor; }
    public int getContaId()      { return contaId; }
    public String getContaNome() { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public LocalDate getData()   { return data; }
    public String getMes()       { return mes; }
    public int getAno()          { return ano; }

    @Override
    public String toString() {
        return String.format("[%d] %-22s | R$ %9.2f | Conta: %-12s | %s (%s/%d)",
                id, tipo, valor, getContaNome(), data, mes, ano);
    }
}
