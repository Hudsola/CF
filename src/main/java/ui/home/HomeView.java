package ui.home;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.*;
import service.CalculadoraXp;
import service.ControleFinanceiro;
import service.Missoes;
import ui.App;
import ui.components.Ui;

import java.time.LocalDate;
import java.util.List;

/**
 * Home: painel do usuário, um card de resumo por aba (com botão para abri-la) e o nível/XP.
 * Estoque aparece como "em breve" até ganhar funcionalidades.
 */
public class HomeView {

    private final ControleFinanceiro cf = ui.Sessao.financeiro();
    private final Missoes missoes = new Missoes(ui.Sessao.usuario().getId());

    public ScrollPane getView() {
        HBox topo = new HBox(12, new PainelUsuario(cf).getView(), criarCardFinancas());
        for (var card : topo.getChildren()) HBox.setHgrow(card, Priority.ALWAYS);

        HBox outras = new HBox(12,
                criarCardMissoes(),
                criarCardAgenda(),
                criarCardEmBreve("ESTOQUE", "Itens e quantidades.", "estoque"));
        for (var card : outras.getChildren()) HBox.setHgrow(card, Priority.ALWAYS);

        VBox root = new VBox(16, topo, outras, criarBlocoXP());
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        return scroll;
    }

    // -------------------------------------------------------------------------
    // Cards de resumo das abas
    // -------------------------------------------------------------------------

    /** Finanças: receitas, despesas e saldo do mês atual. */
    private VBox criarCardFinancas() {
        String mes = Meses.nome(LocalDate.now());
        TotaisPeriodo t = cf.totais(LocalDate.now().getYear(), mes);
        VBox linhas = new VBox(6,
                linhaValor("Receitas", t.receitas(), Ui.COR_RECEITA),
                linhaValor("Despesas", t.despesas(), Ui.COR_DESPESA),
                linhaValor("Investido", t.investimentos(), Ui.COR_INVESTIMENTO),
                linhaValor("Saldo do mês", t.saldo(), t.saldo().signum() >= 0 ? Ui.COR_RECEITA : Ui.COR_DESPESA));
        return cardAba("FINANÇAS — " + mes, linhas, "Abrir Finanças", "financas");
    }

    /** Missões: o que falta hoje, o que já foi feito, atrasadas e objetivos em andamento. */
    private VBox criarCardMissoes() {
        Missoes.Resumo r = missoes.resumo();
        VBox linhas = new VBox(6,
                linhaNumero("Pendentes hoje", r.pendentesHoje(), r.pendentesHoje() > 0 ? Ui.COR_DESTAQUE : Ui.COR_RECEITA),
                linhaNumero("Feitas hoje", r.feitasHoje(), Ui.COR_RECEITA),
                linhaNumero("Atrasadas (7 dias)", r.atrasadas(), r.atrasadas() > 0 ? Ui.COR_DESPESA : Ui.COR_RECEITA),
                linhaNumero("Objetivos em andamento", r.semPrazoEmAndamento(), Ui.COR_INVESTIMENTO));
        return cardAba("MISSÕES — HOJE", linhas, "Abrir Missões", "missoes");
    }

    /** Agenda: as próximas missões dos próximos 7 dias. */
    private VBox criarCardAgenda() {
        LocalDate hoje = missoes.hoje();
        List<OcorrenciaMissao> proximas = missoes.ocorrencias(hoje, hoje.plusDays(6)).stream()
                .filter(o -> !o.concluida()).limit(4).toList();
        VBox linhas = new VBox(4);
        if (proximas.isEmpty()) {
            Label vazio = new Label("Nada pendente nos próximos 7 dias.");
            vazio.getStyleClass().add("form-label");
            linhas.getChildren().add(vazio);
        }
        for (OcorrenciaMissao o : proximas) {
            Label l = new Label(Ui.diaCurto(o.data()) + "  ·  " + o.missao().titulo());
            l.getStyleClass().add("form-label");
            linhas.getChildren().add(l);
        }
        return cardAba("AGENDA — PRÓXIMOS 7 DIAS", linhas, "Abrir Agenda", "agenda");
    }

    private static HBox linhaNumero(String nome, int valor, String cor) {
        Label lblNome = new Label(nome);
        lblNome.getStyleClass().add("form-label");
        Label lblValor = new Label(String.valueOf(valor));
        lblValor.getStyleClass().add("card-linha-valor");
        lblValor.setStyle("-fx-text-fill: " + cor + ";");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linha = new HBox(8, lblNome, espaco, lblValor);
        linha.setAlignment(Pos.BASELINE_LEFT);
        return linha;
    }

    private VBox criarCardEmBreve(String titulo, String descricao, String tela) {
        Label texto = new Label(descricao);
        texto.getStyleClass().add("form-label");
        texto.setWrapText(true);
        Label selo = new Label("EM BREVE");
        selo.getStyleClass().add("selo-em-breve");
        return cardAba(titulo, new VBox(8, texto, selo), "Abrir " + titulo.charAt(0) + titulo.substring(1).toLowerCase(), tela);
    }

    /** Card com botão que leva à aba {@code tela}. */
    private VBox cardAba(String titulo, Region conteudo, String textoBotao, String tela) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("card-stat-label");
        Button abrir = new Button(textoBotao + " →");
        abrir.getStyleClass().add("btn-secondary");
        abrir.setOnAction(e -> App.navegarPara(tela));
        Region espaco = new Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);

        VBox card = new VBox(10, lblTitulo, conteudo, espaco, abrir);
        card.getStyleClass().addAll("profile-card", "card-aba");
        card.setPadding(new Insets(14));
        card.setPrefWidth(220);
        return card;
    }

    private static HBox linhaValor(String nome, java.math.BigDecimal valor, String cor) {
        Label lblNome = new Label(nome);
        lblNome.getStyleClass().add("form-label");
        Label lblValor = new Label(Dinheiro.formatar(valor));
        lblValor.getStyleClass().add("card-linha-valor");
        lblValor.setStyle("-fx-text-fill: " + cor + ";");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linha = new HBox(8, lblNome, espaco, lblValor);
        linha.setAlignment(Pos.BASELINE_LEFT);
        return linha;
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
                "Como ganhar XP: +%d por dia de missão cumprido • +%d por objetivo sem prazo cumprido (missões principais valem "
                        + "o dobro) • +%d por lançamento registrado • +%d por mês encerrado com saldo positivo • +%d por mês com investimento",
                CalculadoraXp.XP_POR_MISSAO, CalculadoraXp.XP_MISSAO_SEM_PRAZO,
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

}
