package ui.cadastros;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import model.Conta;
import model.Dinheiro;
import model.SaldoConta;
import service.ControleFinanceiro;
import service.Conversor;
import ui.components.Ui;

import java.math.BigDecimal;
import java.util.List;

/** Contas com saldo inicial e saldo atual (saldo inicial + receitas − despesas − investimentos). */
class AbaContas extends AbaCrud<SaldoConta> {

    private final TextField tfNome = Ui.campo("Nome da conta (ex: Nubank, Itaú)");
    private final TextField tfSaldoInicial = Ui.campo("Opcional. Ex: 1.250,00 ou -300,00");
    private final Label lblTotal = new Label();

    AbaContas(ControleFinanceiro cf) { super(cf); }

    @Override protected String nome() { return "Conta"; }

    @Override
    protected void montarFormulario(GridPane form) {
        Label ajuda = new Label("Saldo da conta antes do primeiro lançamento registrado no app.");
        ajuda.getStyleClass().add("empty-label");
        form.addRow(0, Ui.label("Nome:"), tfNome);
        form.addRow(1, Ui.label("Saldo inicial R$:"), tfSaldoInicial, ajuda);
    }

    @Override
    protected javafx.scene.Node barraFiltros() {
        lblTotal.getStyleClass().add("form-label");
        return lblTotal;
    }

    @Override
    protected void configurarColunas(TableView<SaldoConta> t) {
        t.getColumns().add(Ui.colunaTexto("Conta", SaldoConta::getNome, 180));
        t.getColumns().add(Ui.colunaValor("Saldo inicial", SaldoConta::saldoInicial, 120, null));
        t.getColumns().add(Ui.colunaValor("Receitas", SaldoConta::receitas, 120, Ui.COR_RECEITA));
        t.getColumns().add(Ui.colunaValor("Despesas", SaldoConta::despesas, 120, Ui.COR_DESPESA));
        t.getColumns().add(Ui.colunaValor("Investimentos", SaldoConta::investimentos, 120, Ui.COR_INVESTIMENTO));
        t.getColumns().add(Ui.colunaValor("Saldo atual", SaldoConta::saldo, 130, null));
    }

    @Override protected List<SaldoConta> carregar() { return cf.saldosPorConta(); }

    @Override
    protected void aposRecarregar(List<SaldoConta> itens) {
        BigDecimal total = itens.stream().map(SaldoConta::saldo).reduce(Dinheiro.ZERO, BigDecimal::add);
        lblTotal.setText("Saldo de todas as contas: " + Dinheiro.formatar(total)
                + "   (saldo atual = saldo inicial + receitas − despesas − investimentos)");
    }

    @Override
    protected void preencherFormulario(SaldoConta s) {
        tfNome.setText(s.conta().getNome());
        tfSaldoInicial.setText(s.conta().getSaldoInicial().signum() == 0 ? ""
                : Dinheiro.formatarSemSimbolo(s.conta().getSaldoInicial()));
    }

    @Override protected void limparFormulario() { tfNome.clear(); tfSaldoInicial.clear(); }

    private BigDecimal saldoInicial() {
        String t = tfSaldoInicial.getText();
        return t == null || t.isBlank() ? Dinheiro.ZERO : Conversor.parseValor(t);
    }

    @Override protected void salvarNovo() {
        cf.salvarConta(new Conta(0, tfNome.getText().trim(), saldoInicial()));
    }

    @Override protected void salvarAlteracao(SaldoConta o) {
        cf.atualizarConta(new Conta(o.conta().getId(), tfNome.getText().trim(), saldoInicial()));
    }

    @Override protected void excluir(SaldoConta s) { cf.excluirConta(s.conta().getId()); }
    @Override protected String descrever(SaldoConta s) { return "a conta \"" + s.getNome() + "\""; }
}
