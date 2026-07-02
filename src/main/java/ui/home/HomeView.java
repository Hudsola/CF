package ui.home;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import model.LancamentoFixo;
import model.Usuario;
import service.ControleFinanceiro;
import ui.components.DonutChart;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeView {

    private final ControleFinanceiro cf = new ControleFinanceiro();

    public VBox getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");

        root.getChildren().addAll(
            criarLinhaCards(),
            criarBlocoXP(),
            criarLinhaGraficos(),
            criarBlocoFixos()
        );

        return root;
    }

    // -------------------------------------------------------------------------
    // Linha de cards (perfil + 3 vazios)
    // -------------------------------------------------------------------------

    private HBox criarLinhaCards() {
        HBox linha = new HBox(12);
        linha.setAlignment(Pos.CENTER_LEFT);

        linha.getChildren().addAll(
            criarCardPerfil(),
            criarCardVazio(),
            criarCardVazio(),
            criarCardVazio()
        );
        return linha;
    }

    private VBox criarCardPerfil() {
        Usuario usuario = cf.getUsuario();
        double saldo    = cf.saldoTotal();

        VBox card = new VBox(8);
        card.getStyleClass().addAll("profile-card", "card-highlight");
        card.setPrefWidth(220);
        card.setPadding(new Insets(14));

        Label lblNome = new Label(usuario.getNome().toUpperCase());
        lblNome.getStyleClass().add("card-name");

        Label lblIdadeLabel = new Label("IDADE");
        lblIdadeLabel.getStyleClass().add("card-stat-label");

        Label lblIdade = new Label(usuario.getIdade() > 0 ? String.valueOf(usuario.getIdade()) : "—");
        lblIdade.getStyleClass().add("card-stat-value");

        Label lblSaldoLabel = new Label("SALDO");
        lblSaldoLabel.getStyleClass().add("card-stat-label");

        Label lblSaldo = new Label(String.format("R$ %,.2f", saldo));
        lblSaldo.getStyleClass().add("card-stat-value");
        lblSaldo.setStyle(saldo >= 0
            ? "-fx-text-fill: #4AE87A;"
            : "-fx-text-fill: #E85C4A;");

        card.getChildren().addAll(lblNome, lblIdadeLabel, lblIdade, lblSaldoLabel, lblSaldo);
        return card;
    }

    private VBox criarCardVazio() {
        VBox card = new VBox();
        card.getStyleClass().add("profile-card");
        card.setPrefWidth(220);
        card.setPadding(new Insets(14));
        Label placeholder = new Label("—");
        placeholder.getStyleClass().add("card-stat-label");
        card.getChildren().add(placeholder);
        return card;
    }

    // -------------------------------------------------------------------------
    // Bloco de XP / Nível
    // -------------------------------------------------------------------------

    private HBox criarBlocoXP() {
        Usuario usuario = cf.getUsuario();

        HBox bloco = new HBox(24);
        bloco.getStyleClass().add("xp-block");
        bloco.setPadding(new Insets(18, 24, 18, 24));
        bloco.setAlignment(Pos.CENTER_LEFT);

        // Nível em círculo
        StackPane circulo = new StackPane();
        circulo.getStyleClass().add("level-circle");
        circulo.setPrefSize(70, 70);
        Label lblNivel = new Label(String.valueOf(usuario.getNivel()));
        lblNivel.getStyleClass().add("level-number");
        circulo.getChildren().add(lblNivel);

        // XP info
        VBox xpInfo = new VBox(6);
        Label lblXpTitulo = new Label("XP TOTAL");
        lblXpTitulo.getStyleClass().add("xp-label");

        Label lblXp = new Label(usuario.getXp() + " XP");
        lblXp.getStyleClass().add("xp-value");

        ProgressBar progressBar = new ProgressBar(usuario.getProgressoXp());
        progressBar.getStyleClass().add("xp-progress");
        progressBar.setPrefWidth(300);

        Label lblXpDetalhe = new Label(usuario.getXp() + " / " + usuario.getXpProximoNivel() + " para o próximo nível");
        lblXpDetalhe.getStyleClass().add("xp-detalhe");

        xpInfo.getChildren().addAll(lblXpTitulo, lblXp, progressBar, lblXpDetalhe);

        bloco.getChildren().addAll(circulo, xpInfo);
        return bloco;
    }

    // -------------------------------------------------------------------------
    // Linha de gráficos donut
    // -------------------------------------------------------------------------

    private HBox criarLinhaGraficos() {
        HBox linha = new HBox(16);

        String mesAtual = LocalDate.now().getMonth()
            .getDisplayName(TextStyle.FULL, new Locale("pt", "BR")).toUpperCase();
        int anoAtual = LocalDate.now().getYear();

        // Donut receitas
        Map<String, Double> receitas = cf.divisaoReceitasPorOrigem(anoAtual, mesAtual);
        double totalReceitas = receitas.values().stream().mapToDouble(Double::doubleValue).sum();
        String labelReceitas = totalReceitas > 0
            ? String.format("R$ %,.0f", totalReceitas)
            : "R$ 0";
        DonutChart donutReceitas = new DonutChart("Receitas — " + mesAtual, receitas, labelReceitas);
        donutReceitas.getStyleClass().add("chart-block");

        // Donut despesas
        Map<String, Double> despesas = cf.divisaoGastosPorCategoria(anoAtual, mesAtual);
        double totalDespesas = despesas.values().stream().mapToDouble(Double::doubleValue).sum();
        String labelDespesas = totalDespesas > 0
            ? String.format("R$ %,.0f", totalDespesas)
            : "R$ 0";
        DonutChart donutDespesas = new DonutChart("Despesas — " + mesAtual, despesas, labelDespesas);
        donutDespesas.getStyleClass().add("chart-block");

        HBox.setHgrow(donutReceitas, Priority.ALWAYS);
        HBox.setHgrow(donutDespesas, Priority.ALWAYS);
        donutReceitas.setMaxWidth(Double.MAX_VALUE);
        donutDespesas.setMaxWidth(Double.MAX_VALUE);

        linha.getChildren().addAll(donutReceitas, donutDespesas);
        return linha;
    }

    // -------------------------------------------------------------------------
    // Bloco lançamentos fixos ativos
    // -------------------------------------------------------------------------

    private VBox criarBlocoFixos() {
        VBox bloco = new VBox(10);
        bloco.getStyleClass().add("fixos-block");
        bloco.setPadding(new Insets(16));

        Label titulo = new Label("Lançamentos Fixos Ativos");
        titulo.getStyleClass().add("section-title");

        List<LancamentoFixo> fixos = cf.getLancamentosFixosAtivos();

        if (fixos.isEmpty()) {
            Label vazio = new Label("Nenhum lançamento fixo ativo.");
            vazio.getStyleClass().add("empty-label");
            bloco.getChildren().addAll(titulo, vazio);
            return bloco;
        }

        // Cabeçalho
        HBox header = criarLinhaFixo("TIPO", "DESCRIÇÃO", "CATEGORIA", "VALOR", "CONTA", "DIA", true);
        bloco.getChildren().addAll(titulo, header);

        for (LancamentoFixo lf : fixos) {
            HBox linha = criarLinhaFixo(
                lf.getTipo().name(),
                lf.getDescricao(),
                lf.getCategoriaNome(),
                String.format("R$ %,.2f", lf.getValor()),
                lf.getContaNome(),
                "Dia " + lf.getDiaVencimento(),
                false
            );
            bloco.getChildren().add(linha);
        }

        return bloco;
    }

    private HBox criarLinhaFixo(String tipo, String desc, String cat,
                                 String valor, String conta, String dia, boolean header) {
        HBox linha = new HBox();
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(6, 8, 6, 8));
        if (header) linha.getStyleClass().add("table-header");
        else        linha.getStyleClass().add("table-row");

        Label lTipo  = col(tipo,  100);
        Label lDesc  = col(desc,  200);
        Label lCat   = col(cat,   130);
        Label lValor = col(valor, 120);
        Label lConta = col(conta, 120);
        Label lDia   = col(dia,    80);

        if (!header) {
            switch (tipo) {
                case "RECEITA"      -> lTipo.setStyle("-fx-text-fill: #4AE87A;");
                case "DESPESA"      -> lTipo.setStyle("-fx-text-fill: #E85C4A;");
                case "INVESTIMENTO" -> lTipo.setStyle("-fx-text-fill: #4A9EE8;");
            }
        }

        linha.getChildren().addAll(lTipo, lDesc, lCat, lValor, lConta, lDia);
        return linha;
    }

    private Label col(String texto, double largura) {
        Label l = new Label(texto);
        l.setPrefWidth(largura);
        l.getStyleClass().add("table-cell");
        return l;
    }
}
