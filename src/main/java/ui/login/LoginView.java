package ui.login;

import db.DatabaseManager;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Usuario;
import service.Autenticacao;
import ui.App;
import ui.components.Ui;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Tela inicial: entrar (usuário/e-mail e senha ou Google), criar conta e — se houver dados de antes do
 * login — criar o acesso para esses dados.
 */
public class LoginView {

    private final Consumer<Usuario> aoEntrar;
    private final Autenticacao auth = new Autenticacao();
    private final StackPane raiz = new StackPane();

    public LoginView(Consumer<Usuario> aoEntrar) {
        this.aoEntrar = aoEntrar;
    }

    public Node getView() {
        raiz.getStyleClass().add("main-content");
        raiz.setPadding(new Insets(24));
        mostrarEntrar();
        return raiz;
    }

    // -------------------------------------------------------------------------
    // Entrar
    // -------------------------------------------------------------------------

    private void mostrarEntrar() {
        VBox card = card("Controle Financeiro", "Entre para ver as suas finanças.");

        Optional<Usuario> semAcesso = auth.perfilSemAcesso();
        semAcesso.ifPresent(perfil -> card.getChildren().add(avisoDadosExistentes(perfil)));

        TextField tfLogin = Ui.campo("Usuário ou e-mail");
        PasswordField pfSenha = new PasswordField();
        pfSenha.setPromptText("Senha");
        pfSenha.getStyleClass().add("dark-field");
        Label lblMsg = new Label();
        lblMsg.setWrapText(true);
        lblMsg.managedProperty().bind(lblMsg.textProperty().isNotEmpty());   // sem texto, não ocupa espaço

        Button btnEntrar = new Button("Entrar");
        btnEntrar.getStyleClass().add("btn-primary");
        btnEntrar.setDefaultButton(true);
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setOnAction(e -> emSegundoPlano(btnEntrar, lblMsg,
                () -> auth.entrar(tfLogin.getText(), pfSenha.getText()), aoEntrar));

        Hyperlink lnkCriar = new Hyperlink("Criar uma conta");
        lnkCriar.setOnAction(e -> mostrarCadastro(null));

        card.getChildren().addAll(
                Ui.label("Usuário ou e-mail"), tfLogin,
                Ui.label("Senha"), pfSenha,
                btnEntrar, lblMsg,
                separadorOu(),
                botaoGoogle("Entrar com Google", null, lblMsg),
                lnkCriar);
        trocar(card);
        tfLogin.requestFocus();
    }

    /** Banner para o perfil de antes do login: criar usuário/senha ou ligar ao Google mantendo os dados. */
    private Node avisoDadosExistentes(Usuario perfil) {
        Label titulo = new Label("Seus dados de antes do login estão guardados");
        titulo.getStyleClass().add("subsection-title");
        Label texto = new Label("Perfil \"" + perfil.getNome() + "\". Para continuar usando esses dados, crie o acesso "
                + "com usuário e senha ou com a sua conta Google.");
        texto.setWrapText(true);
        texto.getStyleClass().add("form-label");
        Label lblMsg = new Label();
        lblMsg.setWrapText(true);
        lblMsg.managedProperty().bind(lblMsg.textProperty().isNotEmpty());   // sem texto, não ocupa espaço

        Button btnLocal = new Button("Criar usuário e senha");
        btnLocal.getStyleClass().add("btn-info");
        btnLocal.setOnAction(e -> mostrarCadastro(perfil));
        HBox botoes = new HBox(10, btnLocal);
        if (FluxoGoogle.credenciais().isPresent()) {
            Node btnGoogle = botaoGoogle("Usar conta Google", perfil.getId(), lblMsg);
            HBox.setHgrow(btnGoogle, Priority.ALWAYS);
            botoes.getChildren().add(btnGoogle);
        }
        VBox aviso = new VBox(8, titulo, texto, botoes, lblMsg);
        aviso.getStyleClass().add("login-aviso");
        aviso.setPadding(new Insets(12));
        return aviso;
    }

    // -------------------------------------------------------------------------
    // Cadastro (conta nova ou acesso para o perfil antigo)
    // -------------------------------------------------------------------------

