package ui.missoes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.CategoriaMissao;
import model.Missao;
import model.TipoMissao;
import service.Missoes;
import ui.components.AbaCrud;
import ui.components.Ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Cadastro de missões. O formulário muda conforme o tipo: dias escolhidos (diária), dias da semana (semanal),
 * dia do mês (mensal) ou nada (sem prazo). Missões com data entram na Agenda como pendentes.
 */
class AbaMissoes extends AbaCrud<Missao> {

    private final Missoes missoes;
    private final Runnable aoMudar;

    private final TextField tfTitulo = Ui.campo("Ex: Academia, Pagar a fatura, Aprender inglês");
    private final TextField tfDescricao = Ui.campo("Opcional");
    private final ComboBox<TipoMissao> cbTipo = new ComboBox<>();
    private final ComboBox<CategoriaMissao> cbCategoria = new ComboBox<>();
    /** Missão-mãe; o item nulo é "nenhuma" (missão independente). */
    private final ComboBox<Missao> cbMae = new ComboBox<>();
    private final StackPane detalhesTipo = new StackPane();
    /** Missão em edição (0 = nova), para saber quais mães podem ser escolhidas. */
    private int idEmEdicao;

    // Diária
    private final DatePicker dpDia = Ui.seletorData(LocalDate.now());
    private final SortedSet<LocalDate> diasEscolhidos = new TreeSet<>();
    private final FlowPane chipsDias = new FlowPane(6, 6);
    // Semanal
    private final Map<DayOfWeek, CheckBox> diasSemana = new EnumMap<>(DayOfWeek.class);
    // Mensal
    private final Spinner<Integer> spDiaMes = new Spinner<>(1, 31, LocalDate.now().getDayOfMonth());
    // Semanal e mensal
    private final DatePicker dpInicio = Ui.seletorData(LocalDate.now());
    private final DatePicker dpFim = Ui.seletorData(null);

    AbaMissoes(Missoes missoes, Runnable aoMudar) {
        this.missoes = missoes;
        this.aoMudar = aoMudar;
    }

    @Override protected String nome() { return "Missão"; }

