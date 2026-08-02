package ui.cadastros;

import model.LinhaImportacao;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Dialog;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.*;
import service.ControleFinanceiro;

import java.time.format.TextStyle;
import java.util.Locale;

public class CadastrosView {

    private final ControleFinanceiro cf = new ControleFinanceiro();

    public VBox getView() {
        VBox root = new VBox(0);
        root.getStyleClass().add("main-content");

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("cadastros-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
            new Tab("Receitas",       abaReceitas()),
            new Tab("Despesas",       abaDespesas()),
            new Tab("Investimentos",  abaInvestimentos()),
            new Tab("Fixos",          abaFixos()),
            new Tab("Categorias",     abaCategorias()),
            new Tab("Contas",         abaContas())
        );

        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.getChildren().add(tabs);
        return root;
    }

    // -------------------------------------------------------------------------
    // ABA RECEITAS
    // -------------------------------------------------------------------------

    private VBox abaReceitas() {
        VBox aba = new VBox(12);
        aba.setPadding(new Insets(16));

        // Formulário
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);

        TextField tfOrigem = campo("Ex: Salário, Freelance");
        TextField tfValor  = campo("Ex: 1500,00");
        ComboBox<Conta> cbConta = new ComboBox<>();
        cbConta.getItems().addAll(cf.getContas());
        cbConta.setPromptText("Selecione a conta");
        DatePicker dpData = new DatePicker(LocalDate.now());

        form.addRow(0, label("Origem/Descrição:"), tfOrigem);
        form.addRow(1, label("Valor R$:"),         tfValor);
        form.addRow(2, label("Conta:"),             cbConta);
        form.addRow(3, label("Data:"),              dpData);

        Button btnSalvar = new Button("Salvar Receita");
        btnSalvar.getStyleClass().add("btn-primary");

        Label lblMsg = new Label();
        lblMsg.getStyleClass().add("msg-label");