    /** @param perfilExistente perfil de antes do login que vai receber o acesso; nulo para conta nova */
    private void mostrarCadastro(Usuario perfilExistente) {
        boolean doPerfil = perfilExistente != null;
        VBox card = card(doPerfil ? "Criar acesso aos seus dados" : "Criar conta",
                doPerfil ? "Os lançamentos já registrados continuam com você."
                         : "A conta nova começa vazia, com as categorias padrão.");

        TextField tfNome = Ui.campo("Como quer ser chamado");
        TextField tfUsuario = Ui.campo("Usado para entrar (ex: hudson)");
        TextField tfEmail = Ui.campo("seu@email.com");
        DatePicker dpNasc = Ui.seletorData(null);
        dpNasc.setMaxWidth(Double.MAX_VALUE);
        PasswordField pfSenha = senha("Mínimo de " + Autenticacao.TAMANHO_MINIMO_SENHA + " caracteres");
        PasswordField pfConfirma = senha("Repita a senha");
        if (doPerfil) {
            tfNome.setText(perfilExistente.getNome());
            dpNasc.setValue(perfilExistente.getDataNascimento());
            if (perfilExistente.getEmail() != null) tfEmail.setText(perfilExistente.getEmail());
        }
        Label lblMsg = new Label();
        lblMsg.setWrapText(true);
        lblMsg.managedProperty().bind(lblMsg.textProperty().isNotEmpty());   // sem texto, não ocupa espaço

        Button btnCriar = new Button(doPerfil ? "Criar acesso e entrar" : "Criar conta e entrar");
        btnCriar.getStyleClass().add("btn-primary");
        btnCriar.setDefaultButton(true);
        btnCriar.setMaxWidth(Double.MAX_VALUE);
        btnCriar.setOnAction(e -> {
            if (!pfSenha.getText().equals(pfConfirma.getText())) {
                Ui.mostrarMsg(lblMsg, "As senhas não conferem.", false);
                return;
            }
            Autenticacao.Cadastro c = new Autenticacao.Cadastro(tfNome.getText(), tfUsuario.getText(),
                    tfEmail.getText(), dpNasc.getValue(), pfSenha.getText());
            emSegundoPlano(btnCriar, lblMsg,
                    () -> doPerfil ? auth.criarAcessoLocal(perfilExistente.getId(), c) : auth.cadastrar(c), aoEntrar);
        });

        Hyperlink lnkVoltar = new Hyperlink("Voltar para o login");
        lnkVoltar.setOnAction(e -> mostrarEntrar());

        card.getChildren().addAll(
                Ui.label("Nome"), tfNome,
                Ui.label("Usuário"), tfUsuario,
                Ui.label("E-mail"), tfEmail,
                Ui.label("Data de nascimento (opcional)"), dpNasc,
                Ui.label("Senha"), pfSenha,
                Ui.label("Confirmar senha"), pfConfirma,
                btnCriar, lblMsg);
        if (!doPerfil) card.getChildren().addAll(separadorOu(), botaoGoogle("Criar conta com Google", null, lblMsg));
        card.getChildren().add(lnkVoltar);
        trocar(card);
        (doPerfil ? tfUsuario : tfNome).requestFocus();
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    /** Botão do Google; sem as credenciais configuradas, vem desativado e com a explicação logo abaixo. */
    private Node botaoGoogle(String texto, Integer vincularA, Label lblMsg) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("btn-google");
        btn.setMaxWidth(Double.MAX_VALUE);
        if (FluxoGoogle.credenciais().isPresent()) {
            btn.setOnAction(e -> {
                Ui.mostrarMsg(lblMsg, "", true);
                FluxoGoogle.executar(raiz.getScene().getWindow(), vincularA, aoEntrar,
                        msg -> Ui.mostrarMsg(lblMsg, msg, false));
            });
            return btn;
        }
        btn.setDisable(true);
        Label ajuda = new Label(FluxoGoogle.comoConfigurar());
        ajuda.setWrapText(true);
        ajuda.getStyleClass().add("empty-label");
        Hyperlink abrirPasta = new Hyperlink("Abrir pasta de dados");
        abrirPasta.setOnAction(e -> App.abrirNoSistema(DatabaseManager.pastaDados()));
        VBox bloco = new VBox(4, btn, ajuda, abrirPasta);
        bloco.setFillWidth(true);
        return bloco;
    }

    /** Hashing de senha leva ~0,5 s: roda fora da thread da tela, com o botão desabilitado. */
    private void emSegundoPlano(Button botao, Label lblMsg, Callable<Usuario> acao, Consumer<Usuario> sucesso) {
        Task<Usuario> tarefa = new Task<>() {
            @Override protected Usuario call() throws Exception { return acao.call(); }
        };
        botao.setDisable(true);
        Ui.mostrarMsg(lblMsg, "Verificando…", true);
        tarefa.setOnSucceeded(e -> sucesso.accept(tarefa.getValue()));
        tarefa.setOnFailed(e -> {
            botao.setDisable(false);
            Throwable t = tarefa.getException();
            Ui.mostrarMsg(lblMsg, t.getMessage() != null ? t.getMessage() : t.toString(), false);
        });
        Thread t = new Thread(tarefa, "login");
        t.setDaemon(true);
        t.start();
    }

    private VBox card(String titulo, String subtitulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("login-titulo");
        Label lblSub = new Label(subtitulo);
        lblSub.getStyleClass().add("form-label");
        lblSub.setWrapText(true);
        VBox card = new VBox(8, lblTitulo, lblSub);
        card.getStyleClass().add("login-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(440);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private void trocar(VBox card) {
        ScrollPane scroll = new ScrollPane(new StackPane(card));
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("main-scroll");
        raiz.getChildren().setAll(scroll);
    }

    private static Node separadorOu() {
        Separator s1 = new Separator(), s2 = new Separator();
        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);
        Label ou = new Label("ou");
        ou.getStyleClass().add("form-label");
        HBox box = new HBox(8, s1, ou, s2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(6, 0, 6, 0));
        return box;
    }

    private static PasswordField senha(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.getStyleClass().add("dark-field");
        return pf;
    }
}
