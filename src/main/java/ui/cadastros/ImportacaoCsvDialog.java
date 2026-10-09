package ui.cadastros;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.*;
import service.ControleFinanceiro;
import service.Conversor;
import ui.components.Ui;

import java.io.File;
import java.util.List;

/** Janela "Importar CSV — Despesas": escolhe arquivo e conta, mostra preview editável e grava. */
class ImportacaoCsvDialog {

    private final ControleFinanceiro cf;
    private final Runnable aoImportar;

    private final Stage stage = new Stage();
    private final TextField tfArquivo = new TextField();
    private final ComboBox<Conta> cbConta = new ComboBox<>();
    private final TableView<LinhaImportacao> tabela = Ui.tabela("Nenhum dado carregado.");
    private final Button btnConfirmar = new Button("Confirmar Importação");
    private final Label lblStatus = new Label("Selecione um arquivo CSV para começar.");
    /** Conta usada no preview (a checagem de duplicatas foi feita contra ela). */
    private Conta contaDoPreview;

    ImportacaoCsvDialog(ControleFinanceiro cf, Runnable aoImportar) {
        this.cf = cf;
        this.aoImportar = aoImportar;
    }

    void abrir(Stage owner) {
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        Ui.comIcone(stage);
        stage.setTitle("Importar CSV — Despesas");
        stage.setMinWidth(980);
        stage.setMinHeight(600);

        tfArquivo.setPromptText("Caminho do arquivo CSV...");
        tfArquivo.setPrefWidth(380);
        tfArquivo.getStyleClass().add("dark-field");
        tfArquivo.setEditable(false);

        Button btnSelecionar = new Button("Selecionar CSV");
        btnSelecionar.getStyleClass().add("btn-secondary");
        btnSelecionar.setOnAction(e -> selecionarArquivo());

        cbConta.getItems().addAll(cf.getContas());
        cbConta.setPromptText("Conta de débito");
        cbConta.getStyleClass().add("dark-combo");
        cbConta.setOnAction(e -> {
            if (contaDoPreview != null && !contaDoPreview.equals(cbConta.getValue())) {
                btnConfirmar.setDisable(true);
                Ui.mostrarMsg(lblStatus, "Conta alterada: clique em \"Carregar Preview\" novamente.", false);
            }
        });

        Button btnCarregar = new Button("Carregar Preview");
        btnCarregar.getStyleClass().add("btn-primary");
        btnCarregar.setOnAction(e -> carregar());

        HBox topo = new HBox(10, tfArquivo, btnSelecionar, cbConta, btnCarregar);
        topo.setAlignment(Pos.CENTER_LEFT);

        configurarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);

