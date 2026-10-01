package ui.components;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import model.Dinheiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Fábricas e utilitários de interface compartilhados pelas telas. */
public final class Ui {

    public static final String COR_RECEITA = "#4AE87A";
    public static final String COR_DESPESA = "#E85C4A";
    public static final String COR_INVESTIMENTO = "#4A9EE8";
    public static final String COR_DESTAQUE = "#E8A838";

    public static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Ui() {}

    public static TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("dark-field");
        tf.setPrefWidth(260);
        return tf;
    }

    public static Label label(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("form-label");
        return l;
    }

    /** DatePicker que mostra e aceita datas no formato dd/mm/aaaa. */
    public static DatePicker seletorData(LocalDate inicial) {
        DatePicker dp = new DatePicker(inicial);
        dp.setPromptText("dd/mm/aaaa");
        dp.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d == null ? "" : DATA_BR.format(d); }
            @Override public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try { return LocalDate.parse(s.trim(), DATA_BR); }
                catch (Exception e) { return service.Conversor.parseData(s); }
            }
        });
        return dp;
    }

    /** Troca os itens do combo mantendo a seleção atual, se ela ainda existir. */
    public static <T> void recarregar(ComboBox<T> combo, java.util.List<T> itens) {
        T selecionado = combo.getValue();
        combo.getItems().setAll(itens);
        combo.setValue(selecionado != null && itens.contains(selecionado) ? selecionado : null);
    }

    public static void mostrarMsg(Label lbl, String texto, boolean sucesso) {
        lbl.setText(texto);
        lbl.setStyle("-fx-text-fill: " + (sucesso ? COR_RECEITA : COR_DESPESA) + ";");
    }

    public static boolean confirmar(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensagem, ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        return alerta.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    public static boolean confirmarExclusao(String oQue) {
        return confirmar("Confirmar exclusão",
                "Deseja realmente excluir " + oQue + "?\nEssa ação não pode ser desfeita.");
    }

    // --- Colunas de TableView ---

    public static <T> TableColumn<T, String> colunaTexto(String titulo, Function<T, String> valor, double largura) {
        TableColumn<T, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        col.setPrefWidth(largura);
        return col;
    }

    /** Coluna de valor em R$, alinhada à direita e ordenável numericamente. */
    public static <T> TableColumn<T, BigDecimal> colunaValor(String titulo, Function<T, BigDecimal> valor,
                                                             double largura, String cor) {
        TableColumn<T, BigDecimal> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean vazio) {
                super.updateItem(v, vazio);
                setText(vazio || v == null ? null : Dinheiro.formatar(v));
                setAlignment(Pos.CENTER_RIGHT);
                String corTexto = cor != null ? cor : (v != null && v.signum() < 0 ? COR_DESPESA : null);
                setStyle(corTexto != null && !vazio ? "-fx-text-fill: " + corTexto + ";" : "");
            }
        });
        col.setPrefWidth(largura);
        return col;
    }

    /** Coluna de data dd/mm/aaaa, ordenável cronologicamente. */
    public static <T> TableColumn<T, LocalDate> colunaData(String titulo, Function<T, LocalDate> valor, double largura) {
        TableColumn<T, LocalDate> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean vazio) {
                super.updateItem(d, vazio);
                setText(vazio || d == null ? null : DATA_BR.format(d));
            }
        });
        col.setPrefWidth(largura);
        return col;
    }

    public static <T> TableView<T> tabela(String textoVazio) {
        TableView<T> t = new TableView<>();
        t.getStyleClass().add("dark-table");
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label vazio = new Label(textoVazio);
        vazio.getStyleClass().add("empty-label");
        t.setPlaceholder(vazio);
        return t;
    }
}
