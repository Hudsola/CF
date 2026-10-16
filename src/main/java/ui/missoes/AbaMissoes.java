package ui.missoes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    private final StackPane detalhesTipo = new StackPane();

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

        form.addRow(0, Ui.label("Título:"), tfTitulo);
        form.addRow(1, Ui.label("Descrição:"), tfDescricao);
        form.addRow(2, Ui.label("Tipo:"), cbTipo);
        form.add(detalhesTipo, 0, 3, 3, 1);
        mostrarDetalhes(TipoMissao.DIARIA);
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
        t.getColumns().add(Ui.colunaTexto("Missão", Missao::titulo, 220));
        t.getColumns().add(Ui.colunaTexto("Tipo", m -> m.tipo().nome(), 90));
        t.getColumns().add(Ui.colunaTexto("Quando", Missao::quando, 200));
        t.getColumns().add(Ui.colunaTexto("Próxima", this::proxima, 120));
        t.getColumns().add(Ui.colunaTexto("Descrição", m -> m.descricao() == null ? "" : m.descricao(), 200));
    }

    private String proxima(Missao m) {
        if (m.tipo() == TipoMissao.SEM_PRAZO)
            return m.concluidaEm() != null ? "✔ concluída em " + Ui.DATA_BR.format(m.concluidaEm()) : "em andamento";
        return m.proximaOcorrencia(missoes.hoje()).map(Ui::diaCurto).orElse("—");
    }

    @Override protected List<Missao> carregar() { return missoes.listar(); }

    @Override
    protected void preencherFormulario(Missao m) {
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
        return m.comId(id);
    }

    @Override protected void salvarNovo() { missoes.criar(lerFormulario(0)); aoMudar.run(); }
    @Override protected void salvarAlteracao(Missao original) { missoes.atualizar(lerFormulario(original.id())); aoMudar.run(); }
    @Override protected void excluir(Missao m) { missoes.excluir(m.id()); aoMudar.run(); }
    @Override protected String descrever(Missao m) { return "a missão \"" + m.titulo() + "\""; }
    @Override protected String descreverVarios(List<Missao> itens) { return "as " + itens.size() + " missões selecionadas"; }

    /** Botão para concluir (ou reabrir) a missão sem prazo selecionada. */
    @Override
    protected List<Button> botoesExtras() {
        Button concluir = new Button("Concluir / reabrir objetivo");
        concluir.getStyleClass().add("btn-primary");
        concluir.setTooltip(new Tooltip("Missões sem prazo: marca como cumprida (+"
                + service.CalculadoraXp.XP_MISSAO_SEM_PRAZO + " XP) ou reabre"));
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
