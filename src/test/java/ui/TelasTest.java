package ui;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import model.Conta;
import model.Dinheiro;
import model.Receita;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ControleFinanceiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.*;

/** Telas com o usuário logado: Home (painel HUD), navegação, cadastros pela tela e Resumo. */
class TelasTest extends TelaTestBase {

    private Usuario usuario;

    @Override
    protected void prepararDados() {
        usuario = criarUsuario("Carla Dias", "carla");
        ControleFinanceiro cf = new ControleFinanceiro(usuario.getId());
        cf.salvarConta(new Conta(0, "Nubank", new BigDecimal("1000.00")));
        int contaId = cf.getContas().get(0).getId();
        cf.salvarReceita(new Receita(0, "Salário", new BigDecimal("2500.00"), contaId, "Nubank", LocalDate.now()));
    }

    @BeforeEach
    void entrar() {
        entrarComo(usuario);
    }

    @Test
    void homeMostraOPainelDoUsuarioComNomeIdadeXpESaldo() {
        assertTrue(temTexto("Carla Dias"));
        int idade = Period.between(LocalDate.of(1990, 6, 15), LocalDate.now()).getYears();
        assertTrue(temTexto(String.valueOf(idade)), "idade no lugar do colete");
        assertTrue(temTexto(Dinheiro.formatar(new BigDecimal("3500.00"))), "saldo geral = 1.000 + 2.500");
        assertTrue(temTexto("NV 1"), "nível na moldura da foto");

        ProgressBar xp = (ProgressBar) exigir(n -> n.getStyleClass().contains("hud-barra-xp"), "barra de XP");
        assertTrue(xp.getProgress() > 0 && xp.getProgress() < 1, "1 lançamento = 10 de 100 XP");
        assertTrue(temTexto("Saldo por Conta"));
        assertTrue(temTexto("Lançamentos Fixos Ativos"));
    }

    @Test
    void navegaPorTodasAsTelasEAbas() {
        clicar("CADASTROS");
        for (String aba : new String[]{"Receitas", "Despesas", "Investimentos", "Fixos", "Categorias", "Contas"}) {
            clicar(aba);
            assertNotNull(exigir(n -> n instanceof TableView<?>, "tabela da aba " + aba));
        }
        clicar("RESUMO");
        assertTrue(temTexto("Resumo Mês a Mês"));
        assertTrue(temTexto("Movimento por Conta"));
        clicar("HOME");
        assertTrue(temTexto("Carla Dias"));
    }

    @Test
    void cadastrarReceitaPelaTelaApareceNaTabelaENoBanco() {
        clicar("CADASTROS");
        clicar("Receitas");
        preencher("Ex: Salário, Freelance", "Freelance");
        preencher("Ex: 1.500,00", "1.234,56");
        selecionarPrimeiraOpcao("Selecione a conta");
        clicar("Salvar Receita");

        assertTrue(temTexto("✔ Salvo."));
        assertEquals(2, tabelaVisivel().getItems().size());
        assertTrue(new ControleFinanceiro(usuario.getId()).getReceitas().stream()
                .anyMatch(r -> r.getOrigem().equals("Freelance") && r.getValor().compareTo(new BigDecimal("1234.56")) == 0));
    }

    @Test
    void receitaSemContaMostraErroENaoSalva() {
        clicar("CADASTROS");
        clicar("Receitas");
        preencher("Ex: Salário, Freelance", "Freelance");
        preencher("Ex: 1.500,00", "100,00");
        clicar("Salvar Receita");

        assertTrue(temTexto("✖ Selecione uma conta."));
        assertEquals(1, new ControleFinanceiro(usuario.getId()).getReceitas().size());
    }

    @Test
    void valorInvalidoMostraErro() {
        clicar("CADASTROS");
        clicar("Receitas");
        preencher("Ex: Salário, Freelance", "Freelance");
        preencher("Ex: 1.500,00", "abc");
        selecionarPrimeiraOpcao("Selecione a conta");
        clicar("Salvar Receita");

        assertTrue(temTextoComecandoCom("✖"));
        assertEquals(1, new ControleFinanceiro(usuario.getId()).getReceitas().size());
    }

    @Test
    void editarReceitaPelaTela() {
        clicar("CADASTROS");
        clicar("Receitas");
        TableView<?> tabela = tabelaVisivel();
        interact(() -> tabela.getSelectionModel().select(0));
        clicar("Editar");
        assertNotNull(botao("Atualizar Receita"));
        preencher("Ex: Salário, Freelance", "Salário de outubro");
        clicar("Atualizar Receita");

        assertTrue(temTexto("✔ Alterações salvas."));
        assertEquals("Salário de outubro", new ControleFinanceiro(usuario.getId()).getReceitas().get(0).getOrigem());
    }

    @Test
    void editarSemSelecionarMostraErro() {
        clicar("CADASTROS");
        clicar("Receitas");
        clicar("Editar");
        assertTrue(temTexto("✖ Selecione um item na tabela."));
    }

    @Test
    void cadastrarContaComSaldoInicialAtualizaOSaldoNaHome() {
        clicar("CADASTROS");
        clicar("Contas");
        preencher("Nome da conta (ex: Nubank, Itaú)", "Itaú");
        preencher("Opcional. Ex: 1.250,00 ou -300,00", "-500,00");
        clicar("Salvar Conta");
        assertTrue(temTexto("✔ Salvo."));

        clicar("HOME");
        assertTrue(temTexto(Dinheiro.formatar(new BigDecimal("3000.00"))), "3.500 - 500 da conta nova");
    }

    @Test
    void contaComNomeRepetidoMostraErro() {
        clicar("CADASTROS");
        clicar("Contas");
        preencher("Nome da conta (ex: Nubank, Itaú)", "nubank");
        clicar("Salvar Conta");
        assertTrue(temTextoComecandoCom("✖"));
        assertEquals(1, new ControleFinanceiro(usuario.getId()).getContas().size());
    }

    private TableView<?> tabelaVisivel() {
        return (TableView<?>) exigir(n -> n instanceof TableView<?>, "tabela");
    }

    private void selecionarPrimeiraOpcao(String prompt) {
        Node n = exigir(x -> x instanceof ComboBox<?> c && prompt.equals(c.getPromptText()), "combo \"" + prompt + "\"");
        interact(() -> ((ComboBox<?>) n).getSelectionModel().select(0));
    }
}
