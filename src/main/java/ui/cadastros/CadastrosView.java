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
import service.Conversor;


public class CadastrosView {

    private final ControleFinanceiro cf = new ControleFinanceiro();

    public VBox getView() {
        VBox root = new VBox(0);
        root.getStyleClass().add("main-content");

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("cadastros-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabReceitas      = new Tab("Receitas");
        Tab tabDespesas      = new Tab("Despesas");
        Tab tabInvestimentos = new Tab("Investimentos");
        Tab tabFixos         = new Tab("Fixos");
        Tab tabCategorias    = new Tab("Categorias");
        Tab tabContas        = new Tab("Contas");

        // Carrega o conteúdo inicial
        tabReceitas.setContent(abaReceitas());
        tabDespesas.setContent(abaDespesas());
        tabInvestimentos.setContent(abaInvestimentos());
        tabFixos.setContent(abaFixos());
        tabCategorias.setContent(abaCategorias());
        tabContas.setContent(abaContas());

        // Recarrega a aba de Despesas sempre que for selecionada
        tabDespesas.setOnSelectionChanged(e -> {
            if (tabDespesas.isSelected()) tabDespesas.setContent(abaDespesas());
        });

        // Recarrega a aba de Receitas (contas podem mudar)
        tabReceitas.setOnSelectionChanged(e -> {
            if (tabReceitas.isSelected()) tabReceitas.setContent(abaReceitas());
        });

        // Recarrega a aba de Fixos (categorias e contas podem mudar)
        tabFixos.setOnSelectionChanged(e -> {
            if (tabFixos.isSelected()) tabFixos.setContent(abaFixos());
        });

        // Recarrega a aba de Investimentos (contas podem mudar)
        tabInvestimentos.setOnSelectionChanged(e -> {
            if (tabInvestimentos.isSelected()) tabInvestimentos.setContent(abaInvestimentos());
        });

        tabs.getTabs().addAll(
                tabReceitas, tabDespesas, tabInvestimentos,
                tabFixos, tabCategorias, tabContas
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

        Button btnSalvar   = new Button("Salvar Receita");
        Button btnCancelar = new Button("Cancelar Edição");
        Label  lblMsg      = new Label();
        lblMsg.getStyleClass().add("msg-label");
        btnSalvar.getStyleClass().add("btn-primary");
        btnCancelar.getStyleClass().add("btn-secondary");
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);

        // --- Tabela estilizada ---
        List<String>  colsReceita = List.of("ID", "ORIGEM", "VALOR", "CONTA", "DATA", "MÊS");
        List<Integer> larsReceita = List.of(60, 200, 110, 130, 110, 90);

        VBox tabelaReceitas = criarTabelaEstilizada(colsReceita, larsReceita);
        ScrollPane scrollReceitas = new ScrollPane(tabelaReceitas);
        scrollReceitas.setFitToWidth(true);
        scrollReceitas.setFitToHeight(false);
        scrollReceitas.getStyleClass().add("main-scroll");
        VBox.setVgrow(scrollReceitas, Priority.ALWAYS);

        // --- Runnable para popular/recarregar a tabela ---
        Runnable recarregarReceitas = () -> {
            tabelaReceitas.getChildren().subList(1, tabelaReceitas.getChildren().size()).clear();
            List<Receita> receitas = cf.getReceitas();
            if (receitas.isEmpty()) {
                Label vazio = new Label("Nenhuma receita cadastrada.");
                vazio.getStyleClass().add("empty-label");
                vazio.setPadding(new Insets(10));
                tabelaReceitas.getChildren().add(vazio);
                return;
            }
            for (Receita r : receitas) {
                HBox linha = criarLinhaEstilizada(
                        List.of(
                                String.valueOf(r.getId()),
                                r.getOrigem(),
                                String.format("R$ %,.2f", r.getValor()),
                                r.getContaNome(),
                                r.getData().toString(),
                                r.getMes()
                        ),
                        larsReceita,
                        "#4AE87A"
                );
                linha.setUserData(r);
                linha.setOnMouseClicked(ev -> {
                    tabelaReceitas.getChildren().stream()
                            .filter(n -> n instanceof HBox && ((HBox) n).getUserData() instanceof Receita)
                            .forEach(n -> n.getStyleClass().remove("table-row-selected"));
                    linha.getStyleClass().add("table-row-selected");
                });
                tabelaReceitas.getChildren().add(linha);
            }
        };
        recarregarReceitas.run();

        // --- Botões ---
        Button btnEditar  = new Button("Editar Selecionada");
        Button btnExcluir = new Button("Excluir Selecionada");
        btnEditar.getStyleClass().add("btn-info");
        btnExcluir.getStyleClass().add("btn-danger");

        final int[] idEmEdicao = {-1};

        btnEditar.setOnAction(e -> {
            Receita sel = tabelaReceitas.getChildren().stream()
                    .filter(n -> n instanceof HBox && n.getStyleClass().contains("table-row-selected"))
                    .map(n -> (Receita) ((HBox) n).getUserData())
                    .findFirst().orElse(null);
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma receita para editar.", false); return; }
            idEmEdicao[0] = sel.getId();
//            cbConta.getItems().setAll(cf.getContas());
            tfOrigem.setText(sel.getOrigem());
            tfValor.setText(String.format("%.2f", sel.getValor()).replace(".", ","));
            dpData.setValue(sel.getData());
            cf.getContas().stream()
                    .filter(c -> c.getId() == sel.getContaId())
                    .findFirst().ifPresent(cbConta::setValue);
            btnSalvar.setText("Atualizar Receita");
            btnSalvar.getStyleClass().remove("btn-primary");
            btnSalvar.getStyleClass().add("btn-info");
            btnCancelar.setVisible(true);
            btnCancelar.setManaged(true);
            mostrarMsg(lblMsg, "Editando receita ID " + idEmEdicao[0], true);
        });

        btnCancelar.setOnAction(e -> {
            idEmEdicao[0] = -1;
            tfOrigem.clear(); tfValor.clear();
            cbConta.setValue(null); dpData.setValue(LocalDate.now());
            btnSalvar.setText("Salvar Receita");
            btnSalvar.getStyleClass().remove("btn-info");
            btnSalvar.getStyleClass().add("btn-primary");
            btnCancelar.setVisible(false);
            lblMsg.setText("");
        });

        btnSalvar.setOnAction(e -> {
            try {
                String origem = tfOrigem.getText().trim();
                if (origem.isEmpty()) throw new RuntimeException("Origem é obrigatória.");
                double valor = Conversor.parseValorPositivo(tfValor.getText());
                Conta conta = cbConta.getValue();
                if (conta == null) throw new RuntimeException("Selecione uma conta.");
                LocalDate data = dpData.getValue();
                if (data == null) throw new RuntimeException("Selecione uma data.");
                String mes = Conversor.nomeMes(data);

                if (idEmEdicao[0] == -1) {
                    cf.salvarReceita(new Receita(origem, valor, conta.getId(), data, mes, data.getYear()));
                    mostrarMsg(lblMsg, "✔ Receita salva!", true);
                } else {
                    cf.atualizarReceita(new Receita(idEmEdicao[0], origem, valor,
                            conta.getId(), conta.getNome(), data, mes, data.getYear()));
                    mostrarMsg(lblMsg, "✔ Receita atualizada!", true);
                    idEmEdicao[0] = -1;
                    btnSalvar.setText("Salvar Receita");
                    btnSalvar.getStyleClass().remove("btn-info");
                    btnSalvar.getStyleClass().add("btn-primary");
                    btnCancelar.setVisible(false);
                }
                recarregarReceitas.run();
                tfOrigem.clear(); tfValor.clear();
                cbConta.setValue(null); dpData.setValue(LocalDate.now());
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
        });

        btnExcluir.setOnAction(e -> {
            Receita sel = tabelaReceitas.getChildren().stream()
                    .filter(n -> n instanceof HBox && n.getStyleClass().contains("table-row-selected"))
                    .map(n -> (Receita) ((HBox) n).getUserData())
                    .findFirst().orElse(null);
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma receita.", false); return; }
            if (!confirmarExclusao("a receita \"" + sel.getOrigem() + "\"")) return;
            try {
                cf.excluirReceita(sel.getId());
                recarregarReceitas.run();
                mostrarMsg(lblMsg, "✔ Excluída.", true);
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
        });

        // Renomeia os botões
        btnEditar.setText("Editar");
        btnExcluir.setText("Excluir");

        HBox acoesEsquerda = new HBox(10, btnSalvar, btnCancelar);
        acoesEsquerda.setAlignment(Pos.CENTER_LEFT);

        HBox acoesDireita = new HBox(10, btnEditar, btnExcluir);
        acoesDireita.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox acoes = new HBox(10, acoesEsquerda, spacer, acoesDireita);
        acoes.setAlignment(Pos.CENTER_LEFT);

        // Título acima da tabela igual ao da HomeView
        Label lblTituloTabela = new Label("Receitas");
        lblTituloTabela.getStyleClass().add("section-title");

        aba.getChildren().addAll(form, acoes, new Separator(), lblTituloTabela, scrollReceitas);
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

        // --- Botões formulário ---
        Button btnSalvar   = new Button("Salvar Despesa");
        Button btnCancelar = new Button("Cancelar Edição");
        Label  lblMsg      = new Label();
        btnSalvar.getStyleClass().add("btn-primary"); // azul
        btnCancelar.getStyleClass().add("btn-secondary");
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);

        // --- Filtros ---
        ComboBox<Categoria> cbCategoriaFiltro = new ComboBox<>();
        cbCategoriaFiltro.getItems().add(null);
        cbCategoriaFiltro.getItems().addAll(cf.getCategorias());
        cbCategoriaFiltro.setPromptText("Todas");
        cbCategoriaFiltro.getStyleClass().add("dark-combo");

        ComboBox<String> cbMesFiltro = new ComboBox<>();
        cbMesFiltro.getItems().addAll(ControleFinanceiro.MESES);
        cbMesFiltro.setPromptText("Todos");
        cbMesFiltro.getStyleClass().add("dark-combo");

        int anoAtual = LocalDate.now().getYear();
        ComboBox<Integer> cbAnoFiltro = new ComboBox<>();
        java.util.TreeSet<Integer> anos = new java.util.TreeSet<>(cf.anosDisponiveis());
        anos.add(anoAtual);
        cbAnoFiltro.getItems().addAll(anos.descendingSet());
        cbAnoFiltro.setValue(anoAtual);
        cbAnoFiltro.getStyleClass().add("dark-combo");

        Button btnLimparFiltro = new Button("Limpar Filtros");
        btnLimparFiltro.getStyleClass().add("btn-secondary");

        // --- Tabela estilizada ---
        List<String>  colsDesp = List.of("ID", "CATEGORIA", "DETALHE", "VALOR", "CONTA", "DATA");
        List<Integer> larsDesp = List.of(60, 130, 200, 110, 120, 110);

        VBox tabelaDespesas = criarTabelaEstilizada(colsDesp, larsDesp);
        ScrollPane scrollDespesas = new ScrollPane(tabelaDespesas);
        scrollDespesas.setFitToWidth(true);
        scrollDespesas.setFitToHeight(false);
        scrollDespesas.getStyleClass().add("main-scroll");
        VBox.setVgrow(scrollDespesas, Priority.ALWAYS);

        // --- Runnable para popular/recarregar ---
        Runnable recarregarDespesas = () -> {
            tabelaDespesas.getChildren().subList(1, tabelaDespesas.getChildren().size()).clear();
            List<Despesa> despesas = cf.getDespesas();
            if (despesas.isEmpty()) {
                Label vazio = new Label("Nenhuma despesa cadastrada.");
                vazio.getStyleClass().add("empty-label");
                vazio.setPadding(new Insets(10));
                tabelaDespesas.getChildren().add(vazio);
                return;
            }
            for (Despesa d : despesas) {
                HBox linha = criarLinhaEstilizada(
                        List.of(
                                String.valueOf(d.getId()),
                                d.getCategoriaNome(),
                                d.getDetalhamento(),
                                String.format("R$ %,.2f", d.getValor()),
                                d.getContaNome(),
                                d.getData().toString()
                        ),
                        larsDesp,
                        "#E85C4A" // vermelho para despesa
                );
                linha.setUserData(d);
                linha.setOnMouseClicked(ev -> {
                    tabelaDespesas.getChildren().stream()
                            .filter(n -> n instanceof HBox && ((HBox) n).getUserData() instanceof Despesa)
                            .forEach(n -> n.getStyleClass().remove("table-row-selected"));
                    linha.getStyleClass().add("table-row-selected");
                });
                tabelaDespesas.getChildren().add(linha);
            }
        };
        recarregarDespesas.run();

        // --- Runnable filtro ---
        Runnable aplicarFiltro = () -> {
            Integer categoriaId = cbCategoriaFiltro.getValue() == null
                    ? null : cbCategoriaFiltro.getValue().getId();
            String mes = cbMesFiltro.getValue();
            int ano = cbAnoFiltro.getValue() != null ? cbAnoFiltro.getValue() : anoAtual;

            tabelaDespesas.getChildren().subList(1, tabelaDespesas.getChildren().size()).clear();
            List<Despesa> filtradas = cf.pesquisarDespesas(categoriaId, mes, ano);
            if (filtradas.isEmpty()) {
                Label vazio = new Label("Nenhuma despesa encontrada.");
                vazio.getStyleClass().add("empty-label");
                vazio.setPadding(new Insets(10));
                tabelaDespesas.getChildren().add(vazio);
                return;
            }
            for (Despesa d : filtradas) {
                HBox linha = criarLinhaEstilizada(
                        List.of(
                                String.valueOf(d.getId()),
                                d.getCategoriaNome(),
                                d.getDetalhamento(),
                                String.format("R$ %,.2f", d.getValor()),
                                d.getContaNome(),
                                d.getData().toString()
                        ),
                        larsDesp,
                        "#E85C4A"
                );
                linha.setUserData(d);
                linha.setOnMouseClicked(ev -> {
                    tabelaDespesas.getChildren().stream()
                            .filter(n -> n instanceof HBox && ((HBox) n).getUserData() instanceof Despesa)
                            .forEach(n -> n.getStyleClass().remove("table-row-selected"));
                    linha.getStyleClass().add("table-row-selected");
                });
                tabelaDespesas.getChildren().add(linha);
            }
        };

        cbCategoriaFiltro.setOnAction(e -> aplicarFiltro.run());
        cbMesFiltro.setOnAction(e -> aplicarFiltro.run());
        cbAnoFiltro.setOnAction(e -> aplicarFiltro.run());
        btnLimparFiltro.setOnAction(e -> {
            cbCategoriaFiltro.setValue(null);
            cbMesFiltro.setValue(null);
            cbAnoFiltro.setValue(anoAtual);
            recarregarDespesas.run();
        });

        // --- Botões ação ---
        Button btnEditar   = new Button("Editar");
        Button btnExcluir  = new Button("Excluir");
        Button btnImportar = new Button("Importar CSV");
        btnEditar.getStyleClass().add("btn-info");
        btnExcluir.getStyleClass().add("btn-danger");
        btnImportar.getStyleClass().add("btn-info");

        final int[] idEmEdicao = {-1};

        btnEditar.setOnAction(e -> {
            Despesa sel = tabelaDespesas.getChildren().stream()
                    .filter(n -> n instanceof HBox && n.getStyleClass().contains("table-row-selected"))
                    .map(n -> (Despesa) ((HBox) n).getUserData())
                    .findFirst().orElse(null);
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma despesa para editar.", false); return; }
            idEmEdicao[0] = sel.getId();
            cbCat.getItems().setAll(cf.getCategorias());
            cbConta.getItems().setAll(cf.getContas());
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
            btnCancelar.setManaged(true);
            mostrarMsg(lblMsg, "Editando despesa ID " + idEmEdicao[0], true);
        });

        btnCancelar.setOnAction(e -> {
            idEmEdicao[0] = -1;
            tfDetalhe.clear(); tfValor.clear();
            cbCat.setValue(null); cbConta.setValue(null); dpData.setValue(LocalDate.now());
            btnSalvar.setText("Salvar Despesa");
            btnCancelar.setVisible(false);
            btnCancelar.setManaged(false);
            lblMsg.setText("");
        });

        btnSalvar.setOnAction(e -> {
            try {
                Categoria cat = cbCat.getValue();
                if (cat == null) throw new RuntimeException("Selecione uma categoria.");
                String det = tfDetalhe.getText().trim();
                if (det.isEmpty()) throw new RuntimeException("Detalhamento é obrigatório.");
                double valor = Conversor.parseValorPositivo(tfValor.getText());
                Conta conta = cbConta.getValue();
                if (conta == null) throw new RuntimeException("Selecione uma conta.");
                LocalDate data = dpData.getValue();
                if (data == null) throw new RuntimeException("Selecione uma data.");
                String mes = Conversor.nomeMes(data);

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
                    btnCancelar.setManaged(false);
                }
                recarregarDespesas.run();
                tfDetalhe.clear(); tfValor.clear();
                cbCat.setValue(null); cbConta.setValue(null); dpData.setValue(LocalDate.now());
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
        });

        btnExcluir.setOnAction(e -> {
            Despesa sel = tabelaDespesas.getChildren().stream()
                    .filter(n -> n instanceof HBox && n.getStyleClass().contains("table-row-selected"))
                    .map(n -> (Despesa) ((HBox) n).getUserData())
                    .findFirst().orElse(null);
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma despesa.", false); return; }
            if (!confirmarExclusao("a despesa \"" + sel.getDetalhamento() + "\"")) return;
            try {
                cf.excluirDespesa(sel.getId());
                recarregarDespesas.run();
                mostrarMsg(lblMsg, "✔ Excluída.", true);
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
        });

        btnImportar.setOnAction(e ->
                abrirImportacao((Stage) btnImportar.getScene().getWindow(), recarregarDespesas));

        // --- Layout ---
        HBox acoesEsquerda = new HBox(10, btnSalvar, btnCancelar);
        acoesEsquerda.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox acoesDireita = new HBox(10, btnEditar, btnExcluir, btnImportar);
        acoesDireita.setAlignment(Pos.CENTER_LEFT);

        HBox acoes = new HBox(10, acoesEsquerda, spacer, acoesDireita);
        acoes.setAlignment(Pos.CENTER_LEFT);

        HBox filtros = new HBox(10,
                label("Categoria:"), cbCategoriaFiltro,
                label("Mês:"), cbMesFiltro,
                label("Ano:"), cbAnoFiltro,
                btnLimparFiltro);
        filtros.setAlignment(Pos.CENTER_LEFT);

        Label lblTituloTabela = new Label("Despesas");
        lblTituloTabela.getStyleClass().add("section-title");

        aba.getChildren().addAll(
                form,
                acoes,
                new Separator(),
                filtros,
                lblTituloTabela,
                scrollDespesas
        );
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
                double valor = Conversor.parseValorPositivo(tfValor.getText());
                Conta conta = cbConta.getValue();
                if (conta == null) throw new RuntimeException("Selecione uma conta.");
                LocalDate data = dpData.getValue();
                if (data == null) throw new RuntimeException("Selecione uma data.");
                String mes = Conversor.nomeMes(data);
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
            if (sel == null) { mostrarMsg(lblMsg, "Selecione um investimento.", false); return; }
            if (!confirmarExclusao("o investimento \"" + sel.getTipo() + "\"")) return;
            try {
                cf.excluirInvestimento(sel.getId());
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getInvestimentos()));
                mostrarMsg(lblMsg, "✔ Excluído.", true);
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
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
                if (tipo == LancamentoFixo.Tipo.DESPESA && cbCat.getValue() == null)
                    throw new RuntimeException("Selecione a categoria da despesa.");
                int catId = tipo == LancamentoFixo.Tipo.DESPESA ? cbCat.getValue().getId() : 0;
                double valor = Conversor.parseValorPositivo(tfValor.getText());
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
            cbMesIni.setValue(Conversor.nomeMes(LocalDate.now()));

            Spinner<Integer> spAnoIni = new Spinner<>(2000, 2100, LocalDate.now().getYear());

            ComboBox<String> cbMesFim = new ComboBox<>();
            cbMesFim.getItems().addAll(ControleFinanceiro.MESES);
            cbMesFim.setValue(Conversor.nomeMes(LocalDate.now()));

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
            if (sel == null) { mostrarMsg(lblMsg, "Selecione um lançamento fixo.", false); return; }
            if (!confirmarExclusao("o lançamento fixo \"" + sel.getDescricao() + "\"")) return;
            try {
                cf.excluirLancamentoFixo(sel.getId());
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getLancamentosFixos()));
                mostrarMsg(lblMsg, "✔ Excluído.", true);
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
        });

