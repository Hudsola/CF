package ui.cadastros;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.*;
import model.LancamentoFixo.Tipo;
import service.ControleFinanceiro;
import service.Conversor;
import ui.components.Ui;

import java.time.LocalDate;
import java.util.List;

class AbaFixos extends AbaCrud<LancamentoFixo> {

    private final ComboBox<Tipo> cbTipo = new ComboBox<>();
    private final TextField tfDesc = Ui.campo("Ex: Condomínio, Salário");
    private final ComboBox<Categoria> cbCat = new ComboBox<>();
    private final TextField tfValor = Ui.campo("Ex: 850,00");
    private final ComboBox<Conta> cbConta = new ComboBox<>();
    private final Spinner<Integer> spDia = new Spinner<>(1, 31, 5);

    AbaFixos(ControleFinanceiro cf) { super(cf); }

    @Override protected String nome() { return "Fixo"; }

    @Override
    protected void montarFormulario(GridPane form) {
        cbTipo.getItems().addAll(Tipo.values());
        cbTipo.setOnAction(e -> {
            boolean despesa = cbTipo.getValue() == Tipo.DESPESA;
            cbCat.setDisable(!despesa);
            if (!despesa) cbCat.setValue(null);
        });
        cbCat.setPromptText("Categoria (só para despesa)");
        cbConta.setPromptText("Selecione a conta");
        form.addRow(0, Ui.label("Tipo:"), cbTipo);
        form.addRow(1, Ui.label("Descrição:"), tfDesc);
        form.addRow(2, Ui.label("Categoria:"), cbCat);
        form.addRow(3, Ui.label("Valor R$:"), tfValor);
        form.addRow(4, Ui.label("Conta:"), cbConta);
        form.addRow(5, Ui.label("Dia do mês:"), spDia);
    }

    @Override
    protected List<Button> botoesExtras() {
        Button btnAlternar = new Button("Ativar/Desativar");
        btnAlternar.getStyleClass().add("btn-secondary");
        btnAlternar.setOnAction(e -> {
            List<LancamentoFixo> sel = selecionados();
            if (sel.isEmpty()) { erro("Selecione um ou mais lançamentos fixos."); return; }
            try {
                for (LancamentoFixo lf : sel) cf.alternarAtivoFixo(lf.getId());
                recarregarTabela();
                sucesso(sel.size() == 1
                        ? "\"" + sel.get(0).getDescricao() + "\" " + (sel.get(0).isAtivo() ? "desativado." : "ativado.")
                        : sel.size() + " lançamentos fixos ativados/desativados.");
            } catch (Exception ex) { erro(ex.getMessage()); }
        });

        Button btnAplicar = new Button("Aplicar nos meses…");
        btnAplicar.getStyleClass().add("btn-info");
        btnAplicar.setOnAction(e -> abrirDialogoAplicar());
        return List.of(btnAlternar, btnAplicar);
    }

    private void abrirDialogoAplicar() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Aplicar lançamentos fixos");
        dialog.setHeaderText("Cria as receitas, despesas e investimentos dos fixos ativos no período.\n"
                + "Meses em que um fixo já foi aplicado são ignorados.");

