package ui.home;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Usuario;
import service.Autenticacao;
import service.FotoPerfil;
import ui.App;
import ui.Sessao;
import ui.components.Ui;
import ui.login.FluxoGoogle;

/** Perfil do usuário logado: nome, e-mail, nascimento, foto, senha e vínculo com a conta Google. */
final class PerfilDialog {

    private PerfilDialog() {}

    /** Mostra o diálogo; devolve true se algo foi alterado (a tela deve ser recarregada). */
    static boolean editar(Usuario u) {
        Autenticacao auth = new Autenticacao();
        boolean[] alterou = {false};

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Meu perfil");
        dialog.setHeaderText(u.getUsuario() != null ? "Usuário: " + u.getUsuario() : null);
        dialog.initOwner(App.getStage());

        // --- Dados pessoais ---
        TextField tfNome = new TextField(u.getNome());
        TextField tfEmail = new TextField(u.getEmail() != null ? u.getEmail() : "");
        DatePicker dpNasc = Ui.seletorData(u.getDataNascimento());
        Label lblErro = new Label();
        lblErro.setWrapText(true);
        GridPane dados = grade();
        dados.addRow(0, new Label("Nome:"), tfNome);
        dados.addRow(1, new Label("E-mail:"), tfEmail);
        dados.addRow(2, new Label("Nascimento:"), dpNasc);

        // --- Senha ---
        Label lblSenha = new Label(u.temSenha() ? "Alterar senha" : "Criar senha (para entrar também sem o Google)");
        lblSenha.getStyleClass().add("subsection-title");
        PasswordField pfAtual = new PasswordField();
        TextField tfUsuario = new TextField();
        PasswordField pfNova = new PasswordField();
        PasswordField pfConfirma = new PasswordField();
        GridPane senha = grade();
        int linha = 0;
        if (u.temSenha()) senha.addRow(linha++, new Label("Senha atual:"), pfAtual);
        if (u.getUsuario() == null || u.getUsuario().isBlank()) senha.addRow(linha++, new Label("Usuário:"), tfUsuario);
        senha.addRow(linha++, new Label("Nova senha:"), pfNova);
        senha.addRow(linha, new Label("Confirmar:"), pfConfirma);
        Label lblMsgSenha = new Label();
        lblMsgSenha.setWrapText(true);
        Button btnSenha = new Button(u.temSenha() ? "Alterar senha" : "Criar senha");
        btnSenha.getStyleClass().add("btn-info");
        btnSenha.setOnAction(e -> {
            if (!pfNova.getText().equals(pfConfirma.getText())) {
                Ui.mostrarMsg(lblMsgSenha, "As senhas não conferem.", false);
                return;
            }
            try {
                auth.definirSenha(u.getId(), pfAtual.getText(), pfNova.getText(), tfUsuario.getText());
                pfAtual.clear(); pfNova.clear(); pfConfirma.clear();
                alterou[0] = true;
                Ui.mostrarMsg(lblMsgSenha, "Senha salva.", true);
            } catch (Exception ex) {
                Ui.mostrarMsg(lblMsgSenha, ex.getMessage(), false);
            }
        });

        // --- Google ---
        Label lblGoogle = new Label("Conta Google");
        lblGoogle.getStyleClass().add("subsection-title");
        Label lblMsgGoogle = new Label(u.isGoogleVinculado() ? "✔ Conta Google vinculada: você pode entrar com ela." : "");
        lblMsgGoogle.setWrapText(true);
        VBox google = new VBox(6, lblGoogle, lblMsgGoogle);
        if (!u.isGoogleVinculado()) {
            Button btnGoogle = new Button("Vincular conta Google");
            btnGoogle.getStyleClass().add("btn-google");
            if (FluxoGoogle.credenciais().isEmpty()) {
                btnGoogle.setDisable(true);
                Ui.mostrarMsg(lblMsgGoogle, FluxoGoogle.comoConfigurar(), false);
            }
            btnGoogle.setOnAction(e -> FluxoGoogle.executar(dialog.getDialogPane().getScene().getWindow(), u.getId(),
                    atualizado -> {
                        alterou[0] = true;
                        btnGoogle.setDisable(true);
                        Ui.mostrarMsg(lblMsgGoogle, "✔ Conta Google vinculada.", true);
                    },
                    msg -> Ui.mostrarMsg(lblMsgGoogle, msg, false)));
            google.getChildren().add(1, btnGoogle);
        }

        // --- Foto ---
        Label lblFoto = new Label("Foto de perfil");
        lblFoto.getStyleClass().add("subsection-title");
        Label lblMsgFoto = new Label(FotoPerfil.temFotoEnviada(u.getId()) ? "Usando a foto enviada por você."
                : FotoPerfil.arquivo(u.getId()).isPresent() ? "Usando a foto da conta Google." : "Nenhuma foto.");
        lblMsgFoto.setWrapText(true);
        Button btnFoto = new Button("Escolher foto…");
        btnFoto.getStyleClass().add("btn-info");
        Button btnRemoverFoto = new Button("Remover foto enviada");
        btnRemoverFoto.getStyleClass().add("btn-secondary");
        btnRemoverFoto.setDisable(!FotoPerfil.temFotoEnviada(u.getId()));
        btnFoto.setOnAction(e -> {
            if (FotoUsuario.escolher(dialog.getDialogPane().getScene().getWindow(), u.getId())) {
                alterou[0] = true;
                btnRemoverFoto.setDisable(false);
                Ui.mostrarMsg(lblMsgFoto, "Foto salva.", true);
            }
        });
        btnRemoverFoto.setOnAction(e -> {
            try {
                FotoPerfil.removerEnviada(u.getId());
                alterou[0] = true;
                btnRemoverFoto.setDisable(true);
                Ui.mostrarMsg(lblMsgFoto, FotoPerfil.arquivo(u.getId()).isPresent()
                        ? "Foto removida: voltando para a foto da conta Google." : "Foto removida.", true);
            } catch (RuntimeException ex) {
                Ui.mostrarMsg(lblMsgFoto, ex.getMessage(), false);
            }
        });
        VBox foto = new VBox(6, lblFoto, new HBox(8, btnFoto, btnRemoverFoto), lblMsgFoto);

        VBox conteudo = new VBox(10, dados, lblErro, new Separator(), foto, new Separator(), lblSenha, senha, btnSenha, lblMsgSenha,
                new Separator(), google);
        conteudo.setPadding(new Insets(10));
        conteudo.setPrefWidth(480);
        dialog.getDialogPane().setContent(conteudo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Salva os dados pessoais ao clicar OK; se der erro, mostra e mantém o diálogo aberto.
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Salvar");
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            try {
                auth.atualizarPerfil(u.getId(), tfNome.getText(), tfEmail.getText(), dpNasc.getValue());
                alterou[0] = true;
            } catch (Exception ex) {
                Ui.mostrarMsg(lblErro, ex.getMessage(), false);
                e.consume();
            }
        });
        dialog.showAndWait();

        if (alterou[0]) Sessao.iniciar(Sessao.financeiro().getUsuario());   // recarrega nome/e-mail da sessão
        return alterou[0];
    }

    private static GridPane grade() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        return g;
    }
}
