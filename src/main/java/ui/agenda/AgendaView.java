package ui.agenda;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import model.OcorrenciaMissao;
import service.Missoes;
import ui.components.Ui;
import ui.missoes.ListaMissoes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aba "Agenda": calendário do mês com as missões de cada dia (pendentes, feitas e atrasadas) e, ao lado,
 * a lista do dia selecionado, onde dá para marcar como feita.
 */
public class AgendaView {

    /** Quantas missões aparecem dentro de cada dia do calendário (o resto vira "+N"). */
    static final int ITENS_POR_DIA = 3;

    private final Missoes missoes = new Missoes(ui.Sessao.usuario().getId());
    private final GridPane calendario = new GridPane();
    private final Label lblMes = new Label();
    private final Label lblDia = new Label();
    private final ListaMissoes listaDoDia = new ListaMissoes(missoes, false, this::atualizar);

    private YearMonth mes = YearMonth.from(missoes.hoje());
    private LocalDate selecionado = missoes.hoje();

    public Node getView() {
        Button anterior = navegacao("‹", "Mês anterior", () -> irPara(mes.minusMonths(1)));
        Button proximo = navegacao("›", "Próximo mês", () -> irPara(mes.plusMonths(1)));
        Button hoje = navegacao("Hoje", "Voltar para hoje", () -> { selecionado = missoes.hoje(); irPara(YearMonth.from(selecionado)); });
        lblMes.getStyleClass().add("section-title");
        lblMes.setMinWidth(170);
        lblMes.setAlignment(Pos.CENTER);
        HBox barra = new HBox(8, anterior, lblMes, proximo, hoje);
        barra.setAlignment(Pos.CENTER_LEFT);

        calendario.getStyleClass().add("agenda-calendario");
        calendario.setHgap(4);
        calendario.setVgap(4);
        for (int c = 0; c < 7; c++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            calendario.getColumnConstraints().add(col);
        }
        VBox ladoEsquerdo = new VBox(12, barra, calendario);
        HBox.setHgrow(ladoEsquerdo, Priority.ALWAYS);

        lblDia.getStyleClass().add("section-title");
        lblDia.setWrapText(true);
        VBox ladoDireito = new VBox(10, lblDia, listaDoDia);
        ladoDireito.getStyleClass().add("fixos-block");
        ladoDireito.setPadding(new Insets(16));
        ladoDireito.setPrefWidth(320);
        ladoDireito.setMinWidth(280);

        HBox corpo = new HBox(16, ladoEsquerdo, ladoDireito);
        corpo.setPadding(new Insets(20));
        corpo.getStyleClass().add("main-content");
        atualizar();

        ScrollPane scroll = new ScrollPane(corpo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        return scroll;
    }

    private Button navegacao(String texto, String dica, Runnable acao) {
        Button b = new Button(texto);
        b.getStyleClass().add("btn-secondary");
        b.setTooltip(new javafx.scene.control.Tooltip(dica));
        b.setOnAction(e -> acao.run());
        return b;
    }

    private void irPara(YearMonth novoMes) {
        mes = novoMes;
        if (!YearMonth.from(selecionado).equals(mes)) selecionado = mes.atDay(1);
        atualizar();
    }

    void selecionar(LocalDate dia) {
        selecionado = dia;
        if (!YearMonth.from(dia).equals(mes)) mes = YearMonth.from(dia);
        atualizar();
    }

    void atualizar() {
        String nomeMes = mes.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Ui.PT_BR);
        lblMes.setText(Character.toUpperCase(nomeMes.charAt(0)) + nomeMes.substring(1) + " " + mes.getYear());

        // semanas de segunda a domingo cobrindo o mês inteiro (with() fica na mesma semana ISO)
        LocalDate inicio = mes.atDay(1).with(DayOfWeek.MONDAY);
        LocalDate fim = mes.atEndOfMonth().with(DayOfWeek.SUNDAY);
        Map<LocalDate, List<OcorrenciaMissao>> porDia = missoes.ocorrencias(inicio, fim).stream()
                .collect(Collectors.groupingBy(OcorrenciaMissao::data));

        calendario.getChildren().clear();
        for (int c = 0; c < 7; c++) {
            Label cab = new Label(DayOfWeek.of(c + 1).getDisplayName(TextStyle.SHORT, Ui.PT_BR).replace(".", "").toUpperCase());
            cab.getStyleClass().add("agenda-cabecalho");
            cab.setMaxWidth(Double.MAX_VALUE);
            calendario.add(cab, c, 0);
        }
        int linha = 1;
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            calendario.add(celula(d, porDia.getOrDefault(d, List.of())), d.getDayOfWeek().getValue() - 1, linha);
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) linha++;
        }

        lblDia.setText(Character.toUpperCase(Ui.diaPorExtenso(selecionado).charAt(0)) + Ui.diaPorExtenso(selecionado).substring(1));
        listaDoDia.mostrar(missoes.doDia(selecionado), "Nenhuma missão neste dia.");
    }

    private VBox celula(LocalDate dia, List<OcorrenciaMissao> doDia) {
        Label numero = new Label(String.valueOf(dia.getDayOfMonth()));
        numero.getStyleClass().add("agenda-numero");
        VBox cel = new VBox(2, numero);
        cel.getStyleClass().add("agenda-dia");
        cel.setUserData(dia);
        cel.setMinHeight(86);
        cel.setPadding(new Insets(4, 6, 4, 6));
        if (!YearMonth.from(dia).equals(mes)) cel.getStyleClass().add("agenda-dia-fora");
        if (dia.equals(missoes.hoje())) cel.getStyleClass().add("agenda-dia-hoje");
        if (dia.equals(selecionado)) cel.getStyleClass().add("agenda-dia-selecionado");

        for (int i = 0; i < Math.min(ITENS_POR_DIA, doDia.size()); i++) {
            OcorrenciaMissao o = doDia.get(i);
            Label item = new Label(o.missao().titulo());
            item.getStyleClass().add("agenda-item");
            item.getStyleClass().add(o.concluida() ? "agenda-item-feita"
                    : o.atrasada(missoes.hoje()) ? "agenda-item-atrasada" : "agenda-item-pendente");
            item.setMaxWidth(Double.MAX_VALUE);
            cel.getChildren().add(item);
        }
        if (doDia.size() > ITENS_POR_DIA) {
            Label mais = new Label("+" + (doDia.size() - ITENS_POR_DIA));
            mais.getStyleClass().add("form-label");
            cel.getChildren().add(mais);
        }
        cel.setOnMouseClicked(e -> selecionar(dia));
        String dica = Ui.diaCurto(dia) + (doDia.isEmpty() ? "" : " — " + doDia.size() + " missão(ões)");
        javafx.scene.control.Tooltip.install(cel, new javafx.scene.control.Tooltip(dica));
        return cel;
    }
}
