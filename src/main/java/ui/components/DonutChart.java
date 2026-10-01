package ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import model.Dinheiro;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Gráfico de rosca com legenda; passar o mouse numa fatia mostra nome, valor e % no centro. */
public class DonutChart extends VBox {

    private static final String[] COLORS = {"#E8A838", "#4A9EE8", "#E85C4A", "#4AE87A", "#A84AE8", "#E84AA8", "#4AE8E8", "#E8E84A"};

    private static final double TAMANHO = 180;
    private static final double CENTRO = TAMANHO / 2;
    private static final double RAIO_EXTERNO = 80;
    private static final double RAIO_INTERNO = 45;
    /** O traço do arco é centrado no raio, então o anel vai de RAIO_INTERNO a RAIO_EXTERNO. */
    private static final double RAIO_ARCO = (RAIO_EXTERNO + RAIO_INTERNO) / 2;
    private static final double ESPESSURA = RAIO_EXTERNO - RAIO_INTERNO;
    private static final int MAX_LEGENDA = 5;

    public DonutChart(String titulo, Map<String, BigDecimal> dados, String labelCentro) {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(10);
        getStyleClass().add("donut-container");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("donut-title");

        Map<String, BigDecimal> ordenados = ordenarPorValor(dados);

        Label lblCentro = new Label(labelCentro);
        lblCentro.getStyleClass().add("donut-center-label");
        lblCentro.setAlignment(Pos.CENTER);
        lblCentro.setMouseTransparent(true);
        lblCentro.setMaxWidth(RAIO_INTERNO * 2);
        lblCentro.setWrapText(true);

        StackPane stack = new StackPane(desenharDonut(ordenados, lblCentro, labelCentro), lblCentro);
        stack.setAlignment(Pos.CENTER);
        stack.setPrefSize(TAMANHO, TAMANHO);
        stack.setMinSize(TAMANHO, TAMANHO);
        stack.setMaxSize(TAMANHO, TAMANHO);

        getChildren().addAll(lblTitulo, stack, criarLegenda(ordenados));
    }

    private Pane desenharDonut(Map<String, BigDecimal> dados, Label lblCentro, String textoOriginal) {
        Pane pane = new Pane();
        pane.setPrefSize(TAMANHO, TAMANHO);
        pane.setMinSize(TAMANHO, TAMANHO);
        pane.setMaxSize(TAMANHO, TAMANHO);
        pane.setPickOnBounds(false);

        double total = dados.values().stream().mapToDouble(BigDecimal::doubleValue).sum();
        if (total <= 0) {
            Circle vazio = new Circle(CENTRO, CENTRO, RAIO_ARCO);
            vazio.setFill(null);
            vazio.setStroke(Color.web("#3a3a3a"));
            vazio.setStrokeWidth(ESPESSURA);
            pane.getChildren().add(vazio);
            return pane;
        }

        double angulo = 90;
        int idx = 0;
        for (Map.Entry<String, BigDecimal> entry : dados.entrySet()) {
            double tamanho = entry.getValue().doubleValue() / total * 360;
            Arc arc = new Arc(CENTRO, CENTRO, RAIO_ARCO, RAIO_ARCO, angulo, -tamanho);
            arc.setType(ArcType.OPEN);
            arc.setFill(null);
            arc.setStroke(Color.web(COLORS[idx % COLORS.length]));
            arc.setStrokeWidth(ESPESSURA);
            arc.setStrokeLineCap(StrokeLineCap.BUTT);

            String texto = entry.getKey() + "\n" + Dinheiro.formatar(entry.getValue()) + "\n"
                    + String.format("%.1f%%", entry.getValue().doubleValue() / total * 100);
            arc.setOnMouseEntered(e -> { lblCentro.setText(texto); arc.setOpacity(0.75); });
            arc.setOnMouseExited(e -> { lblCentro.setText(textoOriginal); arc.setOpacity(1.0); });

            pane.getChildren().add(arc);
            angulo -= tamanho;
            idx++;
        }
        return pane;
    }

    private VBox criarLegenda(Map<String, BigDecimal> dados) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        int idx = 0;
        for (Map.Entry<String, BigDecimal> entry : dados.entrySet()) {
            if (idx == MAX_LEGENDA) {
                Label mais = new Label("+ " + (dados.size() - MAX_LEGENDA) + " outro(s) — passe o mouse no gráfico");
                mais.getStyleClass().add("empty-label");
                box.getChildren().add(mais);
                break;
            }
            Label item = new Label("● " + entry.getKey() + "  " + Dinheiro.formatar(entry.getValue()));
            item.setStyle("-fx-text-fill: " + COLORS[idx % COLORS.length] + "; -fx-font-size: 11px;");
            box.getChildren().add(item);
            idx++;
        }
        return box;
    }

    private static Map<String, BigDecimal> ordenarPorValor(Map<String, BigDecimal> dados) {
        return dados.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
