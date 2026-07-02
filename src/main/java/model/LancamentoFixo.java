package model;

public class LancamentoFixo {

    public enum Tipo { RECEITA, DESPESA, INVESTIMENTO }

    private int id;
    private Tipo tipo;
    private String descricao;
    private int categoriaId;
    private String categoriaNome;
    private double valor;
    private int contaId;
    private String contaNome;
    private int diaVencimento;
    private boolean ativo;

    public LancamentoFixo(Tipo tipo, String descricao, int categoriaId,
                          double valor, int contaId, int diaVencimento) {
        this.tipo = tipo; this.descricao = descricao; this.categoriaId = categoriaId;
        this.valor = valor; this.contaId = contaId; this.diaVencimento = diaVencimento;
        this.ativo = true;
    }

    public LancamentoFixo(int id, Tipo tipo, String descricao, int categoriaId, String categoriaNome,
                          double valor, int contaId, String contaNome, int diaVencimento, boolean ativo) {
        this(tipo, descricao, categoriaId, valor, contaId, diaVencimento);
        this.id = id; this.categoriaNome = categoriaNome;
        this.contaNome = contaNome; this.ativo = ativo;
    }

    public int getId()               { return id; }
    public Tipo getTipo()            { return tipo; }
    public String getDescricao()     { return descricao; }
    public int getCategoriaId()      { return categoriaId; }
    public String getCategoriaNome() { return categoriaNome != null ? categoriaNome : ""; }
    public double getValor()         { return valor; }
    public int getContaId()          { return contaId; }
    public String getContaNome()     { return contaNome != null ? contaNome : String.valueOf(contaId); }
    public int getDiaVencimento()    { return diaVencimento; }
    public boolean isAtivo()         { return ativo; }

    @Override
    public String toString() {
        String cat = tipo == Tipo.DESPESA ? " | Cat: " + getCategoriaNome() : "";
        String sit = ativo ? "✔ ativo" : "✖ inativo";
        return String.format("[%d] %-12s | %-22s%s | R$ %9.2f | Conta: %-10s | Dia %2d | %s",
                id, tipo, descricao, cat, valor, getContaNome(), diaVencimento, sit);
    }
}
