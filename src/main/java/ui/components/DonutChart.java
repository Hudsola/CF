package ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;

import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DonutChart extends VBox {

    private static final String[] COLORS = {"#E8A838", "#4A9EE8", "#E85C4A", "#4AE87A", "#A84AE8", "#E84AA8", "#4AE8E8", "#E8E84A"};

    public DonutChart(String titulo, Map<String, Double> dados, String labelCentro) {

        setAlignment(Pos.TOP_CENTER);
        setSpacing(10);
        getStyleClass().add("donut-container");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("donut-title");

        Map<String, Double> dadosOrdenados = ordenarPorValor(dados);

        Label lblCentro = new Label(labelCentro);
        lblCentro.getStyleClass().add("donut-center-label");
        lblCentro.setAlignment(Pos.CENTER);
        lblCentro.setMouseTransparent(true);

        Pane donut = desenharDonut(dadosOrdenados, lblCentro, labelCentro);

        StackPane stack = new StackPane(donut, lblCentro);
        stack.setAlignment(Pos.CENTER);
        stack.setPrefSize(180, 180);
        stack.setMinSize(180, 180);
        stack.setMaxSize(180, 180);

        VBox legenda = criarLegenda(dadosOrdenados);

        getChildren().addAll(lblTitulo, stack, legenda);
    }

    private Pane desenharDonut(Map<String, Double> dados, Label lblCentro, String textoOriginal) {

        Pane pane = new Pane();
        pane.setPrefSize(180, 180);
        pane.setMinSize(180, 180);
        pane.setMaxSize(180, 180);
        pane.setPickOnBounds(false);

        double total = dados.values().stream().mapToDouble(Double::doubleValue).sum();

        if (total == 0) {

            Circle externo = new Circle(90, 90, 80);
            externo.setFill(Color.web("#3a3a3a"));

            Circle interno = new Circle(90, 90, 45);
            interno.setFill(Color.web("#1e1e1e"));

            pane.getChildren().addAll(externo, interno);

            return pane;
        }

        double angulo = 90;
        int idx = 0;

        for (Map.Entry<String, Double> entry : dados.entrySet()) {

            double tamanho = entry.getValue() / total * 360;

            Color cor = Color.web(COLORS[idx % COLORS.length]);

            Arc arc = new Arc(90, 90, 80, 80, angulo, -tamanho);

            arc.setType(ArcType.OPEN);
            arc.setFill(null);
            arc.setStroke(cor);
            arc.setStrokeWidth(35); // aproximadamente 80 - 45
            arc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.BUTT);

            String nome = entry.getKey();
            double valor = entry.getValue();
            double percentual = valor / total * 100;

            arc.setOnMouseEntered(e -> {

                lblCentro.setText(nome + "\nR$ " + String.format("%,.2f", valor) + "\n" + String.format("%.1f%%", percentual));

                arc.setOpacity(0.75);
            });

            arc.setOnMouseExited(e -> {

                lblCentro.setText(textoOriginal);
                arc.setOpacity(1.0);

            });

            pane.getChildren().add(arc);

            angulo -= tamanho;
            idx++;
        }

        Circle centro = new Circle(90, 90, 45);
        centro.setFill(Color.web("#1e1e1e"));
        centro.setMouseTransparent(true);

        pane.getChildren().add(centro);

        return pane;
    }

    private VBox criarLegenda(Map<String, Double> dados) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        int idx = 0;
        for (Map.Entry<String, Double> entry : dados.entrySet()) {
            Label item = new Label("● " + entry.getKey() + "  R$ " + String.format("%,.2f", entry.getValue()));
            item.setStyle("-fx-text-fill: " + COLORS[idx % COLORS.length] + "; -fx-font-size: 11px;");
            box.getChildren().add(item);
            idx++;
            if (idx >= 5) break; // máx 5 itens na legenda
        }
        return box;
    }

    private Map<String, Double> ordenarPorValor(Map<String, Double> dados) {
        return dados.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