        lblStatus.getStyleClass().add("form-label");
        btnConfirmar.getStyleClass().add("btn-primary");
        btnConfirmar.setDisable(true);
        btnConfirmar.setOnAction(e -> confirmar());
        Button btnFechar = new Button("Fechar");
        btnFechar.getStyleClass().add("btn-secondary");
        btnFechar.setOnAction(e -> stage.close());
        HBox rodape = new HBox(10, btnConfirmar, btnFechar, lblStatus);
        rodape.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, topo, new Separator(), tabela, rodape);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");

        Scene scene = new Scene(root, 1040, 640);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void configurarTabela() {
        tabela.setEditable(true);
        tabela.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<LinhaImportacao, Boolean> colImportar = new TableColumn<>("Importar");
        colImportar.setCellValueFactory(c -> c.getValue().importarProperty());
        colImportar.setCellFactory(CheckBoxTableCell.forTableColumn(colImportar));
        colImportar.setPrefWidth(70);

        TableColumn<LinhaImportacao, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> new SimpleStringProperty(Ui.DATA_BR.format(c.getValue().getData())));
        colData.setCellFactory(TextFieldTableCell.forTableColumn());
        colData.setOnEditCommit(e -> {
            try {
                e.getRowValue().setData(Conversor.parseData(e.getNewValue()));
            } catch (Exception ex) {
                Ui.mostrarMsg(lblStatus, "✖ " + ex.getMessage(), false);
            }
            tabela.refresh();
        });
        colData.setPrefWidth(100);

        TableColumn<LinhaImportacao, String> colTitulo = new TableColumn<>("Descrição Original");
        colTitulo.setCellValueFactory(c -> c.getValue().tituloProperty());
        colTitulo.setCellFactory(TextFieldTableCell.forTableColumn());
        colTitulo.setOnEditCommit(e -> e.getRowValue().setTitulo(e.getNewValue()));
        colTitulo.setPrefWidth(240);

        TableColumn<LinhaImportacao, java.math.BigDecimal> colValor =
                Ui.colunaValor("Valor", LinhaImportacao::getValor, 100, null);
        colValor.setEditable(false);

        TableColumn<LinhaImportacao, Categoria> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(c -> c.getValue().categoriaProperty());
        colCategoria.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(cf.getCategorias())));
        colCategoria.setOnEditCommit(e -> e.getRowValue().setCategoria(e.getNewValue()));
        colCategoria.setPrefWidth(130);

        TableColumn<LinhaImportacao, String> colDetalhe = new TableColumn<>("Detalhe");
        colDetalhe.setCellValueFactory(c -> c.getValue().detalheProperty());
        colDetalhe.setCellFactory(TextFieldTableCell.forTableColumn());
        colDetalhe.setOnEditCommit(e -> e.getRowValue().setDetalhe(e.getNewValue()));
        colDetalhe.setPrefWidth(180);

        TableColumn<LinhaImportacao, String> colMapeamento = Ui.colunaTexto("Mapeamento",
                l -> l.isMapeamentoNovo() ? "⚠ Novo" : "✔ Existente", 100);
        colMapeamento.setEditable(false);

        TableColumn<LinhaImportacao, String> colObs = new TableColumn<>("Observação");
        colObs.setCellValueFactory(c -> c.getValue().observacaoProperty());
        colObs.setEditable(false);
        colObs.setPrefWidth(110);

        tabela.getColumns().addAll(List.of(colImportar, colData, colTitulo, colValor,
                colCategoria, colDetalhe, colMapeamento, colObs));
    }

    private void selecionarArquivo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showOpenDialog(stage);
        if (f != null) tfArquivo.setText(f.getAbsolutePath());
    }

    private void carregar() {
        String caminho = tfArquivo.getText().trim();
        if (caminho.isEmpty()) { Ui.mostrarMsg(lblStatus, "Selecione um arquivo.", false); return; }
        if (cbConta.getValue() == null) { Ui.mostrarMsg(lblStatus, "Selecione uma conta.", false); return; }
        try {
            ResultadoLeituraCsv resultado = cf.lerCsv(caminho, cbConta.getValue().getId());
            List<LinhaImportacao> linhas = resultado.linhas();
            contaDoPreview = cbConta.getValue();
            tabela.setItems(FXCollections.observableArrayList(linhas));
            btnConfirmar.setDisable(linhas.isEmpty());

            long marcadas = linhas.stream().filter(LinhaImportacao::isImportar).count();
            long semCat = linhas.stream().filter(l -> l.isImportar() && l.getCategoria() == null).count();
            long novos = linhas.stream().filter(LinhaImportacao::isMapeamentoNovo).count();
            long desmarcadas = linhas.size() - marcadas;
            List<String> erros = resultado.erros();
            String msg = linhas.size() + " linha(s), " + marcadas + " marcada(s). " + novos + " novo(s) mapeamento(s)."
                    + (desmarcadas > 0 ? " " + desmarcadas + " desmarcada(s) (estorno ou já importada)." : "")
                    + (semCat > 0 ? " ⚠ " + semCat + " sem categoria." : "")
                    + (erros.isEmpty() ? "" : " ⚠ " + erros.size() + " linha(s) ignorada(s) com erro.");
            Ui.mostrarMsg(lblStatus, msg, semCat == 0 && erros.isEmpty());

            if (!erros.isEmpty()) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.initOwner(stage);
                Ui.comIcone(alerta);
                alerta.setTitle("Linhas ignoradas");
                alerta.setHeaderText(erros.size() + " linha(s) do CSV não puderam ser lidas:");
                alerta.setContentText(String.join("\n", erros.subList(0, Math.min(15, erros.size())))
                        + (erros.size() > 15 ? "\n…" : ""));
                alerta.show();
            }
        } catch (Exception ex) {
            Ui.mostrarMsg(lblStatus, "✖ Não foi possível ler o arquivo: " + ex.getMessage(), false);
        }
    }

    private void confirmar() {
        long semCat = tabela.getItems().stream().filter(l -> l.isImportar() && l.getCategoria() == null).count();
        if (semCat > 0) {
            Ui.mostrarMsg(lblStatus, "✖ " + semCat + " linha(s) sem categoria. Preencha ou desmarque.", false);
            return;
        }
        try {
            int importados = cf.confirmarImportacao(tabela.getItems(), contaDoPreview.getId());
            Ui.mostrarMsg(lblStatus, "✔ " + importados + " despesa(s) importada(s)!", true);
            tabela.getItems().clear();
            btnConfirmar.setDisable(true);
            aoImportar.run();
        } catch (Exception ex) {
            Ui.mostrarMsg(lblStatus, "✖ " + ex.getMessage(), false);
        }
    }
}