        btnAlternar.setOnAction(e -> {
            LancamentoFixo sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { mostrarMsg(lblMsg, "Selecione um lançamento fixo.", false); return; }
            try {
                cf.alternarAtivoFixo(sel.getId());
                tabela.setItems(javafx.collections.FXCollections.observableArrayList(cf.getLancamentosFixos()));
            } catch (Exception ex) {
                mostrarMsg(lblMsg, "✖ " + ex.getMessage(), false);
            }
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
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma categoria.", false); return; }
            if (!confirmarExclusao("a categoria \"" + sel.getNome() + "\"")) return;
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
            if (sel == null) { mostrarMsg(lblMsg, "Selecione uma conta.", false); return; }
            if (!confirmarExclusao("a conta \"" + sel.getNome() + "\"")) return;
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

    private boolean confirmarExclusao(String oQue) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente excluir " + oQue + "?\nEssa ação não pode ser desfeita.",
                ButtonType.YES, ButtonType.NO);
        alerta.setTitle("Confirmar exclusão");
        alerta.setHeaderText(null);
        return alerta.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    private void abrirImportacao(Stage owner, Runnable aoImportar) {
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

        TableColumn<LinhaImportacao, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getData().toString()));
        colData.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colData.setOnEditCommit(e -> {
            try {
                LocalDate novaData = Conversor.parseData(e.getNewValue());
                e.getRowValue().setData(novaData);
            } catch (Exception ex) {
                mostrarMsg(lblStatus, "✖ " + ex.getMessage(), false);
                tabelaPreview.refresh();
            }
        });
        colData.setPrefWidth(110);
        colData.setEditable(true);

