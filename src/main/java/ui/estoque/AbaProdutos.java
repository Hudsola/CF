package ui.estoque;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import model.Produto;
import model.UnidadeMedida;
import service.Estoque;
import ui.components.AbaCrud;
import ui.components.Ui;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Cadastro de produtos do estoque, com busca e ajuste rápido da quantidade do produto selecionado
 * (entrada de uma compra, saída por consumo).
 */
class AbaProdutos extends AbaCrud<Produto> {

    static final String PROMPT_CODIGO = "Automático (ex: P0001)";
    static final String PROMPT_DESCRICAO = "Ex: Leite integral 1L, Contra filé";
    static final String PROMPT_QUANTIDADE = "Ex: 12 ou 1,028";
    static final String PROMPT_BUSCA = "Buscar por código ou descrição";
    static final String PROMPT_AJUSTE = "Quantidade";

    private final Estoque estoque;

    private final TextField tfCodigo = Ui.campo(PROMPT_CODIGO);
    private final TextField tfDescricao = Ui.campo(PROMPT_DESCRICAO);
    private final TextField tfQuantidade = Ui.campo(PROMPT_QUANTIDADE);
    private final ComboBox<UnidadeMedida> cbUnidade = new ComboBox<>();
    private final TextField tfBusca = Ui.campo(PROMPT_BUSCA);
    private final TextField tfAjuste = Ui.campo(PROMPT_AJUSTE);
    private final Label lblResumo = new Label();

    AbaProdutos(Estoque estoque) {
        this.estoque = estoque;
    }

    @Override protected String nome() { return "Produto"; }

    @Override
    protected void montarFormulario(GridPane form) {
        cbUnidade.getItems().setAll(UnidadeMedida.values());
        cbUnidade.setValue(UnidadeMedida.UN);
        Label ajuda = new Label("O código é do MyCareer: o mesmo produto comprado em lojas diferentes fica com um código só. "
                + "Deixe em branco para usar o próximo automático.");
        ajuda.getStyleClass().add("empty-label");
        ajuda.setWrapText(true);
        ajuda.setMaxWidth(420);
        ajuda.setMinHeight(Region.USE_PREF_SIZE);   // quebra em linhas em vez de cortar com "…"
        form.addRow(0, Ui.label("Código:"), tfCodigo, ajuda);
        form.addRow(1, Ui.label("Descrição:"), tfDescricao);
        form.addRow(2, Ui.label("Quantidade:"), new HBox(8, tfQuantidade, cbUnidade));
    }

    @Override
    protected void configurarColunas(TableView<Produto> t) {
        t.getColumns().add(Ui.colunaTexto("Código", Produto::codigo, 130));
        t.getColumns().add(Ui.colunaTexto("Descrição", Produto::descricao, 320));
        TableColumn<Produto, String> qtd = Ui.colunaTexto("Quantidade", Produto::quantidadeFormatada, 130);
        qtd.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String texto, boolean vazio) {
                super.updateItem(texto, vazio);
                setText(vazio ? null : texto);
                setAlignment(Pos.CENTER_RIGHT);
                Produto p = vazio || getTableRow() == null ? null : getTableRow().getItem();
                setStyle(p != null && p.semEstoque() ? "-fx-text-fill: " + Ui.COR_DESPESA + ";" : "");
            }
        });
        t.getColumns().add(qtd);
        t.getColumns().add(Ui.colunaTexto("Unidade", p -> p.unidade().toString(), 130));
    }

    /** Busca e ajuste de quantidade, entre o formulário e a tabela. */
    @Override
    protected Node barraFiltros() {
        tfBusca.textProperty().addListener((obs, antes, texto) -> recarregarTabela());
        tfBusca.setPrefWidth(280);
        tfAjuste.setPrefWidth(120);
        Button entrada = new Button("+ Entrada");
        entrada.getStyleClass().add("btn-primary");
        entrada.setTooltip(new Tooltip("Soma a quantidade ao produto selecionado (ex: uma compra)"));
        entrada.setOnAction(e -> ajustar(estoque::entrada, "Entrada"));
        Button saida = new Button("− Saída");
        saida.getStyleClass().add("btn-danger");
        saida.setTooltip(new Tooltip("Tira a quantidade do produto selecionado (ex: consumo)"));
        saida.setOnAction(e -> ajustar(estoque::saida, "Saída"));
        lblResumo.getStyleClass().add("form-label");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox barra = new HBox(10, tfBusca, lblResumo, espaco, Ui.label("Ajustar selecionado:"), tfAjuste, entrada, saida);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    private void ajustar(BiFunction<Integer, BigDecimal, BigDecimal> operacao, String nomeOperacao) {
        Produto p = selecionado();
        if (p == null) { erro("Selecione um produto na tabela."); return; }
        try {
            BigDecimal nova = operacao.apply(p.id(), Estoque.lerQuantidade(tfAjuste.getText()));
            tfAjuste.clear();
            recarregarTabela();
            tabela.getItems().stream().filter(x -> x.id() == p.id()).findFirst()
                    .ifPresent(x -> tabela.getSelectionModel().select(x));
            sucesso(nomeOperacao + " registrada: " + p.descricao() + " agora tem " + p.comQuantidade(nova).quantidadeFormatada() + ".");
        } catch (RuntimeException ex) {
            erro(ex.getMessage());
        }
    }

    @Override protected List<Produto> carregar() { return estoque.buscar(tfBusca.getText()); }

    @Override
    protected void aposRecarregar(List<Produto> itens) {
        long zerados = itens.stream().filter(Produto::semEstoque).count();
        lblResumo.setText(itens.size() + (itens.size() == 1 ? " produto" : " produtos")
                + (zerados > 0 ? " · " + zerados + " sem estoque" : ""));
    }

    @Override
    protected void preencherFormulario(Produto p) {
        tfCodigo.setText(p.codigo());
        tfDescricao.setText(p.descricao());
        tfQuantidade.setText(Produto.formatar(p.quantidade()));
        cbUnidade.setValue(p.unidade());
    }

    @Override
    protected void limparFormulario() {
        tfCodigo.clear();
        tfCodigo.setPromptText("Automático: " + estoque.proximoCodigo());
        tfDescricao.clear();
        tfQuantidade.clear();
        cbUnidade.setValue(UnidadeMedida.UN);
    }

    private Produto lerFormulario(int id) {
        BigDecimal qtd = tfQuantidade.getText().isBlank() ? BigDecimal.ZERO : Estoque.lerQuantidade(tfQuantidade.getText());
        return new Produto(id, tfCodigo.getText(), tfDescricao.getText(), qtd, cbUnidade.getValue());
    }

    @Override protected void salvarNovo() { estoque.cadastrar(lerFormulario(0)); }
    @Override protected void salvarAlteracao(Produto original) { estoque.atualizar(lerFormulario(original.id())); }
    @Override protected void excluir(Produto p) { estoque.excluir(p.id()); }
    @Override protected String descrever(Produto p) { return "o produto " + p.codigo() + " (" + p.descricao() + ")"; }
    @Override protected String descreverVarios(List<Produto> itens) { return "os " + itens.size() + " produtos selecionados"; }
}
