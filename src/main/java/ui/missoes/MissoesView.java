package ui.missoes;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Missao;
import service.Missoes;
import ui.components.Ui;

import java.util.List;

/**
 * Aba "Missões": o que fazer hoje e o que ficou atrasado (marcar conclui e rende XP), os objetivos sem prazo
 * em andamento e o cadastro das missões.
 */
public class MissoesView {

    /** Quantos dias para trás contam como "atrasadas". */
    static final int DIAS_ATRASADAS = 7;

    private final Missoes missoes = new Missoes(ui.Sessao.usuario().getId());
    private final ListaMissoes hoje = new ListaMissoes(missoes, false, this::atualizar);
    private final ListaMissoes atrasadas = new ListaMissoes(missoes, true, this::atualizar);
    private final VBox objetivos = new VBox(4);
    private final Label tituloHoje = new Label();
    private AbaMissoes cadastro;

    public Node getView() {
        cadastro = new AbaMissoes(missoes, this::atualizar);

        HBox topo = new HBox(16,
                bloco(tituloHoje, hoje),
                bloco(titulo("Atrasadas (últimos " + DIAS_ATRASADAS + " dias)"), atrasadas),
                bloco(titulo("Objetivos sem prazo"), objetivos));
        for (Node n : topo.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        VBox cadastroBloco = bloco(titulo("Cadastro de missões"), cadastro.getView());
        VBox root = new VBox(16, topo, cadastroBloco);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");
        atualizar();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        return scroll;
    }

    /** Recarrega as listas (depois de concluir, cadastrar, alterar ou excluir). */
    void atualizar() {
        tituloHoje.setText("Hoje — " + Ui.diaCurto(missoes.hoje()));
        hoje.mostrar(missoes.doDia(missoes.hoje()), "Nenhuma missão para hoje.");
        atrasadas.mostrar(missoes.atrasadas(DIAS_ATRASADAS), "Nada atrasado.");
        List<Missao> emAndamento = missoes.semPrazoEmAndamento();
        objetivos.getChildren().clear();
        if (emAndamento.isEmpty()) {
            Label vazio = new Label("Nenhum objetivo em andamento.");
            vazio.getStyleClass().add("empty-label");
            objetivos.getChildren().add(vazio);
        }
        for (Missao m : emAndamento) {
            Label l = new Label("• " + m.titulo());
            l.getStyleClass().add("missao-titulo");
            objetivos.getChildren().add(l);
        }
        if (cadastro != null) cadastro.atualizar();
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("section-title");
        return l;
    }

    private static VBox bloco(Label titulo, Node conteudo) {
        titulo.getStyleClass().add("section-title");
        VBox b = new VBox(10, titulo, conteudo);
        b.getStyleClass().add("fixos-block");
        b.setPadding(new Insets(16));
        b.setMinWidth(0);
        return b;
    }
}