        TableColumn<LinhaImportacao, String> colTitulo = new TableColumn<>("Descrição Original");
        colTitulo.setCellValueFactory(c -> c.getValue().tituloProperty());
        colTitulo.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colTitulo.setOnEditCommit(e -> e.getRowValue().tituloProperty().set(e.getNewValue()));
        colTitulo.setPrefWidth(240);
        colTitulo.setEditable(true);

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

        TableColumn<LinhaImportacao, String> colObs = new TableColumn<>("Observação");
        colObs.setCellValueFactory(c -> c.getValue().observacaoProperty());
        colObs.setPrefWidth(120);

        tabelaPreview.getColumns().addAll(
                colImportar, colData, colTitulo, colValor, colCategoria, colDetalhe, colStatus, colObs);

        // Botões rodapé
        Button btnConfirmar = new Button("Confirmar Importação");
        btnConfirmar.getStyleClass().add("btn-primary");
        btnConfirmar.setDisable(true);

        Button btnFechar = new Button("Fechar");
        btnFechar.getStyleClass().add("btn-secondary");
        btnFechar.setOnAction(e -> stage.close());

        HBox rodape = new HBox(10, btnConfirmar, btnFechar, lblStatus);
        rodape.setAlignment(Pos.CENTER_LEFT);

        // Conta usada no preview (a checagem de duplicatas é feita contra ela)
        final Conta[] contaDoPreview = {null};
        cbConta.setOnAction(e -> {
            if (contaDoPreview[0] != null && cbConta.getValue() != contaDoPreview[0]) {
                btnConfirmar.setDisable(true);
                mostrarMsg(lblStatus, "Conta alterada: clique em \"Carregar Preview\" novamente.", false);
            }
        });

