package ui.financas;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.*;
import service.ControleFinanceiro;
import ui.components.DonutChart;
import ui.components.Ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Finanças → Visão geral: resumo do mês atual, saldo por conta, gráficos do mês e fixos ativos. */
public class VisaoGeralView {

    private final ControleFinanceiro cf = ui.Sessao.financeiro();
    private final String mesAtual = Meses.nome(LocalDate.now());
    private final int anoAtual = LocalDate.now().getYear();

    public ScrollPane getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");
        root.getChildren().addAll(
                criarLinhaCards(cf.totais(anoAtual, mesAtual)),
                criarBlocoSaldosPorConta(),
                criarLinhaGraficos(),
                criarBlocoFixos());

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        return scroll;
    }

    // -------------------------------------------------------------------------
    // Cards do mês atual
    // -------------------------------------------------------------------------

    private HBox criarLinhaCards(TotaisPeriodo mes) {
        String sufixo = " — " + mesAtual;
        VBox cardReceitas = criarCard("RECEITAS" + sufixo, Dinheiro.formatar(mes.receitas()), Ui.COR_RECEITA, null);
        VBox cardDespesas = criarCard("DESPESAS" + sufixo, Dinheiro.formatar(mes.despesas()), Ui.COR_DESPESA,
                mes.receitas().signum() > 0 ? Dinheiro.formatarPercentual(mes.percentualGasto()) + " da renda" : null);
        BigDecimal saldo = mes.saldo();
        VBox cardSaldo = criarCard("SALDO" + sufixo, Dinheiro.formatar(saldo),
                saldo.signum() >= 0 ? Ui.COR_RECEITA : Ui.COR_DESPESA,
                "Investido: " + Dinheiro.formatar(mes.investimentos()));

        HBox linha = new HBox(12, cardReceitas, cardDespesas, cardSaldo);
        for (var card : linha.getChildren()) HBox.setHgrow(card, Priority.ALWAYS);
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }

    private VBox criarCard(String titulo, String valor, String cor, String detalhe) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("card-stat-label");
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("card-stat-value");
        lblValor.setStyle("-fx-text-fill: " + cor + ";");

        VBox card = new VBox(8, lblTitulo, lblValor);
        if (detalhe != null) {
            Label lblDetalhe = new Label(detalhe);
            lblDetalhe.getStyleClass().add("card-stat-label");
            card.getChildren().add(lblDetalhe);
        }
        card.getStyleClass().add("profile-card");
        card.setPrefWidth(220);
        card.setPadding(new Insets(14));
        return card;
    }

    // -------------------------------------------------------------------------
    // Gráficos do mês atual
    // -------------------------------------------------------------------------

    private HBox criarLinhaGraficos() {
        DonutChart receitas = donut("Receitas — " + mesAtual, cf.divisaoReceitasPorOrigem(anoAtual, mesAtual));
        DonutChart despesas = donut("Despesas — " + mesAtual, cf.divisaoGastosPorCategoria(anoAtual, mesAtual));
        HBox linha = new HBox(16, receitas, despesas);
        for (var d : List.of(receitas, despesas)) {
            HBox.setHgrow(d, Priority.ALWAYS);
            d.setMaxWidth(Double.MAX_VALUE);
        }
        return linha;
    }

    private DonutChart donut(String titulo, Map<String, BigDecimal> dados) {
        BigDecimal total = dados.values().stream().reduce(Dinheiro.ZERO, BigDecimal::add);
        DonutChart d = new DonutChart(titulo, dados, Dinheiro.formatar(total));
        d.getStyleClass().add("chart-block");
        return d;
    }

    // -------------------------------------------------------------------------
    // Saldo por conta
    // -------------------------------------------------------------------------

    private VBox criarBlocoSaldosPorConta() {
        Label titulo = new Label("Saldo por Conta");
        titulo.getStyleClass().add("section-title");

        TableView<SaldoConta> tabela = Ui.tabela("Nenhuma conta cadastrada.");
        tabela.getColumns().add(Ui.colunaTexto("Conta", SaldoConta::getNome, 180));
        tabela.getColumns().add(Ui.colunaValor("Saldo inicial", SaldoConta::saldoInicial, 120, null));
        tabela.getColumns().add(Ui.colunaValor("Receitas", SaldoConta::receitas, 120, Ui.COR_RECEITA));
        tabela.getColumns().add(Ui.colunaValor("Despesas", SaldoConta::despesas, 120, Ui.COR_DESPESA));
        tabela.getColumns().add(Ui.colunaValor("Investimentos", SaldoConta::investimentos, 120, Ui.COR_INVESTIMENTO));
        tabela.getColumns().add(Ui.colunaValor("Saldo atual", SaldoConta::saldo, 130, null));
        List<SaldoConta> saldos = cf.saldosPorConta();
        tabela.getItems().setAll(saldos);
        tabela.setPrefHeight(Math.max(90, 34 + saldos.size() * 28));

        VBox bloco = new VBox(10, titulo, tabela);
        bloco.getStyleClass().add("fixos-block");
        bloco.setPadding(new Insets(16));
        return bloco;
    }

    // -------------------------------------------------------------------------
    // Lançamentos fixos ativos
    // -------------------------------------------------------------------------

    private VBox criarBlocoFixos() {
        Label titulo = new Label("Lançamentos Fixos Ativos");
        titulo.getStyleClass().add("section-title");

        TableView<LancamentoFixo> tabela = Ui.tabela("Nenhum lançamento fixo ativo.");
        tabela.getColumns().add(tipoColorido());
        tabela.getColumns().add(Ui.colunaTexto("Descrição", LancamentoFixo::getDescricao, 200));
        tabela.getColumns().add(Ui.colunaTexto("Categoria", LancamentoFixo::getCategoriaNome, 130));
        tabela.getColumns().add(Ui.colunaValor("Valor", LancamentoFixo::getValor, 120, null));
        tabela.getColumns().add(Ui.colunaTexto("Conta", LancamentoFixo::getContaNome, 120));
        tabela.getColumns().add(Ui.colunaTexto("Dia", lf -> "Dia " + lf.getDiaVencimento(), 80));
        List<LancamentoFixo> fixos = cf.getLancamentosFixosAtivos();
        tabela.getItems().setAll(fixos);
        tabela.setPrefHeight(Math.max(90, 34 + fixos.size() * 28));

        VBox bloco = new VBox(10, titulo, tabela);
        bloco.getStyleClass().add("fixos-block");
        bloco.setPadding(new Insets(16));
        return bloco;
    }

    private TableColumn<LancamentoFixo, String> tipoColorido() {
        TableColumn<LancamentoFixo, String> col = Ui.colunaTexto("Tipo", lf -> lf.getTipo().name(), 110);
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String tipo, boolean vazio) {
                super.updateItem(tipo, vazio);
                setText(vazio ? null : tipo);
                String cor = vazio || tipo == null ? null : switch (tipo) {
                    case "RECEITA" -> Ui.COR_RECEITA;
                    case "DESPESA" -> Ui.COR_DESPESA;
                    default -> Ui.COR_INVESTIMENTO;
                };
                setStyle(cor == null ? "" : "-fx-text-fill: " + cor + ";");
            }
        });
        return col;
    }
}
