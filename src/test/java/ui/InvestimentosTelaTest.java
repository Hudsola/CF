package ui;

import javafx.scene.control.TableView;
import model.Conta;
import model.Dinheiro;
import model.Investimento;
import model.Receita;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ControleFinanceiro;
import ui.components.Ui;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Finanças → Cadastros → Investimentos: aplicação guardada, resgate com lucro ou prejuízo e desfazer. */
class InvestimentosTelaTest extends TelaTestBase {

    private static final String PROMPT_SACADO = "Valor sacado, ex: 5.000,00";

    private Usuario usuario;
    private ControleFinanceiro cf;
    private final LocalDate hoje = LocalDate.now();

    @Override
    protected void prepararDados() {
        usuario = criarUsuario("Helena Sá", "helena");
        cf = new ControleFinanceiro(usuario.getId());
        cf.salvarConta(new Conta("Nubank"));
        int conta = cf.getContas().get(0).getId();
        cf.salvarReceita(new Receita("Salário", new BigDecimal("10000"), conta, hoje.minusDays(10)));
        cf.salvarInvestimento(new Investimento("CDB", new BigDecimal("7000"), conta, hoje.minusDays(10)));
    }

    @BeforeEach
    void abrirInvestimentos() {
        entrarComo(usuario);
        clicar("FINANÇAS");
        clicar("Cadastros");
        clicar("Investimentos");
    }

    @Test
    void mostraOInvestimentoGuardadoEOTotalGuardado() {
        assertTrue(temTexto("Guardado"), "situação na tabela");
        assertTrue(temTexto("Guardado em investimentos: " + Dinheiro.formatar(new BigDecimal("7000"))));
    }

    @Test
    void resgateComPrejuizoLancaADespesaEMostraNaTabela() {
        selecionarPrimeiro();
        preencher(PROMPT_SACADO, "5.000,00");
        clicar("Resgatar");

        assertTrue(temTextoComecandoCom("✔ Resgate registrado: " + Dinheiro.formatar(new BigDecimal("7000"))
                + " voltaram ao disponível; o prejuízo de " + Dinheiro.formatar(new BigDecimal("2000"))));
        assertTrue(temTexto("Resgatado em " + Ui.DATA_BR.format(hoje)));
        assertTrue(temTexto("Guardado em investimentos: " + Dinheiro.formatar(BigDecimal.ZERO)));
        assertEquals(0, new BigDecimal("2000").compareTo(cf.getDespesas().get(0).getValor()));
        assertEquals(0, new BigDecimal("8000").compareTo(cf.saldoTotal()));
    }

    @Test
    void resgateComLucroLancaAReceita() {
        selecionarPrimeiro();
        preencher(PROMPT_SACADO, "7.350,00");
        clicar("Resgatar");
        assertTrue(temTextoComecandoCom("✔ Resgate registrado"));
        assertTrue(temTextoComecandoCom("✔ Resgate registrado: " + Dinheiro.formatar(new BigDecimal("7000"))
                + " voltaram ao disponível e " + Dinheiro.formatar(new BigDecimal("350"))));
        assertTrue(cf.getReceitas().stream().anyMatch(r -> r.getOrigem().equals("Lucro do investimento: CDB")));
    }

    @Test
    void desfazerResgateVoltaAoGuardado() {
        selecionarPrimeiro();
        preencher(PROMPT_SACADO, "5.000,00");
        clicar("Resgatar");
        selecionarPrimeiro();
        clicar("Desfazer resgate");

        assertTrue(temTexto("✔ Resgate desfeito: \"CDB\" voltou a ficar guardado."));
        assertTrue(cf.getDespesas().isEmpty());
        assertFalse(cf.getInvestimentos().get(0).isResgatado());
    }

    @Test
    void errosAparecemNaTela() {
        preencher(PROMPT_SACADO, "100");
        clicar("Resgatar");
        assertTrue(temTexto("✖ Selecione um investimento na tabela."));

        selecionarPrimeiro();
        preencher(PROMPT_SACADO, "abc");
        clicar("Resgatar");
        assertTrue(temTextoComecandoCom("✖ Valor inválido"));

        preencher(PROMPT_SACADO, "5.000,00");
        clicar("Resgatar");
        selecionarPrimeiro();
        clicar("Editar");
        clicar("Atualizar Investimento");
        assertTrue(temTexto("✖ Este investimento já foi resgatado. Desfaça o resgate antes de alterar."));
    }

    @Test
    void visaoGeralEHomeMostramOGuardado() {
        clicar("Visão geral");
        assertTrue(temTexto("GUARDADO EM INVESTIMENTOS"));
        assertTrue(temTexto(Dinheiro.formatar(new BigDecimal("7000"))));
        clicar("HOME");
        assertTrue(temTexto("INVESTIMENTOS"));
        assertTrue(temTexto("Guardado"));
        assertTrue(temTexto(Dinheiro.formatar(new BigDecimal("3000"))), "saldo disponível no painel: 10k − 7k");
    }

    private void selecionarPrimeiro() {
        @SuppressWarnings("unchecked")
        TableView<Investimento> t = (TableView<Investimento>) exigir(n -> n instanceof TableView<?>, "tabela de investimentos");
        interact(() -> t.getSelectionModel().clearAndSelect(0));
    }
}
