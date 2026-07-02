package ui.resumo;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.ResumoMensal;
import service.ControleFinanceiro;

import java.util.List;
import java.util.Map;

public class ResumoView {

    private final ControleFinanceiro cf = new ControleFinanceiro();

    public VBox getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");

        // Filtros
        HBox filtros = new HBox(12);
        filtros.setAlignment(Pos.CENTER_LEFT);

        List<Integer> anos = cf.anosDisponiveis();
        ComboBox<Integer> cbAno = new ComboBox<>();
        cbAno.getItems().addAll(anos);
        if (!anos.isEmpty()) cbAno.setValue(anos.get(anos.size() - 1));
        cbAno.setPromptText("Ano");
        cbAno.getStyleClass().add("dark-combo");

        ComboBox<String> cbMes = new ComboBox<>();
        cbMes.getItems().add("Todos");
        cbMes.getItems().addAll(ControleFinanceiro.MESES);
        cbMes.setValue("Todos");
        cbMes.getStyleClass().add("dark-combo");

        Button btnFiltrar = new Button("Aplicar Filtro");
        btnFiltrar.getStyleClass().add("btn-primary");

        filtros.getChildren().addAll(
            new Label("Ano:"), cbAno,
            new Label("Mês:"), cbMes,
            btnFiltrar
        );

        // Cards de indicadores
        HBox cardsIndicadores = new HBox(16);

        Label lblGastoVal = cardValor("% Renda Gasta", "—%");
        Label lblInvestVal = cardValor("% Renda Investida", "—%");
        Label lblSaldoVal = cardValor("Saldo em Conta", "R$ —");

        HBox cardGasto   = criarCardIndicador("% RENDA GASTA",     lblGastoVal,  "#E85C4A");
        HBox cardInvest  = criarCardIndicador("% RENDA INVESTIDA",  lblInvestVal, "#4A9EE8");
        HBox cardSaldo   = criarCardIndicador("SALDO EM CONTA",     lblSaldoVal,  "#4AE87A");

        HBox.setHgrow(cardGasto,  Priority.ALWAYS);
        HBox.setHgrow(cardInvest, Priority.ALWAYS);
        HBox.setHgrow(cardSaldo,  Priority.ALWAYS);
        cardsIndicadores.getChildren().addAll(cardGasto, cardInvest, cardSaldo);

        // Tabela resumo mês a mês
        Label lblTabelaTitulo = new Label("Resumo Mês a Mês");
        lblTabelaTitulo.getStyleClass().add("section-title");

        TableView<ResumoMensal> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");

        adicionarColuna(tabela, "Mês",           "mes",           120);
        adicionarColuna(tabela, "Receita",        "receita",       110);
        adicionarColuna(tabela, "Investimentos",  "investimentos", 120);
        adicionarColuna(tabela, "Despesas",       "despesaTotal",  110);
        adicionarColuna(tabela, "Saldo",          "saldo",         110);
        adicionarColuna(tabela, "Saldo Acum.",    "saldoAcumulado",120);

        // Blocos de divisão
        Label lblDivTitulo = new Label("Divisão por Categoria / Origem");
        lblDivTitulo.getStyleClass().add("section-title");

        VBox divReceitas   = new VBox(6);
        VBox divDespesas   = new VBox(6);
        VBox divInvest     = new VBox(6);

        Label lblDivRec  = new Label("Receitas por Origem");
        Label lblDivDesp = new Label("Despesas por Categoria");
        Label lblDivInv  = new Label("Investimentos por Tipo");
        lblDivRec.getStyleClass().add("subsection-title");
        lblDivDesp.getStyleClass().add("subsection-title");
        lblDivInv.getStyleClass().add("subsection-title");

        divReceitas.getChildren().add(lblDivRec);
        divDespesas.getChildren().add(lblDivDesp);
        divInvest.getChildren().add(lblDivInv);

        HBox divisoes = new HBox(16);
        HBox.setHgrow(divReceitas, Priority.ALWAYS);
        HBox.setHgrow(divDespesas, Priority.ALWAYS);
        HBox.setHgrow(divInvest,   Priority.ALWAYS);
        divisoes.getChildren().addAll(divReceitas, divDespesas, divInvest);