        // Ações
        btnCarregar.setOnAction(e -> {
            String caminho = tfArquivo.getText().trim();
            if (caminho.isEmpty()) { mostrarMsg(lblStatus, "Selecione um arquivo.", false); return; }
            if (cbConta.getValue() == null) { mostrarMsg(lblStatus, "Selecione uma conta.", false); return; }
            try {
                ResultadoLeituraCsv resultado = cf.lerCsv(caminho, cbConta.getValue().getId());
                List<LinhaImportacao> linhas = resultado.linhas();
                contaDoPreview[0] = cbConta.getValue();
                tabelaPreview.setItems(FXCollections.observableArrayList(linhas));
                btnConfirmar.setDisable(linhas.isEmpty());
                long marcadas = linhas.stream().filter(LinhaImportacao::isImportar).count();
                long semCat = linhas.stream().filter(l -> l.isImportar() && l.getCategoria() == null).count();
                long novos  = linhas.stream().filter(LinhaImportacao::isMapeamentoNovo).count();
                long desmarcadas = linhas.size() - marcadas;
                String msg = linhas.size() + " linha(s), " + marcadas + " marcada(s). " + novos + " novo(s) mapeamento(s)."
                        + (desmarcadas > 0 ? " " + desmarcadas + " desmarcada(s) (estorno ou já importada)." : "")
                        + (semCat > 0 ? " ⚠ " + semCat + " sem categoria." : "")
                        + (resultado.erros().isEmpty() ? "" : " ⚠ " + resultado.erros().size() + " linha(s) ignorada(s) com erro.");
                mostrarMsg(lblStatus, msg, semCat == 0 && resultado.erros().isEmpty());
                if (!resultado.erros().isEmpty()) {
                    Alert alerta = new Alert(Alert.AlertType.WARNING);
                    alerta.setTitle("Linhas ignoradas");
                    alerta.setHeaderText(resultado.erros().size() + " linha(s) do CSV não puderam ser lidas:");
                    alerta.setContentText(String.join("\n",
                            resultado.erros().subList(0, Math.min(15, resultado.erros().size())))
                            + (resultado.erros().size() > 15 ? "\n…" : ""));
                    alerta.initOwner(stage);
                    alerta.show();
                }
            } catch (Exception ex) {
                mostrarMsg(lblStatus, "✖ Não foi possível ler o arquivo: " + ex.getMessage(), false);
            }
        });

