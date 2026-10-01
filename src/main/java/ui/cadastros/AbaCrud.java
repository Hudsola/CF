package ui.cadastros;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import service.ControleFinanceiro;
import ui.components.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Estrutura comum das abas de cadastro: formulário, botões Salvar/Cancelar, tabela com
 * Editar/Excluir (ou duplo clique para editar) e mensagens de status.
 *
 * As subclasses só descrevem os campos, as colunas e como ler/gravar o item.
 */
abstract class AbaCrud<T> {

    protected final ControleFinanceiro cf;
    protected final TableView<T> tabela;
    protected final Label lblMsg = new Label();

    private final Button btnSalvar = new Button();
    private final Button btnCancelar = new Button("Cancelar edição");
    private T emEdicao;
    private VBox raiz;

    protected AbaCrud(ControleFinanceiro cf) {
        this.cf = cf;
        this.tabela = Ui.tabela("Nenhum registro.");
    }

    // --- O que cada aba define ---

    /** Nome usado nos botões, ex: "Receita" → "Salvar Receita". */
    protected abstract String nome();
    protected abstract void montarFormulario(GridPane form);
    protected abstract void configurarColunas(TableView<T> tabela);
    protected abstract List<T> carregar();
    protected abstract void preencherFormulario(T item);
    protected abstract void limparFormulario();
    /** Lê o formulário e grava um item novo; lança exceção com mensagem amigável se algo estiver inválido. */
    protected abstract void salvarNovo();
    /** Lê o formulário e grava as alterações do item original. */
    protected abstract void salvarAlteracao(T original);
    protected abstract void excluir(T item);
    /** Ex: "a receita \"Salário\"" — usado na confirmação de exclusão. */
    protected abstract String descrever(T item);

    /** Recarrega listas dos combos (contas, categorias), que podem ter mudado em outra aba. */
    protected void recarregarOpcoes() {}
    protected Node barraFiltros() { return null; }
    protected List<Button> botoesExtras() { return List.of(); }
    /** Chamado depois de recarregar a tabela (ex: atualizar um total). */
    protected void aposRecarregar(List<T> itens) {}

    // --- Comportamento comum ---

    public Node getView() {
        if (raiz == null) raiz = montar();
        return raiz;
    }

    /** Chamado quando a aba é selecionada. */
    public void atualizar() {
        recarregarOpcoes();
        recarregarTabela();
    }

    protected void recarregarTabela() {
        List<T> itens = carregar();
        tabela.setItems(FXCollections.observableArrayList(itens));
        aposRecarregar(itens);
    }

    protected void sucesso(String msg) { Ui.mostrarMsg(lblMsg, "✔ " + msg, true); }
    protected void erro(String msg)    { Ui.mostrarMsg(lblMsg, "✖ " + msg, false); }

    protected T selecionado() {
        return tabela.getSelectionModel().getSelectedItem();
    }

    private VBox montar() {
        VBox aba = new VBox(12);
        aba.setPadding(new Insets(16));

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        montarFormulario(form);
        recarregarOpcoes();

        btnSalvar.getStyleClass().add("btn-primary");
        btnCancelar.getStyleClass().add("btn-secondary");
        btnSalvar.setOnAction(e -> salvar());
        btnCancelar.setOnAction(e -> sairDaEdicao());
        sairDaEdicao();

        Button btnEditar = new Button("Editar");
        Button btnExcluir = new Button("Excluir");
        btnEditar.getStyleClass().add("btn-info");
        btnExcluir.getStyleClass().add("btn-danger");
        btnEditar.setOnAction(e -> editarSelecionado());
        btnExcluir.setOnAction(e -> excluirSelecionado());

        configurarColunas(tabela);
        tabela.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) editarSelecionado();
        });

        HBox esquerda = new HBox(10, btnSalvar, btnCancelar, lblMsg);
        esquerda.setAlignment(Pos.CENTER_LEFT);
        List<Button> direitaBotoes = new ArrayList<>(List.of(btnEditar, btnExcluir));
        direitaBotoes.addAll(botoesExtras());
        HBox direita = new HBox(10, direitaBotoes.toArray(Node[]::new));
        direita.setAlignment(Pos.CENTER_RIGHT);
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox acoes = new HBox(10, esquerda, espaco, direita);
        acoes.setAlignment(Pos.CENTER_LEFT);

        aba.getChildren().addAll(form, acoes, new Separator());
        Node filtros = barraFiltros();
        if (filtros != null) aba.getChildren().add(filtros);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().add(tabela);

        recarregarTabela();
        return aba;
    }

    private void salvar() {
        try {
            if (emEdicao == null) {
                salvarNovo();
                sucesso("Salvo.");
            } else {
                salvarAlteracao(emEdicao);
                sucesso("Alterações salvas.");
            }
            sairDaEdicao(false);
            recarregarTabela();
        } catch (Exception ex) {
            erro(ex.getMessage());
        }
    }

    private void editarSelecionado() {
        T sel = selecionado();
        if (sel == null) { erro("Selecione um item na tabela."); return; }
        emEdicao = sel;
        recarregarOpcoes();
        preencherFormulario(sel);
        btnSalvar.setText("Atualizar " + nome());
        btnCancelar.setVisible(true);
        btnCancelar.setManaged(true);
        Ui.mostrarMsg(lblMsg, "Editando " + descrever(sel) + ".", true);
    }

    private void excluirSelecionado() {
        T sel = selecionado();
        if (sel == null) { erro("Selecione um item na tabela."); return; }
        if (!Ui.confirmarExclusao(descrever(sel))) return;
        try {
            excluir(sel);
            if (sel == emEdicao) sairDaEdicao(false);
            recarregarTabela();
            sucesso("Excluído.");
        } catch (Exception ex) {
            erro(ex.getMessage());
        }
    }

    private void sairDaEdicao() { sairDaEdicao(true); }

    private void sairDaEdicao(boolean limparMensagem) {
        emEdicao = null;
        limparFormulario();
        btnSalvar.setText("Salvar " + nome());
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
        if (limparMensagem) lblMsg.setText("");
    }
}
