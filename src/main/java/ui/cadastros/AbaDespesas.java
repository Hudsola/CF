package ui.cadastros;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import model.*;
import service.ControleFinanceiro;
import service.Conversor;
import ui.components.Ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

class AbaDespesas extends AbaCrud<Despesa> {

    private final ComboBox<Categoria> cbCat = new ComboBox<>();
    private final TextField tfDetalhe = Ui.campo("Ex: Supermercado, Netflix");
    private final TextField tfValor   = Ui.campo("Ex: 350,00");
    private final ComboBox<Conta> cbConta = new ComboBox<>();
    private final DatePicker dpData = Ui.seletorData(LocalDate.now());

    private final ComboBox<Categoria> cbFiltroCategoria = new ComboBox<>();
    private final ComboBox<String> cbFiltroMes = new ComboBox<>();
    private final ComboBox<Integer> cbFiltroAno = new ComboBox<>();
    private final Label lblTotal = new Label();

    AbaDespesas(ControleFinanceiro cf) { super(cf); }

    @Override protected String nome() { return "Despesa"; }

    @Override
    protected void montarFormulario(GridPane form) {
        cbCat.setPromptText("Selecione a categoria");
        cbConta.setPromptText("Selecione a conta");
        form.addRow(0, Ui.label("Categoria:"), cbCat);
        form.addRow(1, Ui.label("Detalhamento:"), tfDetalhe);
        form.addRow(2, Ui.label("Valor R$:"), tfValor);
        form.addRow(3, Ui.label("Conta:"), cbConta);
        form.addRow(4, Ui.label("Data:"), dpData);
    }

    @Override
    protected Node barraFiltros() {
        cbFiltroCategoria.setPromptText("Todas");
        cbFiltroCategoria.getStyleClass().add("dark-combo");
        cbFiltroMes.getItems().add(Meses.TODOS);
        cbFiltroMes.getItems().addAll(Meses.NOMES);
        cbFiltroMes.setValue(Meses.TODOS);
        cbFiltroMes.getStyleClass().add("dark-combo");
        cbFiltroAno.getStyleClass().add("dark-combo");
        recarregarAnos();

        cbFiltroCategoria.setOnAction(e -> recarregarTabela());
        cbFiltroMes.setOnAction(e -> recarregarTabela());
        cbFiltroAno.setOnAction(e -> recarregarTabela());

        Button btnLimpar = new Button("Limpar filtros");
        btnLimpar.getStyleClass().add("btn-secondary");
        btnLimpar.setOnAction(e -> {
            cbFiltroCategoria.setValue(null);
            cbFiltroMes.setValue(Meses.TODOS);
            cbFiltroAno.setValue(LocalDate.now().getYear());
        });

        lblTotal.getStyleClass().add("form-label");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox filtros = new HBox(10,
                Ui.label("Categoria:"), cbFiltroCategoria,
                Ui.label("Mês:"), cbFiltroMes,
                Ui.label("Ano:"), cbFiltroAno,
                btnLimpar, espaco, lblTotal);
        filtros.setAlignment(Pos.CENTER_LEFT);
        return filtros;
    }

    @Override
    protected List<Button> botoesExtras() {
        Button btnImportar = new Button("Importar CSV");
        btnImportar.getStyleClass().add("btn-info");
        btnImportar.setOnAction(e -> new ImportacaoCsvDialog(cf, () -> {
            recarregarAnos();
            recarregarTabela();
        }).abrir((Stage) btnImportar.getScene().getWindow()));
        return List.of(btnImportar);
    }

    @Override
    protected void configurarColunas(TableView<Despesa> t) {
        t.getColumns().add(Ui.colunaData("Data", Despesa::getData, 100));
        t.getColumns().add(Ui.colunaTexto("Categoria", Despesa::getCategoriaNome, 130));
        t.getColumns().add(Ui.colunaTexto("Detalhe", Despesa::getDetalhamento, 240));
        t.getColumns().add(Ui.colunaValor("Valor", Despesa::getValor, 120, Ui.COR_DESPESA));
        t.getColumns().add(Ui.colunaTexto("Conta", Despesa::getContaNome, 130));
    }

