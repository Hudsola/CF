package ui.cadastros;

import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import model.Categoria;
import model.Conta;
import service.ControleFinanceiro;
import ui.components.Ui;

import java.util.List;

/** Abas de cadastros que só têm nome: Categorias e Contas. */
final class AbaNomes {

    private AbaNomes() {}

    static AbaCrud<Categoria> categorias(ControleFinanceiro cf) {
        return new AbaCrud<>(cf) {
            private final TextField tfNome = Ui.campo("Nome da categoria");

            @Override protected String nome() { return "Categoria"; }
            @Override protected void montarFormulario(GridPane form) { form.addRow(0, Ui.label("Nome:"), tfNome); }
            @Override protected void configurarColunas(TableView<Categoria> t) {
                t.getColumns().add(Ui.colunaTexto("Nome", Categoria::getNome, 300));
            }
            @Override protected List<Categoria> carregar() { return cf.getCategorias(); }
            @Override protected void preencherFormulario(Categoria c) { tfNome.setText(c.getNome()); }
            @Override protected void limparFormulario() { tfNome.clear(); }
            @Override protected void salvarNovo() { cf.salvarCategoria(new Categoria(tfNome.getText().trim())); }
            @Override protected void salvarAlteracao(Categoria o) {
                cf.atualizarCategoria(new Categoria(o.getId(), tfNome.getText().trim()));
            }
            @Override protected void excluir(Categoria c) { cf.excluirCategoria(c.getId()); }
            @Override protected String descrever(Categoria c) { return "a categoria \"" + c.getNome() + "\""; }
        };
    }

    static AbaCrud<Conta> contas(ControleFinanceiro cf) {
        return new AbaCrud<>(cf) {
            private final TextField tfNome = Ui.campo("Nome da conta (ex: Nubank, Itaú)");

            @Override protected String nome() { return "Conta"; }
            @Override protected void montarFormulario(GridPane form) { form.addRow(0, Ui.label("Nome:"), tfNome); }
            @Override protected void configurarColunas(TableView<Conta> t) {
                t.getColumns().add(Ui.colunaTexto("Nome", Conta::getNome, 300));
            }
            @Override protected List<Conta> carregar() { return cf.getContas(); }
            @Override protected void preencherFormulario(Conta c) { tfNome.setText(c.getNome()); }
            @Override protected void limparFormulario() { tfNome.clear(); }
            @Override protected void salvarNovo() { cf.salvarConta(new Conta(tfNome.getText().trim())); }
            @Override protected void salvarAlteracao(Conta o) {
                cf.atualizarConta(new Conta(o.getId(), tfNome.getText().trim()));
            }
            @Override protected void excluir(Conta c) { cf.excluirConta(c.getId()); }
            @Override protected String descrever(Conta c) { return "a conta \"" + c.getNome() + "\""; }
        };
    }
}
