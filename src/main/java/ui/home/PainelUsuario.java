package ui.home;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import model.Dinheiro;
import model.Progresso;
import model.Usuario;
import service.ControleFinanceiro;
import ui.App;
import ui.components.Ui;

import java.math.BigDecimal;
import java.util.Optional;

/** Painel do usuário na Home. */
class PainelUsuario {

    private final ControleFinanceiro cf;

    PainelUsuario(ControleFinanceiro cf) {
        this.cf = cf;
    }

    /**
     * Painel do usuário no estilo do HUD do GTA San Andreas: foto no lugar da arma (com o nível no lugar
     * da munição), nome no lugar do relógio, idade no lugar do colete, XP na barra de saúde e o saldo
     * geral no lugar do dinheiro.
     */
    VBox getView() {
        Usuario usuario = cf.getUsuario();
        BigDecimal saldo = cf.saldoTotal();
        Progresso p = cf.getProgresso();

        Label lblNome = new Label(usuario.getNome());
        lblNome.getStyleClass().addAll("hud-texto", "hud-nome");
        lblNome.setMinWidth(0);

        Button btnEditar = new Button("✎");
        btnEditar.getStyleClass().add("btn-secondary");
        btnEditar.setTooltip(new Tooltip("Editar perfil, foto, senha e conta Google"));
        btnEditar.setOnAction(e -> {
            if (PerfilDialog.editar(usuario)) { App.atualizarBarra(); App.navegarPara("home"); }
        });
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linhaNome = new HBox(6, lblNome, espaco, btnEditar);
        linhaNome.setAlignment(Pos.CENTER_LEFT);

        HBox linhaIdade = new HBox(6);
        linhaIdade.setAlignment(Pos.BASELINE_LEFT);
        if (usuario.getIdade() > 0) {
            linhaIdade.getChildren().addAll(valorHud(String.valueOf(usuario.getIdade()), "hud-idade"),
                    textoHud(usuario.getIdade() == 1 ? "ano" : "anos"));
        } else {
            linhaIdade.getChildren().add(textoHud("Idade não informada"));
        }

        VBox direita = new VBox(6, linhaNome, linhaIdade);
        direita.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(direita, Priority.ALWAYS);
        HBox topo = new HBox(14, criarFotoHud(usuario, p.nivel()), direita);
        topo.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barraXp = new ProgressBar(p.fracaoNivel());
        barraXp.getStyleClass().add("hud-barra-xp");
        barraXp.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(barraXp, new Tooltip("Nível " + p.nivel() + " • " + p.xpNoNivel() + " / "
                + p.xpParaProximo() + " XP para o nível " + (p.nivel() + 1) + " (" + p.xpTotal() + " XP no total)"));

        Label lblSaldo = valorHud(Dinheiro.formatar(saldo), "hud-dinheiro");
        lblSaldo.setStyle(lblSaldo.getStyle() + "-fx-text-fill: " + (saldo.signum() >= 0 ? "#3d8f3a" : "#b4191d") + ";");
        Tooltip.install(lblSaldo, new Tooltip("Saldo geral (todas as contas)"));

        VBox card = new VBox(12, topo, barraXp, lblSaldo);
        VBox.setMargin(barraXp, new Insets(8, 0, 0, 0));   // espaço para o nível que sai da moldura da foto
        card.getStyleClass().addAll("profile-card", "hud-card");
        card.setPrefWidth(360);
        card.setMinWidth(320);
        card.setPadding(new Insets(14));
        return card;
    }

    /** Moldura da foto (no lugar do ícone da arma), com o nível embaixo como a munição. */
    private StackPane criarFotoHud(Usuario usuario, int nivel) {
        final double tamanho = 84;
        StackPane moldura = new StackPane();
        moldura.getStyleClass().add("hud-foto");
        moldura.setMinSize(tamanho, tamanho);
        moldura.setMaxSize(tamanho, tamanho);

        Optional<Image> foto = FotoUsuario.imagem(usuario.getId(), tamanho * 2);
        if (foto.isPresent()) {
            ImageView iv = new ImageView(foto.get());
            iv.setFitWidth(tamanho - 6);
            iv.setFitHeight(tamanho - 6);
            iv.setPreserveRatio(false);
            Rectangle recorte = new Rectangle(tamanho - 6, tamanho - 6);
            recorte.setArcWidth(16);
            recorte.setArcHeight(16);
            iv.setClip(recorte);
            moldura.getChildren().add(iv);
        } else {
            String nome = usuario.getNome().isBlank() ? "?" : usuario.getNome().trim().substring(0, 1).toUpperCase();
            Label inicial = new Label(nome);
            inicial.getStyleClass().addAll("hud-texto", "hud-inicial");
            moldura.getChildren().add(inicial);
        }

        Label lblNivel = valorHud("NV " + nivel, "hud-nivel");
        StackPane.setAlignment(lblNivel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(lblNivel, new Insets(0, 0, -12, 0));
        moldura.getChildren().add(lblNivel);

        Tooltip.install(moldura, new Tooltip("Clique para trocar a foto"));
        moldura.setOnMouseClicked(e -> {
            if (FotoUsuario.escolher(App.getStage(), usuario.getId())) App.navegarPara("home");
        });
        return moldura;
    }

    /** Valor do painel do usuário, na fonte do dinheiro do HUD. */
    private static Label valorHud(String texto, String classe) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("hud-valor", classe);
        l.setStyle(Ui.estiloFonteValores());
        return l;
    }

    /** Texto do painel do usuário, na mesma fonte dos títulos de seção ("Saldo por Conta"). */
    private static Label textoHud(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("hud-texto");
        return l;
    }
}
