package ui.financas.cadastros;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Conta;
import model.Dinheiro;
import model.Investimento;
import service.ControleFinanceiro;
import service.Conversor;
import ui.components.Ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Investimentos: a aplicação tira o valor do saldo disponível e o deixa "guardado" (não é despesa). No resgate
 * (sempre total) o aplicado volta para o disponível e só a diferença para o valor sacado vira lançamento:
 * lucro como receita ou prejuízo como despesa.
 */
class AbaInvestimentos extends AbaFinancas<Investimento> {

    static final String PROMPT_SACADO = "Valor sacado, ex: 5.000,00";

    private final TextField tfTipo  = Ui.campo("Ex: Poupança, Tesouro Direto, CDB");
    private final TextField tfValor = Ui.campo("Ex: 500,00");
    private final ComboBox<Conta> cbConta = new ComboBox<>();
    private final DatePicker dpData = Ui.seletorData(LocalDate.now());

    private final DatePicker dpResgate = Ui.seletorData(LocalDate.now());
    private final TextField tfSacado = Ui.campo(PROMPT_SACADO);
    private final Label lblGuardado = new Label();

    AbaInvestimentos(ControleFinanceiro cf) { super(cf); }

    @Override protected String nome() { return "Investimento"; }

    @Override
    protected void montarFormulario(GridPane form) {
        cbConta.setPromptText("Selecione a conta");
        Label ajuda = new Label("A aplicação sai do saldo disponível e fica guardada (não é despesa). "
                + "No resgate, o aplicado volta e só o lucro (receita) ou o prejuízo (despesa) é lançado.");
        ajuda.getStyleClass().add("empty-label");
        ajuda.setWrapText(true);
        ajuda.setMaxWidth(420);
        ajuda.setMinHeight(Region.USE_PREF_SIZE);
        form.addRow(0, Ui.label("Tipo:"), tfTipo, ajuda);
        form.addRow(1, Ui.label("Valor aplicado R$:"), tfValor);
        form.addRow(2, Ui.label("Conta:"), cbConta);
        form.addRow(3, Ui.label("Data:"), dpData);
    }

    @Override
    protected void configurarColunas(TableView<Investimento> t) {
        t.getColumns().add(Ui.colunaData("Aplicação", Investimento::getData, 100));
        t.getColumns().add(Ui.colunaTexto("Tipo", Investimento::getTipo, 200));
        t.getColumns().add(Ui.colunaValor("Aplicado", Investimento::getValor, 120, Ui.COR_INVESTIMENTO));
        t.getColumns().add(Ui.colunaTexto("Conta", Investimento::getContaNome, 120));
        t.getColumns().add(Ui.colunaTexto("Situação", AbaInvestimentos::situacao, 160));
        t.getColumns().add(Ui.colunaValor("Sacado", Investimento::getValorResgatado, 120, null));
        t.getColumns().add(Ui.colunaValor("Lucro / prejuízo", i -> i.isResgatado() ? i.getResultado() : null, 130, null));
    }

    static String situacao(Investimento i) {
        return i.isResgatado() ? "Resgatado em " + Ui.DATA_BR.format(i.getDataResgate()) : "Guardado";
    }

    /** Resgate do investimento selecionado, entre o formulário e a tabela. */
    @Override
    protected Node barraFiltros() {
        tfSacado.setPrefWidth(190);
        dpResgate.setPrefWidth(140);
        Button resgatar = new Button("Resgatar");
        resgatar.getStyleClass().add("btn-primary");
        resgatar.setTooltip(new Tooltip("Resgata o investimento selecionado inteiro"));
        resgatar.setOnAction(e -> resgatar());
        Button desfazer = new Button("Desfazer resgate");
        desfazer.getStyleClass().add("btn-secondary");
        desfazer.setTooltip(new Tooltip("Volta o investimento para guardado e apaga o lucro ou prejuízo lançado"));
        desfazer.setOnAction(e -> desfazerResgate());
        lblGuardado.getStyleClass().add("form-label");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linha = new HBox(10, lblGuardado, espaco, Ui.label("Resgate do selecionado:"), dpResgate, tfSacado, resgatar, desfazer);
        linha.setAlignment(Pos.CENTER_LEFT);
        return new VBox(linha);
    }

    private void resgatar() {
        Investimento i = selecionado();
        if (i == null) { erro("Selecione um investimento na tabela."); return; }
        try {
            BigDecimal resultado = cf.resgatarInvestimento(i.getId(), dpResgate.getValue(), Conversor.parseValor(tfSacado.getText()));
            tfSacado.clear();
            recarregarTabela();
            sucesso("Resgate registrado: " + Dinheiro.formatar(i.getValor()) + " voltaram ao disponível"
                    + (resultado.signum() > 0 ? " e " + Dinheiro.formatar(resultado) + " de lucro entraram como receita."
                    : resultado.signum() < 0 ? "; o prejuízo de " + Dinheiro.formatar(resultado.negate()) + " foi lançado como despesa."
                    : ", sem lucro nem prejuízo."));
        } catch (RuntimeException ex) {
            erro(ex.getMessage());
        }
    }

    private void desfazerResgate() {
        Investimento i = selecionado();
        if (i == null) { erro("Selecione um investimento na tabela."); return; }
        try {
            cf.desfazerResgate(i.getId());
            recarregarTabela();
            sucesso("Resgate desfeito: \"" + i.getTipo() + "\" voltou a ficar guardado.");
        } catch (RuntimeException ex) {
            erro(ex.getMessage());
        }
    }

    @Override protected void recarregarOpcoes() { Ui.recarregar(cbConta, cf.getContas()); }
    @Override protected List<Investimento> carregar() { return cf.getInvestimentos(); }

    @Override
    protected void aposRecarregar(List<Investimento> itens) {
        lblGuardado.setText("Guardado em investimentos: " + Dinheiro.formatar(cf.guardadoEmInvestimentos()));
    }

    @Override
    protected void preencherFormulario(Investimento i) {
        tfTipo.setText(i.getTipo());
        tfValor.setText(Dinheiro.formatarSemSimbolo(i.getValor()));
        cbConta.setValue(new Conta(i.getContaId(), i.getContaNome()));
        dpData.setValue(i.getData());
    }

    @Override
    protected void limparFormulario() {
        tfTipo.clear(); tfValor.clear();
        cbConta.setValue(null); dpData.setValue(LocalDate.now());
    }

    private Investimento lerFormulario(int id) {
        Conta conta = cbConta.getValue();
        if (conta == null) throw new IllegalArgumentException("Selecione uma conta.");
        return new Investimento(id, tfTipo.getText().trim(), Conversor.parseValorPositivo(tfValor.getText()),
                conta.getId(), conta.getNome(), dpData.getValue());
    }

    @Override protected void salvarNovo() { cf.salvarInvestimento(lerFormulario(0)); }
    @Override protected void salvarAlteracao(Investimento original) { cf.atualizarInvestimento(lerFormulario(original.getId())); }
    @Override protected void excluir(Investimento i) { cf.excluirInvestimento(i.getId()); }
    @Override protected String descrever(Investimento i) {
        return "o investimento \"" + i.getTipo() + "\"" + (i.isResgatado() ? " (e o lucro ou prejuízo lançado no resgate)" : "");
    }
    @Override protected String descreverVarios(List<Investimento> itens) {
        return "os " + itens.size() + " investimentos selecionados (total "
                + Dinheiro.formatar(itens.stream().map(Investimento::getValor).reduce(Dinheiro.ZERO, BigDecimal::add)) + ")";
    }
}
