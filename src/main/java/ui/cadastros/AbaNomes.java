package ui.cadastros;

import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import model.Categoria;
import service.ControleFinanceiro;
import ui.components.Ui;

import java.util.List;

/** Aba de cadastro que só tem nome: Categorias. */
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
            @Override protected String descreverVarios(List<Categoria> itens) { return "as " + itens.size() + " categorias selecionadas"; }
        };
    }
}
