package model;

import java.time.LocalDate;

public class Despesa {
    private int id;
    private int categoriaId;
    private String categoriaNome;
    private String detalhamento;
    private double valor;
    private int contaId;
    private String contaNome;
    private LocalDate data;
    private String mes;
    private int ano;

    public Despesa(int categoriaId, String detalhamento, double valor,
                   int contaId, LocalDate data, String mes, int ano) {
        this.categoriaId  = categoriaId;
        this.detalhamento = detalhamento;
        this.valor        = valor;
        this.contaId      = contaId;
        this.data         = data;
        this.mes          = mes;
        this.ano          = ano;
    }

    public Despesa(int id, int categoriaId, String categoriaNome, String detalhamento,
                   double valor, int contaId, String contaNome,
                   LocalDate data, String mes, int ano) {
        this(categoriaId, detalhamento, valor, contaId, data, mes, ano);
        this.id            = id;
        this.categoriaNome = categoriaNome;
        this.contaNome     = contaNome;
    }

    public int getId()               { return id; }
    public int getCategoriaId()      { return categoriaId; }
    public String getCategoriaNome() { return categoriaNome != null ? categoriaNome : String.valueOf(categoriaId); }
    public String getTipoGasto()     { return getCategoriaNome(); }
    public String getDetalhamento()  { return detalhamento; }
    public double getValor()         { return valor; }
    public int getContaId()          { return contaId; }
    public String getContaNome()     { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public LocalDate getData()       { return data; }
    public String getMes()           { return mes; }
    public int getAno()              { return ano; }

    @Override
    public String toString() {
        return String.format("[%d] %-15s | %-22s | R$ %9.2f | Conta: %-12s | %s (%s/%d)",
                id, getCategoriaNome(), detalhamento, valor, getContaNome(), data, mes, ano);
    }
}