        // Tabela
        TableView<Receita> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");
        tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getReceitas()));

        adicionarColuna(tabela, "ID",      "id",        60);
        adicionarColuna(tabela, "Origem",  "origem",   200);
        adicionarColuna(tabela, "Valor",   "valor",    100);
        adicionarColuna(tabela, "Conta",   "contaNome",130);
        adicionarColuna(tabela, "Data",    "data",     110);
        adicionarColuna(tabela, "Mês",     "mes",       90);

        Button btnExcluir = new Button("Excluir Selecionada");
        btnExcluir.getStyleClass().add("btn-danger");

        btnSalvar.setOnAction(e -> {
            try {
                String origem = tfOrigem.getText().trim();
                if (origem.isEmpty()) throw new RuntimeException("Origem é obrigatória.");
                double valor = Double.parseDouble(tfValor.getText().trim().replace(",", "."));
                if (valor <= 0) throw new RuntimeException("Valor deve ser maior que zero.");
                Conta conta = cbConta.getValue();
                if (conta == null) throw new RuntimeException("Selecione uma conta.");
                LocalDate data = dpData.getValue();
                if (data == null) throw new RuntimeException("Selecione uma data.");
                String mes = data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt","BR")).toUpperCase();
                cf.salvarReceita(new Receita(origem, valor, conta.getId(), data, mes, data.getYear()));
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getReceitas()));
                tfOrigem.clear(); tfValor.clear();
                lblMsg.setText("✔ Receita salva!"); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        btnExcluir.setOnAction(e -> {
            Receita sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { lblMsg.setText("Selecione uma receita."); return; }
            cf.excluirReceita(sel.getId());
            tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getReceitas()));
            lblMsg.setText("✔ Excluída."); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
        });

        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().addAll(form, new HBox(10, btnSalvar, lblMsg), new Separator(), btnExcluir, tabela);
        return aba;
    }

    // -------------------------------------------------------------------------
    // ABA DESPESAS
    // -------------------------------------------------------------------------

    private VBox abaDespesas() {
        VBox aba = new VBox(12);
        aba.setPadding(new Insets(16));

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);

        ComboBox<Categoria> cbCat = new ComboBox<>();
        cbCat.getItems().addAll(cf.getCategorias());
        cbCat.setPromptText("Selecione a categoria");
        TextField tfDetalhe = campo("Ex: Supermercado, Netflix");
        TextField tfValor   = campo("Ex: 350,00");
        ComboBox<Conta> cbConta = new ComboBox<>();
        cbConta.getItems().addAll(cf.getContas());
        cbConta.setPromptText("Selecione a conta");
        DatePicker dpData = new DatePicker(LocalDate.now());

        form.addRow(0, label("Categoria:"),    cbCat);
        form.addRow(1, label("Detalhamento:"), tfDetalhe);
        form.addRow(2, label("Valor R$:"),     tfValor);
        form.addRow(3, label("Conta:"),        cbConta);
        form.addRow(4, label("Data:"),         dpData);

        Button btnSalvar   = new Button("Salvar Despesa");
        Button btnCancelar = new Button("Cancelar Edição");
        Label  lblMsg      = new Label();
        btnSalvar.getStyleClass().add("btn-danger");
        btnCancelar.getStyleClass().add("btn-secondary");
        btnCancelar.setVisible(false);

        TableView<Despesa> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");
        tabela.setPlaceholder(new Label("Nenhuma despesa cadastrada."));
        tabela.setItems(FXCollections.observableArrayList(cf.getDespesas()));

        adicionarColuna(tabela, "ID",        "id",            60);
        adicionarColuna(tabela, "Categoria", "categoriaNome",130);
        adicionarColuna(tabela, "Detalhe",   "detalhamento", 180);
        adicionarColuna(tabela, "Valor",     "valor",        100);
        adicionarColuna(tabela, "Conta",     "contaNome",    120);
        adicionarColuna(tabela, "Data",      "data",         110);

        Button btnEditar  = new Button("Editar Selecionada");
        Button btnExcluir = new Button("Excluir Selecionada");
        Button btnImportar = new Button("Importar CSV");
        btnEditar.getStyleClass().add("btn-info");
        btnExcluir.getStyleClass().add("btn-danger");
        btnImportar.getStyleClass().add("btn-info");

        final int[] idEmEdicao = {-1};

        btnEditar.setOnAction(e -> {
            Despesa sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma despesa para editar.", false); return; }
            idEmEdicao[0] = sel.getId();
            tfDetalhe.setText(sel.getDetalhamento());
            tfValor.setText(String.format("%.2f", sel.getValor()).replace(".", ","));
            dpData.setValue(sel.getData());
            cf.getCategorias().stream()
                    .filter(c -> c.getId() == sel.getCategoriaId())
                    .findFirst().ifPresent(cbCat::setValue);
            cf.getContas().stream()
                    .filter(c -> c.getId() == sel.getContaId())
                    .findFirst().ifPresent(cbConta::setValue);
            btnSalvar.setText("Atualizar Despesa");
            btnCancelar.setVisible(true);
            mostrarMsg(lblMsg, "Editando despesa ID " + idEmEdicao[0], true);
        });

        btnCancelar.setOnAction(e -> {
            idEmEdicao[0] = -1;
            tfDetalhe.clear(); tfValor.clear();
            cbCat.setValue(null); cbConta.setValue(null); dpData.setValue(LocalDate.now());
            btnSalvar.setText("Salvar Despesa");
            btnCancelar.setVisible(false);
            lblMsg.setText("");
        });

        btnSalvar.setOnAction(e -> {
            try {
                Categoria cat = cbCat.getValue();
                if (cat == null) throw new RuntimeException("Selecione uma categoria.");
                String det = tfDetalhe.getText().trim();
                if (det.isEmpty()) throw new RuntimeException("Detalhamento é obrigatório.");
                double valor = Double.parseDouble(tfValor.getText().trim().replace(",", "."));
                if (valor <= 0) throw new RuntimeException("Valor deve ser maior que zero.");
                Conta conta = cbConta.getValue();
                if (conta == null) throw new RuntimeException("Selecione uma conta.");
                LocalDate data = dpData.getValue();
                String mes = data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt","BR")).toUpperCase();

                if (idEmEdicao[0] == -1) {
                    cf.salvarDespesa(new Despesa(cat.getId(), det, valor, conta.getId(), data, mes, data.getYear()));
                    mostrarMsg(lblMsg, "✔ Despesa salva!", true);
                } else {
                    cf.atualizarDespesa(new Despesa(idEmEdicao[0], cat.getId(), cat.getNome(),
                            det, valor, conta.getId(), conta.getNome(), data, mes, data.getYear()));
                    mostrarMsg(lblMsg, "✔ Despesa atualizada!", true);
                    idEmEdicao[0] = -1;
                    btnSalvar.setText("Salvar Despesa");
                    btnCancelar.setVisible(false);
                }
                tabela.setItems(FXCollections.observableArrayList(cf.getDespesas()));
                tfDetalhe.clear(); tfValor.clear();
                cbCat.setValue(null); cbConta.setValue(null); dpData.setValue(LocalDate.now());
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
        });

        btnExcluir.setOnAction(e -> {
            Despesa sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma despesa.", false); return; }
            cf.excluirDespesa(sel.getId());
            tabela.setItems(FXCollections.observableArrayList(cf.getDespesas()));
            mostrarMsg(lblMsg, "✔ Excluída.", true);
        });

        // Abre janela modal de importação CSV
        btnImportar.setOnAction(e ->
                abrirImportacao((Stage) btnImportar.getScene().getWindow(), tabela));

        HBox acoes = new HBox(10, btnSalvar, btnCancelar, lblMsg);
        acoes.setAlignment(Pos.CENTER_LEFT);
        HBox botoesTabela = new HBox(10, btnEditar, btnExcluir, btnImportar);

        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().addAll(form, acoes, new Separator(), botoesTabela, tabela);
        return aba;
    }

    // -------------------------------------------------------------------------
    // ABA INVESTIMENTOS
    // -------------------------------------------------------------------------

    private VBox abaInvestimentos() {
        VBox aba = new VBox(12);
        aba.setPadding(new Insets(16));

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);

        TextField tfTipo  = campo("Ex: Poupança, Tesouro Direto, CDB");
        TextField tfValor = campo("Ex: 500,00");
        ComboBox<Conta> cbConta = new ComboBox<>();
        cbConta.getItems().addAll(cf.getContas());
        cbConta.setPromptText("Selecione a conta");
        DatePicker dpData = new DatePicker(LocalDate.now());

        form.addRow(0, label("Tipo:"),     tfTipo);
        form.addRow(1, label("Valor R$:"), tfValor);
        form.addRow(2, label("Conta:"),    cbConta);
        form.addRow(3, label("Data:"),     dpData);

        Button btnSalvar = new Button("Salvar Investimento");
        btnSalvar.getStyleClass().add("btn-info");
        Label lblMsg = new Label();

        TableView<Investimento> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");
        tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getInvestimentos()));

        adicionarColuna(tabela, "ID",    "id",        60);
        adicionarColuna(tabela, "Tipo",  "tipo",     200);
        adicionarColuna(tabela, "Valor", "valor",    100);
        adicionarColuna(tabela, "Conta", "contaNome",130);
        adicionarColuna(tabela, "Data",  "data",     110);

        Button btnExcluir = new Button("Excluir Selecionado");
        btnExcluir.getStyleClass().add("btn-danger");

        btnSalvar.setOnAction(e -> {
            try {
                String tipo = tfTipo.getText().trim();
                if (tipo.isEmpty()) throw new RuntimeException("Tipo é obrigatório.");
                double valor = Double.parseDouble(tfValor.getText().trim().replace(",", "."));
                Conta conta = cbConta.getValue();
                if (conta == null) throw new RuntimeException("Selecione uma conta.");
                LocalDate data = dpData.getValue();
                String mes = data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt","BR")).toUpperCase();
                cf.salvarInvestimento(new Investimento(tipo, valor, conta.getId(), data, mes, data.getYear()));
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getInvestimentos()));
                tfTipo.clear(); tfValor.clear();
                lblMsg.setText("✔ Salvo!"); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        btnExcluir.setOnAction(e -> {
            Investimento sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            cf.excluirInvestimento(sel.getId());
            tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getInvestimentos()));
        });

        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().addAll(form, new HBox(10, btnSalvar, lblMsg), new Separator(), btnExcluir, tabela);
        return aba;
    }

    // -------------------------------------------------------------------------
    // ABA FIXOS
    // -------------------------------------------------------------------------

    private VBox abaFixos() {
        VBox aba = new VBox(12);
        aba.setPadding(new Insets(16));

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);

        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll("RECEITA", "DESPESA", "INVESTIMENTO");
        cbTipo.setValue("DESPESA");
        TextField tfDesc = campo("Ex: Condomínio, Salário");
        ComboBox<Categoria> cbCat = new ComboBox<>();
        cbCat.getItems().addAll(cf.getCategorias());
        cbCat.setPromptText("Categoria (apenas Despesa)");
        TextField tfValor = campo("Ex: 850,00");
        ComboBox<Conta> cbConta = new ComboBox<>();
        cbConta.getItems().addAll(cf.getContas());
        cbConta.setPromptText("Selecione a conta");
        Spinner<Integer> spDia = new Spinner<>(1, 31, 5);

        form.addRow(0, label("Tipo:"),        cbTipo);
        form.addRow(1, label("Descrição:"),   tfDesc);
        form.addRow(2, label("Categoria:"),   cbCat);
        form.addRow(3, label("Valor R$:"),    tfValor);
        form.addRow(4, label("Conta:"),       cbConta);
        form.addRow(5, label("Dia do mês:"),  spDia);

        cbTipo.setOnAction(e -> cbCat.setDisable(!"DESPESA".equals(cbTipo.getValue())));

        Button btnSalvar  = new Button("Salvar Fixo");
        btnSalvar.getStyleClass().add("btn-primary");
        Button btnAplicar = new Button("Aplicar Mês Atual");
        btnAplicar.getStyleClass().add("btn-info");
        Label lblMsg = new Label();

        TableView<LancamentoFixo> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");
        tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getLancamentosFixos()));

        adicionarColuna(tabela, "ID",       "id",           50);
        adicionarColuna(tabela, "Tipo",     "tipo",         110);
        adicionarColuna(tabela, "Descrição","descricao",    180);
        adicionarColuna(tabela, "Valor",    "valor",         90);
        adicionarColuna(tabela, "Conta",    "contaNome",    120);
        adicionarColuna(tabela, "Dia",      "diaVencimento", 50);
        adicionarColuna(tabela, "Ativo",    "ativo",         60);

        Button btnExcluir  = new Button("Excluir");
        Button btnAlternar = new Button("Ativar/Desativar");
        btnExcluir.getStyleClass().add("btn-danger");
        btnAlternar.getStyleClass().add("btn-secondary");

        btnSalvar.setOnAction(e -> {
            try {
                LancamentoFixo.Tipo tipo = LancamentoFixo.Tipo.valueOf(cbTipo.getValue());
                String desc = tfDesc.getText().trim();
                if (desc.isEmpty()) throw new RuntimeException("Descrição obrigatória.");
                int catId = tipo == LancamentoFixo.Tipo.DESPESA && cbCat.getValue() != null
                    ? cbCat.getValue().getId() : 0;
                double valor = Double.parseDouble(tfValor.getText().trim().replace(",", "."));
                Conta conta = cbConta.getValue();
                if (conta == null) throw new RuntimeException("Selecione uma conta.");
                cf.salvarLancamentoFixo(new LancamentoFixo(tipo, desc, catId, valor, conta.getId(), spDia.getValue()));
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getLancamentosFixos()));
                tfDesc.clear(); tfValor.clear();
                lblMsg.setText("✔ Fixo salvo!"); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        btnAplicar.setOnAction(e -> {
            // Diálogo de intervalo
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Aplicar Fixos por Intervalo");
            dialog.setHeaderText("Selecione o período:");

            ComboBox<String> cbMesIni = new ComboBox<>();
            cbMesIni.getItems().addAll(ControleFinanceiro.MESES);
            cbMesIni.setValue(LocalDate.now().getMonth()
                    .getDisplayName(TextStyle.FULL, new Locale("pt","BR")).toUpperCase());

            Spinner<Integer> spAnoIni = new Spinner<>(2000, 2100, LocalDate.now().getYear());

            ComboBox<String> cbMesFim = new ComboBox<>();
            cbMesFim.getItems().addAll(ControleFinanceiro.MESES);
            cbMesFim.setValue(LocalDate.now().getMonth()
                    .getDisplayName(TextStyle.FULL, new Locale("pt","BR")).toUpperCase());

            Spinner<Integer> spAnoFim = new Spinner<>(2000, 2100, LocalDate.now().getYear());

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.setPadding(new Insets(10));
            grid.addRow(0, new Label("Mês inicial:"), cbMesIni, new Label("Ano:"), spAnoIni);
            grid.addRow(1, new Label("Mês final:"),   cbMesFim, new Label("Ano:"), spAnoFim);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        int aplicados = cf.aplicarFixosIntervalo(
                                cbMesIni.getValue(), spAnoIni.getValue(),
                                cbMesFim.getValue(), spAnoFim.getValue()
                        );
                        mostrarMsg(lblMsg, "✔ " + aplicados + " fixo(s) aplicado(s) no intervalo!", true);
                    } catch (Exception ex) {
                        mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
                    }
                }
            });
        });

        btnExcluir.setOnAction(e -> {
            LancamentoFixo sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            cf.excluirLancamentoFixo(sel.getId());
            tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getLancamentosFixos()));
        });

        btnAlternar.setOnAction(e -> {
            LancamentoFixo sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            cf.alternarAtivoFixo(sel.getId());
            tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getLancamentosFixos()));
        });

        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().addAll(form, new HBox(10, btnSalvar, btnAplicar, lblMsg),
            new Separator(), new HBox(10, btnExcluir, btnAlternar), tabela);
        return aba;
    }

    // -------------------------------------------------------------------------
    // ABA CATEGORIAS
    // -------------------------------------------------------------------------

    private VBox abaCategorias() {
        VBox aba = new VBox(12);
        aba.setPadding(new Insets(16));

        TextField tfNome = campo("Nome da categoria");
        Button btnSalvar = new Button("Adicionar");
        btnSalvar.getStyleClass().add("btn-primary");
        Label lblMsg = new Label();

        TableView<Categoria> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");
        tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getCategorias()));
        adicionarColuna(tabela, "ID",   "id",    60);
        adicionarColuna(tabela, "Nome", "nome", 300);

        Button btnExcluir = new Button("Excluir Selecionada");
        btnExcluir.getStyleClass().add("btn-danger");

        btnSalvar.setOnAction(e -> {
            try {
                String nome = tfNome.getText().trim();
                if (nome.isEmpty()) throw new RuntimeException("Nome obrigatório.");
                cf.salvarCategoria(new Categoria(nome));
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getCategorias()));
                tfNome.clear();
                lblMsg.setText("✔ Salva!"); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        btnExcluir.setOnAction(e -> {
            Categoria sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            try {
                cf.excluirCategoria(sel.getId());
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getCategorias()));
                lblMsg.setText("✔ Excluída."); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().addAll(new HBox(10, tfNome, btnSalvar, lblMsg), new Separator(), btnExcluir, tabela);
        return aba;
    }

    // -------------------------------------------------------------------------
    // ABA CONTAS
    // -------------------------------------------------------------------------

    private VBox abaContas() {
        VBox aba = new VBox(12);
        aba.setPadding(new Insets(16));

        TextField tfNome = campo("Nome da conta (ex: Nubank, Itaú)");
        Button btnSalvar = new Button("Adicionar");
        btnSalvar.getStyleClass().add("btn-primary");
        Label lblMsg = new Label();

        TableView<Conta> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");
        tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getContas()));
        adicionarColuna(tabela, "ID",   "id",    60);
        adicionarColuna(tabela, "Nome", "nome", 300);

        Button btnExcluir = new Button("Excluir Selecionada");
        btnExcluir.getStyleClass().add("btn-danger");

        btnSalvar.setOnAction(e -> {
            try {
                String nome = tfNome.getText().trim();
                if (nome.isEmpty()) throw new RuntimeException("Nome obrigatório.");
                cf.salvarConta(new Conta(nome));
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getContas()));
                tfNome.clear();
                lblMsg.setText("✔ Salva!"); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        btnExcluir.setOnAction(e -> {
            Conta sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            try {
                cf.excluirConta(sel.getId());
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getContas()));
                lblMsg.setText("✔ Excluída."); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().addAll(new HBox(10, tfNome, btnSalvar, lblMsg), new Separator(), btnExcluir, tabela);
        return aba;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("dark-field");
        tf.setPrefWidth(260);
        return tf;
    }

    private Label label(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("form-label");
        return l;
    }

    private <T, V> void adicionarColuna(TableView<T> tabela, String titulo, String propriedade, double largura) {
        TableColumn<T, V> col = new TableColumn<>(titulo);
        col.setCellValueFactory(new PropertyValueFactory<>(propriedade));
        col.setPrefWidth(largura);
        tabela.getColumns().add(col);
    }

    private void mostrarMsg(Label lbl, String texto, boolean sucesso) {
        lbl.setText(texto);
        lbl.setStyle(sucesso ? "-fx-text-fill: #4AE87A;" : "-fx-text-fill: #E85C4A;");
    }

    private void abrirImportacao(Stage owner, TableView<Despesa> tabela) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Importar CSV — Despesas");
        stage.setMinWidth(980);
        stage.setMinHeight(600);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");

        // Seleção de arquivo e conta
        TextField tfArquivo = new TextField();
        tfArquivo.setPromptText("Caminho do arquivo CSV...");
        tfArquivo.setPrefWidth(380);
        tfArquivo.getStyleClass().add("dark-field");
        tfArquivo.setEditable(false);

        Button btnSelecionar = new Button("Selecionar CSV");
        btnSelecionar.getStyleClass().add("btn-secondary");
        btnSelecionar.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Selecionar CSV");
            fc.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
            java.io.File f = fc.showOpenDialog(stage);
            if (f != null) tfArquivo.setText(f.getAbsolutePath());
        });

        ComboBox<Conta> cbConta = new ComboBox<>();
        cbConta.getItems().addAll(cf.getContas());
        cbConta.setPromptText("Conta de débito");
        cbConta.getStyleClass().add("dark-combo");

        Button btnCarregar = new Button("Carregar Preview");
        btnCarregar.getStyleClass().add("btn-primary");

        HBox topoForm = new HBox(10, tfArquivo, btnSelecionar, cbConta, btnCarregar);
        topoForm.setAlignment(Pos.CENTER_LEFT);

        Label lblStatus = new Label("Selecione um arquivo CSV para começar.");
        lblStatus.getStyleClass().add("form-label");

        // Tabela de preview
        TableView<LinhaImportacao> tabelaPreview = new TableView<>();
        tabelaPreview.setEditable(true);
        tabelaPreview.getStyleClass().add("dark-table");
        tabelaPreview.setPlaceholder(new Label("Nenhum dado carregado."));
        VBox.setVgrow(tabelaPreview, Priority.ALWAYS);

        TableColumn<LinhaImportacao, Boolean> colImportar = new TableColumn<>("Importar");
        colImportar.setCellValueFactory(c -> c.getValue().importarProperty());
        colImportar.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(colImportar));
        colImportar.setPrefWidth(70);
        colImportar.setEditable(true);

        TableColumn<LinhaImportacao, LocalDate> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> c.getValue().dataProperty());
        colData.setPrefWidth(100);

        TableColumn<LinhaImportacao, String> colTitulo = new TableColumn<>("Descrição Original");
        colTitulo.setCellValueFactory(c -> c.getValue().tituloProperty());
        colTitulo.setPrefWidth(240);

        TableColumn<LinhaImportacao, Double> colValor = new TableColumn<>("Valor R$");
        colValor.setCellValueFactory(c -> c.getValue().valorProperty().asObject());
        colValor.setPrefWidth(90);

        TableColumn<LinhaImportacao, Categoria> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(c -> c.getValue().categoriaProperty());
        colCategoria.setCellFactory(javafx.scene.control.cell.ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList(cf.getCategorias())));
        colCategoria.setOnEditCommit(e -> e.getRowValue().setCategoria(e.getNewValue()));
        colCategoria.setPrefWidth(130);
        colCategoria.setEditable(true);

        TableColumn<LinhaImportacao, String> colDetalhe = new TableColumn<>("Detalhe");
        colDetalhe.setCellValueFactory(c -> c.getValue().detalheProperty());
        colDetalhe.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colDetalhe.setOnEditCommit(e -> e.getRowValue().setDetalhe(e.getNewValue()));
        colDetalhe.setPrefWidth(190);
        colDetalhe.setEditable(true);

        TableColumn<LinhaImportacao, String> colStatus = new TableColumn<>("Mapeamento");
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().isMapeamentoNovo() ? "⚠ Novo" : "✔ Existente"));
        colStatus.setPrefWidth(100);

        tabelaPreview.getColumns().addAll(
                colImportar, colData, colTitulo, colValor, colCategoria, colDetalhe, colStatus);

        // Botões rodapé
        Button btnConfirmar = new Button("Confirmar Importação");
        btnConfirmar.getStyleClass().add("btn-primary");
        btnConfirmar.setDisable(true);

        Button btnFechar = new Button("Fechar");
        btnFechar.getStyleClass().add("btn-secondary");
        btnFechar.setOnAction(e -> {
            tabela.setItems(FXCollections.observableArrayList(cf.getDespesas()));
            stage.close();
        });

        HBox rodape = new HBox(10, btnConfirmar, btnFechar, lblStatus);
        rodape.setAlignment(Pos.CENTER_LEFT);

        // Ações
        btnCarregar.setOnAction(e -> {
            String caminho = tfArquivo.getText().trim();
            if (caminho.isEmpty()) { mostrarMsg(lblStatus, "Selecione um arquivo.", false); return; }
            if (cbConta.getValue() == null) { mostrarMsg(lblStatus, "Selecione uma conta.", false); return; }
            try {
                List<LinhaImportacao> linhas = cf.lerCsv(caminho);
                tabelaPreview.setItems(FXCollections.observableArrayList(linhas));
                btnConfirmar.setDisable(false);
                long semCat = linhas.stream().filter(l -> l.getCategoria() == null).count();
                long novos  = linhas.stream().filter(LinhaImportacao::isMapeamentoNovo).count();
                mostrarMsg(lblStatus, linhas.size() + " linha(s). " + novos + " novo(s) mapeamento(s)." +
                        (semCat > 0 ? " ⚠ " + semCat + " sem categoria." : ""), semCat == 0);
            } catch (Exception ex) {
                mostrarMsg(lblStatus, "✖ " + ex.getMessage(), false);
            }
        });

        btnConfirmar.setOnAction(e -> {
            long semCat = tabelaPreview.getItems().stream()
                    .filter(l -> l.isImportar() && l.getCategoria() == null).count();
            if (semCat > 0) {
                mostrarMsg(lblStatus, "✖ " + semCat + " linha(s) sem categoria. Preencha ou desmarque.", false);
                return;
            }
            int importados = cf.confirmarImportacao(tabelaPreview.getItems(), cbConta.getValue().getId());
            mostrarMsg(lblStatus, "✔ " + importados + " despesa(s) importada(s)!", true);
            btnConfirmar.setDisable(true);
        });

        root.getChildren().addAll(topoForm, new Separator(), tabelaPreview, rodape);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 980, 640);
        scene.getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