        // Ação do botão filtrar
        btnFiltrar.setOnAction(e -> {
            Integer ano = cbAno.getValue();
            String mes  = cbMes.getValue();
            if (ano == null) return;

            // Atualiza indicadores
            lblGastoVal.setText(String.format("%.1f%%", cf.porcentagemRendaGasta(ano, mes) * 100));
            lblInvestVal.setText(String.format("%.1f%%", cf.porcentagemRendaInvestida(ano, mes) * 100));
            double saldo = cf.saldoEmConta(ano, mes);
            lblSaldoVal.setText(String.format("R$ %,.2f", saldo));
            lblSaldoVal.setStyle(saldo >= 0 ? "-fx-text-fill: #4AE87A;" : "-fx-text-fill: #E85C4A;");

            // Atualiza tabela
            List<ResumoMensal> resumos = cf.gerarResumoAnual(ano).stream()
                .filter(r -> mes.equals("Todos") || r.getMes().equalsIgnoreCase(mes))
                .filter(r -> r.getReceita() != 0 || r.getDespesaTotal() != 0 || r.getInvestimentos() != 0)
                .toList();
            tabela.setItems(FXCollections.observableArrayList(resumos));

            // Atualiza divisões
            atualizarDivisao(divReceitas,  lblDivRec,  cf.divisaoReceitasPorOrigem(ano, mes),     "#4AE87A");
            atualizarDivisao(divDespesas,  lblDivDesp, cf.divisaoGastosPorCategoria(ano, mes),    "#E85C4A");
            atualizarDivisao(divInvest,    lblDivInv,  cf.divisaoInvestimentosPorTipo(ano, mes),  "#4A9EE8");
        });

        // Dispara filtro inicial se há anos disponíveis
        if (!anos.isEmpty()) btnFiltrar.fire();

        VBox.setVgrow(tabela, Priority.ALWAYS);
        root.getChildren().addAll(
            filtros,
            cardsIndicadores,
            lblTabelaTitulo,
            tabela,
            lblDivTitulo,
            divisoes
        );
        return root;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HBox criarCardIndicador(String titulo, Label lblValor, String cor) {
        VBox conteudo = new VBox(6);
        conteudo.setPadding(new Insets(14));
        conteudo.getStyleClass().add("indicator-card");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("indicator-title");
        lblValor.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");

        conteudo.getChildren().addAll(lblTitulo, lblValor);

        HBox card = new HBox(conteudo);
        HBox.setHgrow(conteudo, Priority.ALWAYS);
        return card;
    }

    private Label cardValor(String tooltip, String texto) {
        Label l = new Label(texto);
        l.setTooltip(new Tooltip(tooltip));
        return l;
    }

    private void atualizarDivisao(VBox container, Label header, Map<String, Double> dados, String cor) {
        container.getChildren().clear();
        container.getChildren().add(header);
        if (dados.isEmpty()) {
            Label vazio = new Label("(sem dados no período)");
            vazio.getStyleClass().add("empty-label");
            container.getChildren().add(vazio);
            return;
        }
        dados.forEach((k, v) -> {
            HBox linha = new HBox();
            linha.setAlignment(Pos.CENTER_LEFT);
            linha.setPadding(new Insets(4, 8, 4, 8));
            linha.getStyleClass().add("div-row");

            Label lNome  = new Label(k);
            lNome.setPrefWidth(160);
            lNome.getStyleClass().add("div-label");

            Label lValor = new Label(String.format("R$ %,.2f", v));
            lValor.setStyle("-fx-text-fill: " + cor + "; -fx-font-weight: bold;");

            linha.getChildren().addAll(lNome, lValor);
            container.getChildren().add(linha);
        });
    }

    private <T> void adicionarColuna(TableView<T> tabela, String titulo, String prop, double largura) {
        TableColumn<T, Object> col = new TableColumn<>(titulo);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(largura);
        tabela.getColumns().add(col);
    }
}
