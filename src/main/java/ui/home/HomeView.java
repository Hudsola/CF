package ui.home;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import model.*;
import service.CalculadoraXp;
import service.ControleFinanceiro;
import ui.App;
import ui.components.DonutChart;
import ui.components.Ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HomeView {

    private final ControleFinanceiro cf = ui.Sessao.financeiro();
    private final String mesAtual = Meses.nome(LocalDate.now());
    private final int anoAtual = LocalDate.now().getYear();

    public ScrollPane getView() {
        TotaisPeriodo mes = cf.totais(anoAtual, mesAtual);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");
        root.getChildren().addAll(
                criarLinhaCards(mes),
                criarBlocoSaldosPorConta(),
                criarBlocoXP(),
                criarLinhaGraficos(),
                criarBlocoFixos());

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        return scroll;
    }

    // -------------------------------------------------------------------------
    // Cards: perfil + resumo do mês atual
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

        HBox linha = new HBox(12, criarCardPerfil(), cardReceitas, cardDespesas, cardSaldo);
        for (var card : linha.getChildren()) HBox.setHgrow(card, Priority.ALWAYS);
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }

    /**
     * Painel do usuário no estilo do HUD do GTA San Andreas: foto no lugar da arma (com o nível no lugar
     * da munição), nome no lugar do relógio, idade no lugar do colete, XP na barra de saúde e o saldo
     * geral no lugar do dinheiro.
     */
    private VBox criarCardPerfil() {
        Usuario usuario = cf.getUsuario();
        BigDecimal saldo = cf.saldoTotal();
        Progresso p = cf.getProgresso();

        Label lblNome = new Label(usuario.getNome());
        lblNome.getStyleClass().addAll("hud-texto", "hud-nome");
        lblNome.setMinWidth(0);

        Button btnEditar = new Button("✎");
        btnEditar.getStyleClass().add("btn-secondary");
        btnEditar.setTooltip(new Tooltip("Editar perfil, foto, senha e conta Google"));
        btnEditar.setOnAction(e -> {
            if (PerfilDialog.editar(usuario)) { App.atualizarBarra(); App.navegarPara("home"); }
        });
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linhaNome = new HBox(6, lblNome, espaco, btnEditar);
        linhaNome.setAlignment(Pos.CENTER_LEFT);

        HBox linhaIdade = new HBox(6);
        linhaIdade.setAlignment(Pos.BASELINE_LEFT);
        if (usuario.getIdade() > 0) {
            linhaIdade.getChildren().addAll(valorHud(String.valueOf(usuario.getIdade()), "hud-idade"),
                    textoHud(usuario.getIdade() == 1 ? "ano" : "anos"));
        } else {
            linhaIdade.getChildren().add(textoHud("Idade não informada"));
        }

        VBox direita = new VBox(6, linhaNome, linhaIdade);
        direita.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(direita, Priority.ALWAYS);
        HBox topo = new HBox(14, criarFotoHud(usuario, p.nivel()), direita);
        topo.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barraXp = new ProgressBar(p.fracaoNivel());
        barraXp.getStyleClass().add("hud-barra-xp");
        barraXp.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(barraXp, new Tooltip("Nível " + p.nivel() + " • " + p.xpNoNivel() + " / "
                + p.xpParaProximo() + " XP para o nível " + (p.nivel() + 1) + " (" + p.xpTotal() + " XP no total)"));

        Label lblSaldo = valorHud(Dinheiro.formatar(saldo), "hud-dinheiro");
        lblSaldo.setStyle(lblSaldo.getStyle() + "-fx-text-fill: " + (saldo.signum() >= 0 ? "#3d8f3a" : "#b4191d") + ";");
        Tooltip.install(lblSaldo, new Tooltip("Saldo geral (todas as contas)"));

        VBox card = new VBox(12, topo, barraXp, lblSaldo);
        VBox.setMargin(barraXp, new Insets(8, 0, 0, 0));   // espaço para o nível que sai da moldura da foto
        card.getStyleClass().addAll("profile-card", "hud-card");
        card.setPrefWidth(360);
        card.setMinWidth(320);
        card.setPadding(new Insets(14));
        return card;
    }

    /** Moldura da foto (no lugar do ícone da arma), com o nível embaixo como a munição. */
    private StackPane criarFotoHud(Usuario usuario, int nivel) {
        final double tamanho = 84;
        StackPane moldura = new StackPane();
        moldura.getStyleClass().add("hud-foto");
        moldura.setMinSize(tamanho, tamanho);
        moldura.setMaxSize(tamanho, tamanho);

        Optional<Image> foto = FotoUsuario.imagem(usuario.getId(), tamanho * 2);
        if (foto.isPresent()) {
            ImageView iv = new ImageView(foto.get());
            iv.setFitWidth(tamanho - 6);
            iv.setFitHeight(tamanho - 6);
            iv.setPreserveRatio(false);
            Rectangle recorte = new Rectangle(tamanho - 6, tamanho - 6);
            recorte.setArcWidth(16);
            recorte.setArcHeight(16);
            iv.setClip(recorte);
            moldura.getChildren().add(iv);
        } else {
            String nome = usuario.getNome().isBlank() ? "?" : usuario.getNome().trim().substring(0, 1).toUpperCase();
            Label inicial = new Label(nome);
            inicial.getStyleClass().addAll("hud-texto", "hud-inicial");
            moldura.getChildren().add(inicial);
        }

        Label lblNivel = valorHud("NV " + nivel, "hud-nivel");
        StackPane.setAlignment(lblNivel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(lblNivel, new Insets(0, 0, -12, 0));
        moldura.getChildren().add(lblNivel);

        Tooltip.install(moldura, new Tooltip("Clique para trocar a foto"));
        moldura.setOnMouseClicked(e -> {
            if (FotoUsuario.escolher(App.getStage(), usuario.getId())) App.navegarPara("home");
        });
        return moldura;
    }

    /** Valor do painel do usuário, na fonte do dinheiro do HUD. */
    private static Label valorHud(String texto, String classe) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("hud-valor", classe);
        l.setStyle(Ui.estiloFonteValores());
        return l;
    }

    /** Texto do painel do usuário, na mesma fonte dos títulos de seção ("Saldo por Conta"). */
    private static Label textoHud(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("hud-texto");
        return l;
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
    // Nível / XP
    // -------------------------------------------------------------------------

    private HBox criarBlocoXP() {
        Progresso p = cf.getProgresso();

        StackPane circulo = new StackPane();
        circulo.getStyleClass().add("level-circle");
        circulo.setPrefSize(70, 70);
        circulo.setMinSize(70, 70);
        circulo.setMaxSize(70, 70);   // sem isso o HBox estica a altura e o círculo vira oval
        Label lblNivel = new Label(String.valueOf(p.nivel()));
        lblNivel.getStyleClass().add("level-number");
        circulo.getChildren().add(lblNivel);

        Label lblXpTitulo = new Label("NÍVEL " + p.nivel() + "  •  XP TOTAL");
        lblXpTitulo.getStyleClass().add("xp-label");
        Label lblXp = new Label(p.xpTotal() + " XP");
        lblXp.getStyleClass().add("xp-value");

        ProgressBar barra = new ProgressBar(p.fracaoNivel());
        barra.getStyleClass().add("xp-progress");
        barra.setPrefWidth(300);

        Label lblDetalhe = new Label(p.xpNoNivel() + " / " + p.xpParaProximo() + " XP para o nível " + (p.nivel() + 1));
        lblDetalhe.getStyleClass().add("xp-detalhe");

        Label lblRegras = new Label(String.format(
                "Como ganhar XP: +%d por lançamento registrado • +%d por mês encerrado com saldo positivo • +%d por mês com investimento",
                CalculadoraXp.XP_POR_LANCAMENTO, CalculadoraXp.XP_MES_POSITIVO, CalculadoraXp.XP_MES_COM_INVESTIMENTO));
        lblRegras.getStyleClass().add("xp-detalhe");
        lblRegras.setWrapText(true);

        VBox xpInfo = new VBox(6, lblXpTitulo, lblXp, barra, lblDetalhe, lblRegras);
        HBox bloco = new HBox(24, circulo, xpInfo);
        bloco.getStyleClass().add("xp-block");
        bloco.setPadding(new Insets(18, 24, 18, 24));
        bloco.setAlignment(Pos.CENTER_LEFT);
        return bloco;
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
