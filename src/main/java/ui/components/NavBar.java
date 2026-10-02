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

public class NavBar extends HBox {

    private Button btnAtivo;

    public NavBar() {
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 20, 0, 20));
        setSpacing(4);
        setPrefHeight(45);
        getStyleClass().add("navbar");

        Button btnHome      = criarBotaoNav("HOME",      "home");
        Button btnCadastros = criarBotaoNav("CADASTROS", "cadastros");
        Button btnResumo    = criarBotaoNav("RESUMO",    "resumo");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(btnHome, btnCadastros, btnResumo, spacer, criarInfoBanco());
        ativar(btnHome);
    }

    /** Usuário logado, atalho para a pasta do banco (caminho no tooltip) e botão Sair. */
    private HBox criarInfoBanco() {
        Path arquivo = DatabaseManager.arquivoBanco();
        Button btnPasta = new Button("Pasta de dados");
        btnPasta.getStyleClass().add("btn-secondary");
        btnPasta.setTooltip(new Tooltip("Banco: " + arquivo + "\nCopie esse arquivo para fazer backup.\n"
                + "Para abrir no DB Browser, feche o app antes de editar."));
        btnPasta.setOnAction(e -> App.abrirNoSistema(arquivo.getParent()));

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
        btn.setOnAction(e -> {
            App.navegarPara(tela);
            ativar(btn);
        });
        return btn;
    }

    private void ativar(Button btn) {
        if (btnAtivo != null) btnAtivo.getStyleClass().remove("nav-button-active");
        btnAtivo = btn;
        btn.getStyleClass().add("nav-button-active");
    }
}
