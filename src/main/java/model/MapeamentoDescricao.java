package model;

public class MapeamentoDescricao {
    private int id;
    private String padrao;
    private int categoriaId;
    private String categoriaNome;
    private String detalhe;

    public MapeamentoDescricao(String padrao, int categoriaId, String detalhe) {
        this.padrao = padrao;
        this.categoriaId = categoriaId;
        this.detalhe = detalhe;
    }

    public MapeamentoDescricao(int id, String padrao, int categoriaId,
                               String categoriaNome, String detalhe) {
        this(padrao, categoriaId, detalhe);
        this.id = id;
        this.categoriaNome = categoriaNome;
    }

    public int getId()               { return id; }
    public String getPadrao()        { return padrao; }
    public int getCategoriaId()      { return categoriaId; }
    public String getCategoriaNome() { return categoriaNome != null ? categoriaNome : ""; }
    public String getDetalhe()       { return detalhe; }
}