    @Override
    protected void recarregarOpcoes() {
        List<Categoria> categorias = cf.getCategorias();
        Ui.recarregar(cbCat, categorias);
        Ui.recarregar(cbFiltroCategoria, categorias);
        Ui.recarregar(cbConta, cf.getContas());
    }

    private void recarregarAnos() {
        TreeSet<Integer> anos = new TreeSet<>(cf.anosDisponiveis());
        anos.add(LocalDate.now().getYear());
        Integer atual = cbFiltroAno.getValue();
        cbFiltroAno.getItems().setAll(new ArrayList<>(anos.descendingSet()));
        cbFiltroAno.setValue(atual != null && anos.contains(atual) ? atual : LocalDate.now().getYear());
    }

    @Override
    protected List<Despesa> carregar() {
        Categoria cat = cbFiltroCategoria.getValue();
        int ano = cbFiltroAno.getValue() != null ? cbFiltroAno.getValue() : LocalDate.now().getYear();
        List<Despesa> despesas = new ArrayList<>(
                cf.pesquisarDespesas(cat == null ? null : cat.getId(), ano, cbFiltroMes.getValue()));
        java.util.Collections.reverse(despesas);   // mais recentes primeiro
        return despesas;
    }

    @Override
    protected void aposRecarregar(List<Despesa> itens) {
        if (!ouvindoSelecao) {
            tabela.getSelectionModel().getSelectedItems().addListener(
                    (javafx.collections.ListChangeListener<Despesa>) c -> atualizarTotais());
            ouvindoSelecao = true;
        }
        atualizarTotais();
    }

    private boolean ouvindoSelecao = false;

    /** "53 despesa(s) — total R$ X" e, havendo seleção, "| 3 selecionada(s) — R$ Y". */
    private void atualizarTotais() {
        List<Despesa> itens = tabela.getItems();
        String texto = itens.size() + " despesa(s) — total " + Dinheiro.formatar(somar(itens));
        List<Despesa> sel = selecionados();
        if (!sel.isEmpty()) texto += "   |   " + sel.size() + " selecionada(s) — " + Dinheiro.formatar(somar(sel));
        lblTotal.setText(texto);
    }

    private static BigDecimal somar(List<Despesa> itens) {
        return itens.stream().map(Despesa::getValor).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    @Override
    protected void preencherFormulario(Despesa d) {
        cbCat.setValue(new Categoria(d.getCategoriaId(), d.getCategoriaNome()));
        tfDetalhe.setText(d.getDetalhamento());
        tfValor.setText(Dinheiro.formatarSemSimbolo(d.getValor()));
        cbConta.setValue(new Conta(d.getContaId(), d.getContaNome()));
        dpData.setValue(d.getData());
    }

    @Override
    protected void limparFormulario() {
        cbCat.setValue(null); tfDetalhe.clear(); tfValor.clear();
        cbConta.setValue(null); dpData.setValue(LocalDate.now());
    }

    private Despesa lerFormulario(int id) {
        Categoria cat = cbCat.getValue();
        if (cat == null) throw new IllegalArgumentException("Selecione uma categoria.");
        Conta conta = cbConta.getValue();
        if (conta == null) throw new IllegalArgumentException("Selecione uma conta.");
        return new Despesa(id, cat.getId(), cat.getNome(), tfDetalhe.getText().trim(),
                Conversor.parseValorPositivo(tfValor.getText()), conta.getId(), conta.getNome(), dpData.getValue());
    }

    @Override protected void salvarNovo() { cf.salvarDespesa(lerFormulario(0)); recarregarAnos(); }
    @Override protected void salvarAlteracao(Despesa original) { cf.atualizarDespesa(lerFormulario(original.getId())); recarregarAnos(); }
    @Override protected void excluir(Despesa d) { cf.excluirDespesa(d.getId()); }
    @Override protected String descrever(Despesa d) { return "a despesa \"" + d.getDetalhamento() + "\""; }
    @Override protected String descreverVarios(List<Despesa> itens) {
        return "as " + itens.size() + " despesas selecionadas (total "
                + Dinheiro.formatar(itens.stream().map(Despesa::getValor).reduce(Dinheiro.ZERO, BigDecimal::add)) + ")";
    }
}
