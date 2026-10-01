package ui.cadastros;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Conta;
import model.Dinheiro;
import model.Investimento;
import service.ControleFinanceiro;
import service.Conversor;
import ui.components.Ui;

import java.time.LocalDate;
import java.util.List;

class AbaInvestimentos extends AbaCrud<Investimento> {

    private final TextField tfTipo  = Ui.campo("Ex: Poupança, Tesouro Direto, CDB");
    private final TextField tfValor = Ui.campo("Ex: 500,00");
    private final ComboBox<Conta> cbConta = new ComboBox<>();
    private final DatePicker dpData = Ui.seletorData(LocalDate.now());

    AbaInvestimentos(ControleFinanceiro cf) { super(cf); }

    @Override protected String nome() { return "Investimento"; }

    @Override
    protected void montarFormulario(GridPane form) {
        cbConta.setPromptText("Selecione a conta");
        form.addRow(0, Ui.label("Tipo:"), tfTipo);
        form.addRow(1, Ui.label("Valor R$:"), tfValor);
        form.addRow(2, Ui.label("Conta:"), cbConta);
        form.addRow(3, Ui.label("Data:"), dpData);
    }

    @Override
    protected void configurarColunas(TableView<Investimento> t) {
        t.getColumns().add(Ui.colunaData("Data", Investimento::getData, 100));
        t.getColumns().add(Ui.colunaTexto("Tipo", Investimento::getTipo, 260));
        t.getColumns().add(Ui.colunaValor("Valor", Investimento::getValor, 120, Ui.COR_INVESTIMENTO));
        t.getColumns().add(Ui.colunaTexto("Conta", Investimento::getContaNome, 140));
    }

    @Override protected void recarregarOpcoes() { Ui.recarregar(cbConta, cf.getContas()); }
    @Override protected List<Investimento> carregar() { return cf.getInvestimentos(); }

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
    @Override protected String descrever(Investimento i) { return "o investimento \"" + i.getTipo() + "\""; }
}