        String mesAtual = Meses.nome(LocalDate.now());
        int anoAtual = LocalDate.now().getYear();
        ComboBox<String> cbMesIni = new ComboBox<>();
        cbMesIni.getItems().addAll(Meses.NOMES);
        cbMesIni.setValue(mesAtual);
        Spinner<Integer> spAnoIni = new Spinner<>(2000, 2100, anoAtual);
        ComboBox<String> cbMesFim = new ComboBox<>();
        cbMesFim.getItems().addAll(Meses.NOMES);
        cbMesFim.setValue(mesAtual);
        Spinner<Integer> spAnoFim = new Spinner<>(2000, 2100, anoAtual);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Mês inicial:"), cbMesIni, new Label("Ano:"), spAnoIni);
        grid.addRow(1, new Label("Mês final:"), cbMesFim, new Label("Ano:"), spAnoFim);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                int aplicados = cf.aplicarFixosIntervalo(cbMesIni.getValue(), spAnoIni.getValue(),
                        cbMesFim.getValue(), spAnoFim.getValue());
                sucesso(aplicados + " lançamento(s) criado(s) a partir dos fixos.");
            } catch (Exception ex) { erro(ex.getMessage()); }
        });
    }

    @Override
    protected void configurarColunas(TableView<LancamentoFixo> t) {
        t.getColumns().add(Ui.colunaTexto("Tipo", lf -> lf.getTipo().name(), 110));
        t.getColumns().add(Ui.colunaTexto("Descrição", LancamentoFixo::getDescricao, 200));
        t.getColumns().add(Ui.colunaTexto("Categoria", LancamentoFixo::getCategoriaNome, 120));
        t.getColumns().add(Ui.colunaValor("Valor", LancamentoFixo::getValor, 110, null));
        t.getColumns().add(Ui.colunaTexto("Conta", LancamentoFixo::getContaNome, 120));
        t.getColumns().add(Ui.colunaTexto("Dia", lf -> String.valueOf(lf.getDiaVencimento()), 50));
        t.getColumns().add(Ui.colunaTexto("Situação", lf -> lf.isAtivo() ? "Ativo" : "Inativo", 80));
    }

    @Override
    protected void recarregarOpcoes() {
        Ui.recarregar(cbCat, cf.getCategorias());
        Ui.recarregar(cbConta, cf.getContas());
    }

    @Override protected List<LancamentoFixo> carregar() { return cf.getLancamentosFixos(); }

    @Override
    protected void preencherFormulario(LancamentoFixo lf) {
        cbTipo.setValue(lf.getTipo());
        cbCat.setDisable(lf.getTipo() != Tipo.DESPESA);
        tfDesc.setText(lf.getDescricao());
        cbCat.setValue(lf.getTipo() == Tipo.DESPESA ? new Categoria(lf.getCategoriaId(), lf.getCategoriaNome()) : null);
        tfValor.setText(Dinheiro.formatarSemSimbolo(lf.getValor()));
        cbConta.setValue(new Conta(lf.getContaId(), lf.getContaNome()));
        spDia.getValueFactory().setValue(lf.getDiaVencimento());
    }

    @Override
    protected void limparFormulario() {
        cbTipo.setValue(Tipo.DESPESA);
        cbCat.setDisable(false);
        tfDesc.clear(); cbCat.setValue(null); tfValor.clear(); cbConta.setValue(null);
        spDia.getValueFactory().setValue(5);
    }

    private LancamentoFixo lerFormulario(int id, boolean ativo) {
        Tipo tipo = cbTipo.getValue();
        if (tipo == Tipo.DESPESA && cbCat.getValue() == null)
            throw new IllegalArgumentException("Selecione a categoria da despesa.");
        Conta conta = cbConta.getValue();
        if (conta == null) throw new IllegalArgumentException("Selecione uma conta.");
        int catId = tipo == Tipo.DESPESA ? cbCat.getValue().getId() : 0;
        return new LancamentoFixo(id, tipo, tfDesc.getText().trim(), catId, null,
                Conversor.parseValorPositivo(tfValor.getText()), conta.getId(), conta.getNome(),
                spDia.getValue(), ativo);
    }

    @Override protected void salvarNovo() { cf.salvarLancamentoFixo(lerFormulario(0, true)); }
    @Override protected void salvarAlteracao(LancamentoFixo o) { cf.atualizarLancamentoFixo(lerFormulario(o.getId(), o.isAtivo())); }
    @Override protected void excluir(LancamentoFixo lf) { cf.excluirLancamentoFixo(lf.getId()); }
    @Override protected String descrever(LancamentoFixo lf) { return "o lançamento fixo \"" + lf.getDescricao() + "\""; }
    @Override protected String descreverVarios(List<LancamentoFixo> itens) { return "os " + itens.size() + " lançamentos fixos selecionados"; }
}
