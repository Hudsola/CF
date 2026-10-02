package ui.cadastros;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import service.ControleFinanceiro;

/** Tela "Cadastros": uma aba por tipo de registro. Cada aba recarrega seus dados ao ser selecionada. */
public class CadastrosView {

    private final ControleFinanceiro cf = ui.Sessao.financeiro();

    public VBox getView() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("cadastros-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        adicionar(tabs, "Receitas", new AbaReceitas(cf));
        adicionar(tabs, "Despesas", new AbaDespesas(cf));
        adicionar(tabs, "Investimentos", new AbaInvestimentos(cf));
        adicionar(tabs, "Fixos", new AbaFixos(cf));
        adicionar(tabs, "Categorias", AbaNomes.categorias(cf));
        adicionar(tabs, "Contas", new AbaContas(cf));

        VBox root = new VBox(tabs);
        root.getStyleClass().add("main-content");
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return root;
    }

    private void adicionar(TabPane tabs, String titulo, AbaCrud<?> aba) {
        Tab tab = new Tab(titulo, aba.getView());
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) aba.atualizar(); });
        tabs.getTabs().add(tab);
    }
}
