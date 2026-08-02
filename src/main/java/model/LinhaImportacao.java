package model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class LinhaImportacao {
    private final StringProperty titulo          = new SimpleStringProperty();
    private final DoubleProperty valor           = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDate> data = new SimpleObjectProperty<>();
    private final ObjectProperty<Categoria> categoria = new SimpleObjectProperty<>();
    private final StringProperty detalhe         = new SimpleStringProperty();
    private final BooleanProperty importar        = new SimpleBooleanProperty(true);
    private final BooleanProperty mapeamentoNovo  = new SimpleBooleanProperty(false);

    public LinhaImportacao(String titulo, double valor, LocalDate data) {
        this.titulo.set(titulo);
        this.valor.set(valor);
        this.data.set(data);
        this.detalhe.set(titulo);
    }

    public StringProperty tituloProperty()           { return titulo; }
    public DoubleProperty valorProperty()             { return valor; }
    public ObjectProperty<LocalDate> dataProperty()   { return data; }
    public ObjectProperty<Categoria> categoriaProperty() { return categoria; }
    public StringProperty detalheProperty()           { return detalhe; }
    public BooleanProperty importarProperty()         { return importar; }
    public BooleanProperty mapeamentoNovoProperty()   { return mapeamentoNovo; }

    public String getTitulo()         { return titulo.get(); }
    public double getValor()          { return valor.get(); }
    public LocalDate getData()        { return data.get(); }
    public Categoria getCategoria()   { return categoria.get(); }
    public String getDetalhe()        { return detalhe.get(); }
    public boolean isImportar()       { return importar.get(); }
    public boolean isMapeamentoNovo() { return mapeamentoNovo.get(); }

    public void setCategoria(Categoria c)     { categoria.set(c); }
    public void setDetalhe(String d)          { detalhe.set(d); }
    public void setMapeamentoNovo(boolean b)  { mapeamentoNovo.set(b); }
}