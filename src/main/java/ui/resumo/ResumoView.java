package ui.resumo;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.*;
import service.ControleFinanceiro;
import ui.components.Ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResumoView {

    private final ControleFinanceiro cf = ui.Sessao.financeiro();

    private final Label lblGasto = new Label("—");
    private final Label lblInvestido = new Label("—");
    private final Label lblSaldo = new Label("—");
    private final TableView<ResumoMensal> tabela = Ui.tabela("Sem lançamentos no período.");
    private final TableView<SaldoConta> tabelaContas = Ui.tabela("Sem lançamentos no período.");
    private final VBox divReceitas = new VBox(6);
    private final VBox divDespesas = new VBox(6);
    private final VBox divInvest = new VBox(6);

    public VBox getView() {
        List<Integer> anos = cf.anosDisponiveis();
        ComboBox<Integer> cbAno = new ComboBox<>();
        cbAno.getItems().addAll(anos);
        cbAno.setValue(anos.isEmpty() ? LocalDate.now().getYear() : anos.get(anos.size() - 1));
        cbAno.getStyleClass().add("dark-combo");

        ComboBox<String> cbMes = new ComboBox<>();
        cbMes.getItems().add(Meses.TODOS);
        cbMes.getItems().addAll(Meses.NOMES);
        cbMes.setValue(Meses.TODOS);
        cbMes.getStyleClass().add("dark-combo");

        Runnable atualizar = () -> { if (cbAno.getValue() != null) atualizar(cbAno.getValue(), cbMes.getValue()); };
        cbAno.setOnAction(e -> atualizar.run());
        cbMes.setOnAction(e -> atualizar.run());

        HBox filtros = new HBox(12, Ui.label("Ano:"), cbAno, Ui.label("Mês:"), cbMes);
        filtros.setAlignment(Pos.CENTER_LEFT);

        HBox cards = new HBox(16,
                criarCardIndicador("% RENDA GASTA", lblGasto, Ui.COR_DESPESA),
                criarCardIndicador("% RENDA INVESTIDA", lblInvestido, Ui.COR_INVESTIMENTO),
                criarCardIndicador("SALDO DO PERÍODO", lblSaldo, Ui.COR_RECEITA));
        cards.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        Label lblTabela = new Label("Resumo Mês a Mês");
        lblTabela.getStyleClass().add("section-title");
        tabela.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        Label lblDiv = new Label("Divisão por Categoria / Origem");
        lblDiv.getStyleClass().add("section-title");
        HBox divisoes = new HBox(16, divReceitas, divDespesas, divInvest);
        divisoes.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        VBox.setVgrow(tabela, Priority.ALWAYS);
        Label lblContas = new Label("Movimento por Conta");
        lblContas.getStyleClass().add("section-title");
        configurarTabelaContas();

        VBox root = new VBox(16, filtros, cards, lblTabela, tabela, lblContas, tabelaContas, lblDiv, divisoes);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");

        atualizar.run();
        return root;
    }

    private void atualizar(int ano, String mes) {
        TotaisPeriodo t = cf.totais(ano, mes);
        lblGasto.setText(Dinheiro.formatarPercentual(t.percentualGasto()));
        lblInvestido.setText(Dinheiro.formatarPercentual(t.percentualInvestido()));
        lblSaldo.setText(Dinheiro.formatar(t.saldo()));
        lblSaldo.setStyle(estiloValor(t.saldo().signum() >= 0 ? Ui.COR_RECEITA : Ui.COR_DESPESA));

        List<ResumoMensal> resumos = cf.gerarResumoAnual(ano).stream()
                .filter(r -> Meses.ehTodos(mes) || r.getMes().equalsIgnoreCase(mes))
                .filter(ResumoMensal::temMovimento)
                .toList();
        montarColunas(resumos);
        List<SaldoConta> contas = cf.movimentoPorConta(ano, mes);
        tabelaContas.getItems().setAll(contas);
        tabelaContas.setPrefHeight(Math.max(90, 34 + contas.size() * 28));
        tabela.setItems(FXCollections.observableArrayList(resumos));

        atualizarDivisao(divReceitas, "Receitas por Origem", cf.divisaoReceitasPorOrigem(ano, mes), Ui.COR_RECEITA);
        atualizarDivisao(divDespesas, "Despesas por Categoria", cf.divisaoGastosPorCategoria(ano, mes), Ui.COR_DESPESA);
        atualizarDivisao(divInvest, "Investimentos por Tipo", cf.divisaoInvestimentosPorTipo(ano, mes), Ui.COR_INVESTIMENTO);
    }

    /** Receitas, despesas e investimentos de cada conta no período; resultado = receitas − despesas − investimentos. */
    private void configurarTabelaContas() {
        tabelaContas.setMinHeight(90);
        tabelaContas.getColumns().add(Ui.colunaTexto("Conta", SaldoConta::getNome, 180));
        tabelaContas.getColumns().add(Ui.colunaValor("Receitas", SaldoConta::receitas, 120, Ui.COR_RECEITA));
        tabelaContas.getColumns().add(Ui.colunaValor("Despesas", SaldoConta::despesas, 120, Ui.COR_DESPESA));
        tabelaContas.getColumns().add(Ui.colunaValor("Investimentos", SaldoConta::investimentos, 120, Ui.COR_INVESTIMENTO));
        tabelaContas.getColumns().add(Ui.colunaValor("Resultado", SaldoConta::movimento, 130, null));
    }

    /** Colunas fixas + uma coluna por categoria que teve despesa no período mostrado. */
    private void montarColunas(List<ResumoMensal> resumos) {
        tabela.getColumns().clear();
        tabela.getColumns().add(Ui.colunaTexto("Mês", ResumoMensal::getMes, 110));
        tabela.getColumns().add(Ui.colunaValor("Receita", ResumoMensal::getReceita, 115, Ui.COR_RECEITA));
        tabela.getColumns().add(Ui.colunaValor("Despesas", ResumoMensal::getDespesaTotal, 115, Ui.COR_DESPESA));
        tabela.getColumns().add(Ui.colunaValor("Investimentos", ResumoMensal::getInvestimentos, 120, Ui.COR_INVESTIMENTO));
        tabela.getColumns().add(Ui.colunaValor("Saldo", ResumoMensal::getSaldo, 115, null));
        tabela.getColumns().add(Ui.colunaValor("Saldo Acum.", ResumoMensal::getSaldoAcumulado, 120, null));

        Set<String> categorias = new LinkedHashSet<>();
        cf.getCategorias().forEach(c -> {
            if (resumos.stream().anyMatch(r -> r.getDespesasPorCategoria().containsKey(c.getNome())))
                categorias.add(c.getNome());
        });
        for (String cat : categorias)
            tabela.getColumns().add(Ui.colunaValor(cat, r -> r.getDespesaCategoria(cat), 110, null));
    }

    private HBox criarCardIndicador(String titulo, Label lblValor, String cor) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("indicator-title");
        lblValor.setStyle(estiloValor(cor));
        VBox conteudo = new VBox(6, lblTitulo, lblValor);
        conteudo.setPadding(new Insets(14));
        conteudo.getStyleClass().add("indicator-card");
        HBox card = new HBox(conteudo);
        HBox.setHgrow(conteudo, Priority.ALWAYS);
        return card;
    }

    private static String estiloValor(String cor) {
        return "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";";
    }

    private void atualizarDivisao(VBox container, String titulo, Map<String, BigDecimal> dados, String cor) {
        Label header = new Label(titulo);
        header.getStyleClass().add("subsection-title");
        container.getChildren().setAll(header);
        if (dados.isEmpty()) {
            Label vazio = new Label("(sem dados no período)");
            vazio.getStyleClass().add("empty-label");
            container.getChildren().add(vazio);
            return;
        }
        dados.forEach((nome, valor) -> {
            Label lNome = new Label(nome);
            lNome.setPrefWidth(160);
            lNome.getStyleClass().add("div-label");
            Label lValor = new Label(Dinheiro.formatar(valor));
            lValor.setStyle("-fx-text-fill: " + cor + "; -fx-font-weight: bold;");
            HBox linha = new HBox(lNome, lValor);
            linha.setAlignment(Pos.CENTER_LEFT);
            linha.setPadding(new Insets(4, 8, 4, 8));
            linha.getStyleClass().add("div-row");
            container.getChildren().add(linha);
        });
    }
}
