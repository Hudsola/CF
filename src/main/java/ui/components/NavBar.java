package ui.components;

import db.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import ui.App;
import ui.Sessao;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavBar extends HBox {

    /** Abas do app, na ordem da barra: identificador da tela -> texto do botão. */
    public static final Map<String, String> ABAS = abas();

    private final Map<String, Button> botoes = new HashMap<>();
    private Button btnAtivo;

    private static Map<String, String> abas() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("home", "HOME");
        m.put("missoes", "MISSÕES");
        m.put("financas", "FINANÇAS");
        m.put("agenda", "AGENDA");
        m.put("estoque", "ESTOQUE");
        return Collections.unmodifiableMap(m);
    }

    public NavBar() {
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 20, 0, 20));
        setSpacing(4);
        setPrefHeight(45);
        getStyleClass().add("navbar");

        for (var aba : ABAS.entrySet()) getChildren().add(criarBotaoNav(aba.getValue(), aba.getKey()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(spacer, criarInfoBanco());
        marcarAtivo("home");
    }

    /** Usuário logado, atalho para a pasta do banco (caminho no tooltip) e botão Sair. */
    private HBox criarInfoBanco() {
        Path arquivo = DatabaseManager.arquivoBanco();
        Button btnPasta = new Button("Pasta de dados");
        btnPasta.getStyleClass().add("btn-secondary");
        btnPasta.setTooltip(new Tooltip("Banco: " + arquivo + "\nCopie esse arquivo para fazer backup.\n"
                + "Para abrir no DB Browser, feche o app antes de editar."));
        btnPasta.setOnAction(e -> App.abrirNoSistema(DatabaseManager.pastaDados()));

        Label lblUsuario = new Label(Sessao.ativa() ? "Olá, " + Sessao.usuario().getNome() : "");
        lblUsuario.getStyleClass().add("navbar-usuario");

        Button btnSair = new Button("Sair");
        btnSair.getStyleClass().add("btn-secondary");
        btnSair.setTooltip(new Tooltip("Encerrar a sessão e voltar para a tela de login"));
        btnSair.setOnAction(e -> App.mostrarLogin());

        HBox box = new HBox(10, btnPasta, lblUsuario, btnSair);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private Button criarBotaoNav(String label, String tela) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> App.navegarPara(tela));
        botoes.put(tela, btn);
        return btn;
    }

    /** Destaca o botão da aba aberta (chamado pelo App a cada navegação). */
    public void marcarAtivo(String tela) {
        Button btn = botoes.get(tela);
        if (btn == null) return;
        if (btnAtivo != null) btnAtivo.getStyleClass().remove("nav-button-active");
        btnAtivo = btn;
        btn.getStyleClass().add("nav-button-active");
    }
}
