package ui.cadastros;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.*;
import service.ControleFinanceiro;

import java.time.LocalDate;
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

        Button btnSalvar = new Button("Salvar Despesa");
        btnSalvar.getStyleClass().add("btn-danger");
        Label lblMsg = new Label();

        TableView<Despesa> tabela = new TableView<>();
        tabela.getStyleClass().add("dark-table");
        tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getDespesas()));

        adicionarColuna(tabela, "ID",         "id",            60);
        adicionarColuna(tabela, "Categoria",  "categoriaNome",130);
        adicionarColuna(tabela, "Detalhe",    "detalhamento", 180);
        adicionarColuna(tabela, "Valor",      "valor",        100);
        adicionarColuna(tabela, "Conta",      "contaNome",    120);
        adicionarColuna(tabela, "Data",       "data",         110);

        Button btnExcluir = new Button("Excluir Selecionada");
        btnExcluir.getStyleClass().add("btn-danger");

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
                cf.salvarDespesa(new Despesa(cat.getId(), det, valor, conta.getId(), data, mes, data.getYear()));
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getDespesas()));
                tfDetalhe.clear(); tfValor.clear();
                lblMsg.setText("✔ Despesa salva!"); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
            } catch (Exception ex) {
                lblMsg.setText("✖ " + ex.getMessage()); lblMsg.setStyle("-fx-text-fill: #E85C4A;");
            }
        });

        btnExcluir.setOnAction(e -> {
            Despesa sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { lblMsg.setText("Selecione uma despesa."); return; }
            cf.excluirDespesa(sel.getId());
            tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getDespesas()));
            lblMsg.setText("✔ Excluída."); lblMsg.setStyle("-fx-text-fill: #4AE87A;");
        });

        VBox.setVgrow(tabela, Priority.ALWAYS);
        aba.getChildren().addAll(form, new HBox(10, btnSalvar, lblMsg), new Separator(), btnExcluir, tabela);
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
            LocalDate hoje = LocalDate.now();
            String mes = hoje.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt","BR")).toUpperCase();
            int aplicados = cf.aplicarFixosMes(mes, hoje.getYear());
            lblMsg.setText("✔ " + aplicados + " fixo(s) aplicado(s) em " + mes + "/" + hoje.getYear());
            lblMsg.setStyle("-fx-text-fill: #4AE87A;");
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
}
