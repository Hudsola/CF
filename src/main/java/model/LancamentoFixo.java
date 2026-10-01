package model;

import java.math.BigDecimal;

public class LancamentoFixo {

    public enum Tipo { RECEITA, DESPESA, INVESTIMENTO }

    private int id;
    private final Tipo tipo;
    private final String descricao;
    private final int categoriaId;
    private String categoriaNome;
    private final BigDecimal valor;
    private final int contaId;
    private String contaNome;
    private final int diaVencimento;
    private boolean ativo;

    public LancamentoFixo(Tipo tipo, String descricao, int categoriaId,
                          BigDecimal valor, int contaId, int diaVencimento) {
        this.tipo = tipo; this.descricao = descricao; this.categoriaId = categoriaId;
        this.valor = Dinheiro.arredondar(valor); this.contaId = contaId; this.diaVencimento = diaVencimento;
        this.ativo = true;
    }

    public LancamentoFixo(int id, Tipo tipo, String descricao, int categoriaId, String categoriaNome,
                          BigDecimal valor, int contaId, String contaNome, int diaVencimento, boolean ativo) {
        this(tipo, descricao, categoriaId, valor, contaId, diaVencimento);
        this.id = id; this.categoriaNome = categoriaNome;
        this.contaNome = contaNome; this.ativo = ativo;
    }

    public int getId()               { return id; }
    public Tipo getTipo()            { return tipo; }
    public String getDescricao()     { return descricao; }
    public int getCategoriaId()      { return categoriaId; }
    public String getCategoriaNome() { return categoriaNome != null ? categoriaNome : ""; }
    public BigDecimal getValor()     { return valor; }
    public int getContaId()          { return contaId; }
    public String getContaNome()     { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public int getDiaVencimento()    { return diaVencimento; }
    public boolean isAtivo()         { return ativo; }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s | dia %d | %s",
                id, tipo, descricao, Dinheiro.formatar(valor), diaVencimento, ativo ? "ativo" : "inativo");
    }
}
