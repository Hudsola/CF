package model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LinhaImportacao {
    private final StringProperty titulo          = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> valor = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> data = new SimpleObjectProperty<>();
    private final ObjectProperty<Categoria> categoria = new SimpleObjectProperty<>();
    private final StringProperty detalhe         = new SimpleStringProperty();
    private final BooleanProperty importar        = new SimpleBooleanProperty(true);
    private final BooleanProperty mapeamentoNovo  = new SimpleBooleanProperty(false);
    private final StringProperty observacao      = new SimpleStringProperty("");

    /** Mapeamento encontrado na leitura (null quando a linha não tinha mapeamento). */
    private MapeamentoDescricao mapeamentoOriginal;

    public LinhaImportacao(String titulo, BigDecimal valor, LocalDate data) {
        this.titulo.set(titulo);
        this.valor.set(Dinheiro.arredondar(valor));
        this.data.set(data);
        this.detalhe.set(titulo);
    }

    public StringProperty tituloProperty()           { return titulo; }
    public ObjectProperty<BigDecimal> valorProperty() { return valor; }
    public ObjectProperty<LocalDate> dataProperty()   { return data; }
    public ObjectProperty<Categoria> categoriaProperty() { return categoria; }
    public StringProperty detalheProperty()           { return detalhe; }
    public BooleanProperty importarProperty()         { return importar; }
    public BooleanProperty mapeamentoNovoProperty()   { return mapeamentoNovo; }
    public StringProperty observacaoProperty()        { return observacao; }

    public String getTitulo()         { return titulo.get(); }
    public BigDecimal getValor()      { return valor.get(); }
    public LocalDate getData()        { return data.get(); }
    public Categoria getCategoria()   { return categoria.get(); }
    public String getDetalhe()        { return detalhe.get(); }
    public boolean isImportar()       { return importar.get(); }
    public boolean isMapeamentoNovo() { return mapeamentoNovo.get(); }
    public String getObservacao()     { return observacao.get(); }
    public MapeamentoDescricao getMapeamentoOriginal() { return mapeamentoOriginal; }

    public void setCategoria(Categoria c)     { categoria.set(c); }
    public void setDetalhe(String d)          { detalhe.set(d); }
    public void setMapeamentoNovo(boolean b)  { mapeamentoNovo.set(b); }
    public void setData(LocalDate d)  { data.set(d); }
    public void setTitulo(String t)   { titulo.set(t); }
    public void setImportar(boolean b)        { importar.set(b); }
    public void setObservacao(String o)       { observacao.set(o); }
    public void setMapeamentoOriginal(MapeamentoDescricao m) { mapeamentoOriginal = m; }
}
