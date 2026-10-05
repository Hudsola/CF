package ui;

import org.junit.jupiter.api.Test;
import repository.UsuarioRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Tela de login: entrar, criar conta, erros mostrados ao usuário e sair. */
class LoginTelaTest extends TelaTestBase {

    @Override
    protected void prepararDados() {
        criarUsuario("Ana Teste", "ana");
    }

    @Test
    void abreNaTelaDeLoginSemMostrarDadosFinanceiros() {
        assertNotNull(botao("Entrar"));
        assertFalse(Sessao.ativa());
        assertFalse(temTexto("HOME"), "a barra de navegação só aparece depois do login");
    }

    @Test
    void entrarComUsuarioESenhaAbreAHomeESairVoltaParaOLogin() throws Exception {
        preencher("Usuário ou e-mail", "ana");
        preencher("Senha", SENHA);
        clicar("Entrar");
        esperar(() -> temTexto("Olá, Ana Teste"));

        assertTrue(temTexto("Ana Teste"), "nome no painel do usuário");
        assertTrue(temTexto("Saldo por Conta"));

        clicar("Sair");
        assertNotNull(botao("Entrar"));
        assertFalse(Sessao.ativa());
    }

    @Test
    void entrarPeloEmail() throws Exception {
        preencher("Usuário ou e-mail", "ANA@teste.com");
        preencher("Senha", SENHA);
        clicar("Entrar");
        esperar(() -> temTexto("Olá, Ana Teste"));
    }

    @Test
    void senhaErradaMostraMensagemENaoEntra() throws Exception {
        preencher("Usuário ou e-mail", "ana");
        preencher("Senha", "senha-errada");
        clicar("Entrar");
        esperar(() -> temTexto("Usuário ou senha incorretos."));
        assertFalse(Sessao.ativa());
        assertFalse(botao("Entrar").isDisabled(), "o botão volta a funcionar para tentar de novo");
    }

    @Test
    void camposVaziosMostramMensagem() throws Exception {
        clicar("Entrar");
        esperar(() -> temTexto("Informe o usuário (ou e-mail) e a senha."));
    }

    @Test
    void criarContaPelaTelaEntraNoApp() throws Exception {
        clicar("Criar uma conta");
        preencher("Como quer ser chamado", "Bruno Lima");
        preencher("Usado para entrar (ex: hudson)", "bruno");
        preencher("seu@email.com", "bruno@teste.com");
        preencher("Mínimo de 8 caracteres", "outra-senha-9");
        preencher("Repita a senha", "outra-senha-9");
        clicar("Criar conta e entrar");
        esperar(() -> temTexto("Olá, Bruno Lima"));

        assertTrue(new UsuarioRepository().buscarPorLogin("bruno").isPresent());
    }

    @Test
    void cadastroComSenhasDiferentesNaoCriaAConta() {
        clicar("Criar uma conta");
        preencher("Como quer ser chamado", "Bruno Lima");
        preencher("Usado para entrar (ex: hudson)", "bruno");
        preencher("seu@email.com", "bruno@teste.com");
        preencher("Mínimo de 8 caracteres", "outra-senha-9");
        preencher("Repita a senha", "diferente-123");
        clicar("Criar conta e entrar");

        assertTrue(temTexto("As senhas não conferem."));
        assertTrue(new UsuarioRepository().buscarPorLogin("bruno").isEmpty());
    }

    @Test
    void cadastroComUsuarioRepetidoMostraOErro() throws Exception {
        clicar("Criar uma conta");
        preencher("Como quer ser chamado", "Outra Ana");
        preencher("Usado para entrar (ex: hudson)", "ana");
        preencher("seu@email.com", "outra@teste.com");
        preencher("Mínimo de 8 caracteres", "outra-senha-9");
        preencher("Repita a senha", "outra-senha-9");
        clicar("Criar conta e entrar");
        esperar(() -> temTexto("Esse nome de usuário já está em uso."));
        assertFalse(Sessao.ativa());
    }

    @Test
    void voltarDoCadastroParaOLogin() {
        clicar("Criar uma conta");
        assertNotNull(botao("Criar conta e entrar"));
        clicar("Voltar para o login");
        assertNotNull(botao("Entrar"));
    }
}
