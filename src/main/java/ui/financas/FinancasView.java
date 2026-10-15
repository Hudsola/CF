package ui.financas;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ui.financas.cadastros.CadastrosView;
import ui.financas.resumo.ResumoView;

import java.util.function.Supplier;

/**
 * Aba "Finanças": Visão geral | Cadastros | Resumo.
 *
 * Visão geral e Resumo são recriados sempre que a sub-aba é aberta, para refletir o que mudou em Cadastros.
 */
public class FinancasView {

    public static final String VISAO_GERAL = "Visão geral";
    public static final String CADASTROS = "Cadastros";
    public static final String RESUMO = "Resumo";

    public VBox getView() {
        TabPane abas = new TabPane();
        abas.getStyleClass().add("financas-tabs");
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        abas.getTabs().addAll(
                recarregavel(VISAO_GERAL, () -> new VisaoGeralView().getView()),
                new Tab(CADASTROS, new CadastrosView().getView()),
                recarregavel(RESUMO, () -> new ResumoView().getView()));

        VBox root = new VBox(abas);
        root.getStyleClass().add("main-content");
        VBox.setVgrow(abas, Priority.ALWAYS);
        return root;
    }

    private static Tab recarregavel(String titulo, Supplier<Node> tela) {
        Tab tab = new Tab(titulo, tela.get());
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) tab.setContent(tela.get()); });
        return tab;
    }
}
