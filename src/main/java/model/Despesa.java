package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Despesa {
    private int id;
    private final int categoriaId;
    private String categoriaNome;
    private final String detalhamento;
    private final BigDecimal valor;
    private final int contaId;
    private String contaNome;
    private final LocalDate data;

    public Despesa(int categoriaId, String detalhamento, BigDecimal valor, int contaId, LocalDate data) {
        this.categoriaId = categoriaId; this.detalhamento = detalhamento;
        this.valor = Dinheiro.arredondar(valor); this.contaId = contaId; this.data = data;
    }

    public Despesa(int id, int categoriaId, String categoriaNome, String detalhamento,
                   BigDecimal valor, int contaId, String contaNome, LocalDate data) {
        this(categoriaId, detalhamento, valor, contaId, data);
        this.id = id; this.categoriaNome = categoriaNome; this.contaNome = contaNome;
    }

    public int getId()               { return id; }
    public int getCategoriaId()      { return categoriaId; }
    public String getCategoriaNome() { return categoriaNome != null ? categoriaNome : String.valueOf(categoriaId); }
    public String getDetalhamento()  { return detalhamento; }
    public BigDecimal getValor()     { return valor; }
    public int getContaId()          { return contaId; }
    public String getContaNome()     { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public LocalDate getData()       { return data; }
    public String getMes()           { return Meses.nome(data); }
    public int getAno()              { return data.getYear(); }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s | %s | %s",
                id, getCategoriaNome(), detalhamento, Dinheiro.formatar(valor), getContaNome(), data);
    }
}
