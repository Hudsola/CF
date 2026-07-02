package ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import ui.App;

public class NavBar extends HBox {

    private Button btnAtivo;

    public NavBar() {
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 20, 0, 20));
        setPrefHeight(45);
        getStyleClass().add("navbar");

        Button btnHome      = criarBotaoNav("HOME",      "home");
        Button btnCadastros = criarBotaoNav("CADASTROS", "cadastros");
        Button btnResumo    = criarBotaoNav("RESUMO",    "resumo");

        // Espaçador empurra itens futuros para a direita
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(btnHome, btnCadastros, btnResumo, spacer);

        // Home ativa por padrão
        ativar(btnHome);
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