    @Override
    protected void montarFormulario(GridPane form) {
        cbTipo.getItems().setAll(TipoMissao.values());
        cbTipo.setValue(TipoMissao.DIARIA);
        cbTipo.valueProperty().addListener((obs, antes, tipo) -> mostrarDetalhes(tipo));
        spDiaMes.setEditable(true);
        spDiaMes.setPrefWidth(90);
        dpFim.setPromptText("Sem data final");

        cbCategoria.getItems().setAll(CategoriaMissao.values());
        cbCategoria.setValue(CategoriaMissao.SECUNDARIA);
        cbCategoria.setTooltip(new Tooltip("Missões principais valem o dobro de XP"));
        cbMae.setPromptText("Nenhuma (missão independente)");
        cbMae.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Missao m) { return m == null ? "Nenhuma (missão independente)" : m.titulo(); }
            @Override public Missao fromString(String s) { return null; }
        });
        cbMae.setPrefWidth(260);
        cbMae.valueProperty().addListener((obs, antes, mae) -> {
            // a sub-missão acompanha a categoria da mãe
            if (mae != null) cbCategoria.setValue(mae.categoria());
            cbCategoria.setDisable(mae != null);
        });

        form.addRow(0, Ui.label("Título:"), tfTitulo);
        form.addRow(1, Ui.label("Descrição:"), tfDescricao);
        form.addRow(2, Ui.label("Categoria:"), cbCategoria);
        form.addRow(3, Ui.label("Sub-missão de:"), cbMae,
                ajuda("Sub-missões sem prazo são requisitos: a missão-mãe só é concluída depois delas."));
        form.addRow(4, Ui.label("Tipo:"), cbTipo);
        form.add(detalhesTipo, 0, 5, 3, 1);
        mostrarDetalhes(TipoMissao.DIARIA);
    }

    @Override
    protected void recarregarOpcoes() {
        Missao atual = cbMae.getValue();
        List<Missao> opcoes = new ArrayList<>();
        opcoes.add(null);
        opcoes.addAll(missoes.possiveisMaes(idEmEdicao));
        cbMae.getItems().setAll(opcoes);
        cbMae.setValue(atual == null ? null : opcoes.stream().filter(m -> m != null && m.id() == atual.id()).findFirst().orElse(null));
        cbMae.setDisable(opcoes.size() == 1 && idEmEdicao != 0 && !missoes.subMissoes(idEmEdicao).isEmpty());
    }

    private void mostrarDetalhes(TipoMissao tipo) {
        Node conteudo = switch (tipo == null ? TipoMissao.DIARIA : tipo) {
            case DIARIA -> detalhesDiaria();
            case SEMANAL -> detalhesRecorrente("Dias da semana:", linhaDiasSemana());
            case MENSAL -> detalhesRecorrente("Dia do mês:", new HBox(8, spDiaMes,
                    ajuda("Se o mês não tiver esse dia (ex: 31), vale o último dia do mês.")));
            case SEM_PRAZO -> ajuda("Objetivo sem data (ex: juntar R$ 10 mil, comprar uma moto). "
                    + "Não aparece na Agenda; conclua quando cumprir.");
        };
        detalhesTipo.getChildren().setAll(conteudo);
        StackPane.setAlignment(conteudo, Pos.CENTER_LEFT);
    }

    private Node detalhesDiaria() {
        Button adicionar = new Button("Adicionar dia");
        adicionar.getStyleClass().add("btn-secondary");
        adicionar.setOnAction(e -> {
            if (dpDia.getValue() != null) diasEscolhidos.add(dpDia.getValue());
            atualizarChips();
        });
        atualizarChips();
        GridPane g = grade();
        g.addRow(0, Ui.label("Dias:"), new HBox(8, dpDia, adicionar));
        g.add(chipsDias, 1, 1);
        g.add(ajuda("Escolha um ou mais dias (pode ser hoje). Se nenhum for adicionado, vale o dia selecionado."), 1, 2);
        return g;
    }

    private void atualizarChips() {
        chipsDias.getChildren().clear();
        for (LocalDate d : diasEscolhidos) {
            Button chip = new Button(Ui.diaCurto(d) + "/" + d.getYear() + "  ✕");
            chip.getStyleClass().add("chip-dia");
            chip.setTooltip(new Tooltip("Remover este dia"));
            chip.setOnAction(e -> { diasEscolhidos.remove(d); atualizarChips(); });
            chipsDias.getChildren().add(chip);
        }
    }

    private HBox linhaDiasSemana() {
        HBox linha = new HBox(8);
        for (DayOfWeek d : DayOfWeek.values()) {
            CheckBox cb = diasSemana.computeIfAbsent(d,
                    k -> new CheckBox(k.getDisplayName(TextStyle.SHORT, Ui.PT_BR).replace(".", "")));
            linha.getChildren().add(cb);
        }
        return linha;
    }

    private Node detalhesRecorrente(String rotulo, Node campo) {
        GridPane g = grade();
        g.addRow(0, Ui.label(rotulo), campo);
        g.addRow(1, Ui.label("A partir de:"), dpInicio);
        g.addRow(2, Ui.label("Até (opcional):"), dpFim);
        return g;
    }

    private static GridPane grade() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(8);
        return g;
    }

    private static Label ajuda(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("empty-label");
        l.setWrapText(true);
        l.setMaxWidth(520);
        return l;
    }

    @Override
    protected void configurarColunas(TableView<Missao> t) {
        t.getColumns().add(Ui.colunaTexto("Missão", AbaMissoes::tituloNaTabela, 240));
        t.getColumns().add(Ui.colunaTexto("Categoria", m -> m.categoria().nome(), 90));
        t.getColumns().add(Ui.colunaTexto("Tipo", m -> m.tipo().nome(), 80));
        t.getColumns().add(Ui.colunaTexto("Quando", Missao::quando, 180));
        t.getColumns().add(Ui.colunaTexto("Próxima / situação", this::proxima, 170));
        t.getColumns().add(Ui.colunaTexto("Descrição", m -> m.descricao() == null ? "" : m.descricao(), 180));
        t.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Missao m, boolean vazio) {
                super.updateItem(m, vazio);
                getStyleClass().removeAll("linha-principal", "linha-secundaria", "linha-sub");
                if (!vazio && m != null)
                    getStyleClass().add(m.subMissao() ? "linha-sub" : m.principal() ? "linha-principal" : "linha-secundaria");
            }
        });
    }

    /** Principais com ★ e sub-missões recuadas com ↳, logo abaixo da mãe (ver {@link Missoes#listarEmOrdem()}). */
    static String tituloNaTabela(Missao m) {
        if (m.subMissao()) return "     ↳ " + m.titulo() + (m.requisito() ? "  (requisito)" : "");
        return (m.principal() ? "★ " : "") + m.titulo();
    }

    private String proxima(Missao m) {
        String requisitos = "";
        if (!m.subMissao()) {
            List<Missao> reqs = missoes.subMissoes(m.id()).stream().filter(Missao::requisito).toList();
            long feitos = reqs.stream().filter(r -> r.concluidaEm() != null).count();
            if (!reqs.isEmpty()) requisitos = " · " + feitos + "/" + reqs.size() + " requisitos";
        }
        if (m.tipo() == TipoMissao.SEM_PRAZO)
            return (m.concluidaEm() != null ? "✔ concluída em " + Ui.DATA_BR.format(m.concluidaEm()) : "em andamento") + requisitos;
        return m.proximaOcorrencia(missoes.hoje()).map(Ui::diaCurto).orElse("—") + requisitos;
    }

    @Override protected List<Missao> carregar() { return missoes.listarEmOrdem(); }

    @Override
    protected void preencherFormulario(Missao m) {
        idEmEdicao = m.id();
        recarregarOpcoes();
        cbMae.setValue(m.missaoPaiId() == null ? null : cbMae.getItems().stream()
                .filter(x -> x != null && x.id() == m.missaoPaiId()).findFirst().orElse(null));
        cbCategoria.setValue(m.categoria());
        tfTitulo.setText(m.titulo());
        tfDescricao.setText(m.descricao() == null ? "" : m.descricao());
        cbTipo.setValue(m.tipo());
        diasEscolhidos.clear();
        diasEscolhidos.addAll(m.datas());
        atualizarChips();
        diasSemana.forEach((d, cb) -> cb.setSelected(m.diasSemana().contains(d)));
        if (m.diaMes() != null) spDiaMes.getValueFactory().setValue(m.diaMes());
        dpInicio.setValue(m.inicio() != null ? m.inicio() : LocalDate.now());
        dpFim.setValue(m.fim());
        mostrarDetalhes(m.tipo());
    }

    @Override
    protected void limparFormulario() {
        idEmEdicao = 0;
        recarregarOpcoes();
        cbMae.setValue(null);
        cbCategoria.setValue(CategoriaMissao.SECUNDARIA);
        tfTitulo.clear();
        tfDescricao.clear();
        cbTipo.setValue(TipoMissao.DIARIA);
        diasEscolhidos.clear();
        atualizarChips();
        dpDia.setValue(LocalDate.now());
        diasSemana.values().forEach(cb -> cb.setSelected(false));
        spDiaMes.getValueFactory().setValue(LocalDate.now().getDayOfMonth());
        dpInicio.setValue(LocalDate.now());
        dpFim.setValue(null);
    }

    /** Lê o formulário; a validação fica no serviço (mensagens iguais em qualquer tela). */
    private Missao lerFormulario(int id) {
        String titulo = tfTitulo.getText(), descricao = tfDescricao.getText();
        TipoMissao tipo = cbTipo.getValue();
        if (tipo == null) throw new IllegalArgumentException("Escolha o tipo da missão.");
        Missao m = switch (tipo) {
            case DIARIA -> {
                SortedSet<LocalDate> dias = new TreeSet<>(diasEscolhidos);
                if (dias.isEmpty() && dpDia.getValue() != null) dias.add(dpDia.getValue());
                yield Missao.diaria(titulo, descricao, dias);
            }
            case SEMANAL -> {
                Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
                diasSemana.forEach((d, cb) -> { if (cb.isSelected()) dias.add(d); });
                yield Missao.semanal(titulo, descricao, dias, dpInicio.getValue(), dpFim.getValue());
            }
            case MENSAL -> Missao.mensal(titulo, descricao, spDiaMes.getValue(), dpInicio.getValue(), dpFim.getValue());
            case SEM_PRAZO -> Missao.semPrazo(titulo, descricao);
        };
        Missao mae = cbMae.getValue();
        return m.comId(id).comCategoria(cbCategoria.getValue()).comMissaoPai(mae == null ? null : mae.id());
    }

    @Override protected void salvarNovo() { missoes.criar(lerFormulario(0)); aoMudar.run(); }
    @Override protected void salvarAlteracao(Missao original) { missoes.atualizar(lerFormulario(original.id())); aoMudar.run(); }

    /** Excluir a mãe já exclui as sub-missões; se elas também estavam selecionadas, não há mais o que excluir. */
    @Override
    protected void excluir(Missao m) {
        if (missoes.buscar(m.id()).isEmpty()) return;
        missoes.excluir(m.id());
        aoMudar.run();
    }

    @Override
    protected String descrever(Missao m) {
        int subs = m.subMissao() ? 0 : missoes.subMissoes(m.id()).size();
        return "a missão \"" + m.titulo() + "\"" + (subs == 0 ? "" : subs == 1 ? " e a sub-missão dela" : " e as " + subs + " sub-missões dela");
    }
    @Override protected String descreverVarios(List<Missao> itens) { return "as " + itens.size() + " missões selecionadas"; }

    /** Botão para concluir (ou reabrir) a missão sem prazo selecionada. */
    @Override
    protected List<Button> botoesExtras() {
        Button concluir = new Button("Concluir / reabrir objetivo");
        concluir.getStyleClass().add("btn-primary");
        concluir.setTooltip(new Tooltip("Missões sem prazo: marca como cumprida (+"
                + service.CalculadoraXp.XP_MISSAO_SEM_PRAZO + " XP, o dobro se for principal) ou reabre. "
                + "Se tiver requisitos, eles precisam estar cumpridos."));
        concluir.setOnAction(e -> alternarObjetivo());
        return List.of(concluir);
    }

    private void alternarObjetivo() {
        Missao m = selecionado();
        if (m == null || m.tipo() != TipoMissao.SEM_PRAZO) {
            erro("Selecione uma missão sem prazo na tabela.");
            return;
        }
        try {
            if (m.concluidaEm() == null) {
                missoes.concluirSemPrazo(m.id());
                sucesso("Objetivo \"" + m.titulo() + "\" concluído!");
            } else {
                missoes.reabrirSemPrazo(m.id());
                sucesso("Objetivo \"" + m.titulo() + "\" reaberto.");
            }
            recarregarTabela();
            aoMudar.run();
        } catch (RuntimeException ex) {
            erro(ex.getMessage());
        }
    }
}
