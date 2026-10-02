package ui.cadastros;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Conta;
import model.Dinheiro;
import model.Receita;
import service.ControleFinanceiro;
import service.Conversor;
import ui.components.Ui;

import java.time.LocalDate;
import java.util.List;

class AbaReceitas extends AbaCrud<Receita> {

    private final TextField tfOrigem = Ui.campo("Ex: Salário, Freelance");
    private final TextField tfValor  = Ui.campo("Ex: 1.500,00");
    private final ComboBox<Conta> cbConta = new ComboBox<>();
    private final DatePicker dpData = Ui.seletorData(LocalDate.now());

    AbaReceitas(ControleFinanceiro cf) { super(cf); }

    @Override protected String nome() { return "Receita"; }

    @Override
    protected void montarFormulario(GridPane form) {
        cbConta.setPromptText("Selecione a conta");
        form.addRow(0, Ui.label("Origem/Descrição:"), tfOrigem);
        form.addRow(1, Ui.label("Valor R$:"), tfValor);
        form.addRow(2, Ui.label("Conta:"), cbConta);
        form.addRow(3, Ui.label("Data:"), dpData);
    }

    @Override
    protected void configurarColunas(TableView<Receita> t) {
        t.getColumns().add(Ui.colunaData("Data", Receita::getData, 100));
        t.getColumns().add(Ui.colunaTexto("Origem", Receita::getOrigem, 260));
        t.getColumns().add(Ui.colunaValor("Valor", Receita::getValor, 120, Ui.COR_RECEITA));
        t.getColumns().add(Ui.colunaTexto("Conta", Receita::getContaNome, 140));
    }

    @Override protected void recarregarOpcoes() { Ui.recarregar(cbConta, cf.getContas()); }
    @Override protected List<Receita> carregar() { return cf.getReceitas(); }

    @Override
    protected void preencherFormulario(Receita r) {
        tfOrigem.setText(r.getOrigem());
        tfValor.setText(Dinheiro.formatarSemSimbolo(r.getValor()));
        cbConta.setValue(new Conta(r.getContaId(), r.getContaNome()));
        dpData.setValue(r.getData());
    }

    @Override
    protected void limparFormulario() {
        tfOrigem.clear(); tfValor.clear();
        cbConta.setValue(null); dpData.setValue(LocalDate.now());
    }

    private Receita lerFormulario(int id) {
        Conta conta = cbConta.getValue();
        if (conta == null) throw new IllegalArgumentException("Selecione uma conta.");
        return new Receita(id, tfOrigem.getText().trim(), Conversor.parseValorPositivo(tfValor.getText()),
                conta.getId(), conta.getNome(), dpData.getValue());
    }

    @Override protected void salvarNovo() { cf.salvarReceita(lerFormulario(0)); }
    @Override protected void salvarAlteracao(Receita original) { cf.atualizarReceita(lerFormulario(original.getId())); }
    @Override protected void excluir(Receita r) { cf.excluirReceita(r.getId()); }
    @Override protected String descrever(Receita r) { return "a receita \"" + r.getOrigem() + "\""; }
    @Override protected String descreverVarios(List<Receita> itens) {
        return "as " + itens.size() + " receitas selecionadas (total "
                + Dinheiro.formatar(itens.stream().map(Receita::getValor).reduce(Dinheiro.ZERO, java.math.BigDecimal::add)) + ")";
    }
}