        btnConfirmar.setOnAction(e -> {
            long semCat = tabelaPreview.getItems().stream()
                    .filter(l -> l.isImportar() && l.getCategoria() == null).count();
            if (semCat > 0) {
                mostrarMsg(lblStatus, "✖ " + semCat + " linha(s) sem categoria. Preencha ou desmarque.", false);
                return;
            }
            try {
                int importados = cf.confirmarImportacao(tabelaPreview.getItems(), contaDoPreview[0].getId());
                mostrarMsg(lblStatus, "✔ " + importados + " despesa(s) importada(s)!", true);
                tabelaPreview.getItems().clear();
                btnConfirmar.setDisable(true);
                aoImportar.run();
            } catch (Exception ex) {
                mostrarMsg(lblStatus, "✖ " + ex.getMessage(), false);
            }
        });

        root.getChildren().addAll(topoForm, new Separator(), tabelaPreview, rodape);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 980, 640);
        scene.getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private VBox criarTabelaEstilizada(List<String> colunas, List<Integer> larguras) {
        VBox tabela = new VBox(0);
        tabela.getStyleClass().add("fixos-block");

        // Cabeçalho
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 8, 6, 8));
        header.getStyleClass().add("table-header");
        for (int i = 0; i < colunas.size(); i++) {
            header.getChildren().add(col(colunas.get(i), larguras.get(i)));
        }
        tabela.getChildren().add(header);
        return tabela;
    }

    private HBox criarLinhaEstilizada(List<String> valores, List<Integer> larguras, String corPrimeira) {
        HBox linha = new HBox();
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(6, 8, 6, 8));
        linha.getStyleClass().add("table-row");

        for (int i = 0; i < valores.size(); i++) {
            Label l = col(valores.get(i), larguras.get(i));
            if (i == 0 && corPrimeira != null) l.setStyle("-fx-text-fill: " + corPrimeira + ";");
            linha.getChildren().add(l);
        }
        return linha;
    }

    private Label col(String texto, int largura) {
        Label l = new Label(texto != null ? texto : "");
        l.setPrefWidth(largura);
        l.getStyleClass().add("table-cell");
        return l;
    }


}
