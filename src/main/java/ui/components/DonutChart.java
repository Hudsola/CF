package ui.components;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public class DonutChart extends VBox {

    private static final String[] COLORS = {
        "#E8A838", "#4A9EE8", "#E85C4A", "#4AE87A",
        "#A84AE8", "#E84AA8", "#4AE8E8", "#E8E84A"
    };

    public DonutChart(String titulo, Map<String, Double> dados, String labelCentro) {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(10);
        getStyleClass().add("donut-container");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("donut-title");

        // Canvas do donut
        Canvas canvas = new Canvas(180, 180);
        desenharDonut(canvas, dados);

        // Label central
        Label lblCentro = new Label(labelCentro);
        lblCentro.getStyleClass().add("donut-center-label");

        StackPane stack = new StackPane(canvas, lblCentro);
        stack.setAlignment(Pos.CENTER);

        // Legenda
        VBox legenda = criarLegenda(dados);

        getChildren().addAll(lblTitulo, stack, legenda);
    }

    private void desenharDonut(Canvas canvas, Map<String, Double> dados) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double total = dados.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) {
            gc.setFill(Color.web("#3a3a3a"));
            gc.fillOval(10, 10, 160, 160);
            gc.setFill(Color.web("#1e1e1e"));
            gc.fillOval(45, 45, 90, 90);
            return;
        }

        double angulo = -90;
        int idx = 0;
        for (Map.Entry<String, Double> entry : dados.entrySet()) {
            double fatia = (entry.getValue() / total) * 360;
            gc.setFill(Color.web(COLORS[idx % COLORS.length]));
            gc.fillArc(10, 10, 160, 160, angulo, fatia, javafx.scene.shape.ArcType.ROUND);
            angulo += fatia;
            idx++;
        }

        // Buraco do meio (donut)
        gc.setFill(Color.web("#1e1e1e"));
        gc.fillOval(45, 45, 90, 90);
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
}
