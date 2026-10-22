package ui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import model.Produto;
import model.UnidadeMedida;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.Estoque;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Aba Estoque: cadastro, busca, ajuste de quantidade e o card da Home. */
class EstoqueTelaTest extends TelaTestBase {

    private static final String PROMPT_CODIGO = "Automático (ex: P0001)";
    private static final String PROMPT_DESCRICAO = "Ex: Leite integral 1L, Contra filé";
    private static final String PROMPT_QUANTIDADE = "Ex: 12 ou 1,028";

    private Usuario usuario;
    private Estoque estoque;

    @Override
    protected void prepararDados() {
        usuario = criarUsuario("Gil Matos", "gil");
        estoque = new Estoque(usuario.getId());
        estoque.cadastrar(new Produto(0, "", "Leite integral 1L", new BigDecimal("12"), UnidadeMedida.UN));
        estoque.cadastrar(new Produto(0, "", "Café", BigDecimal.ZERO, UnidadeMedida.PCT));
    }

    @BeforeEach
    void abrirEstoque() {
        entrarComo(usuario);
        clicar("ESTOQUE");
    }

    @Test
    void listaOsProdutosComQuantidadeEUnidadeEOResumo() {
        assertTrue(temTexto("Leite integral 1L"));
        assertTrue(temTexto("12 un"));
        assertTrue(temTexto("0 pct"));
        assertTrue(temTexto("2 produtos · 1 sem estoque"));
    }

    @Test
    void cadastrarComCodigoAutomaticoEQuantidadeDecimal() {
        assertTrue(campoPrompt("Automático: P0003"), "o campo de código mostra a próxima sugestão");
        preencher(PROMPT_DESCRICAO, "Contra filé");
        preencher(PROMPT_QUANTIDADE, "1,028");
        escolherUnidade(UnidadeMedida.KG);
        clicar("Salvar Produto");

        assertTrue(temTexto("✔ Salvo."));
        Produto p = buscar("Contra filé");
        assertEquals("P0003", p.codigo());
        assertEquals(0, new BigDecimal("1.028").compareTo(p.quantidade()));
        assertTrue(temTexto("1,028 kg"));
    }

    @Test
    void cadastrarComCodigoProprio() {
        preencher("Automático: P0003", "carne-contrafile");
        preencher(PROMPT_DESCRICAO, "Contra filé");
        preencher(PROMPT_QUANTIDADE, "2");
        escolherUnidade(UnidadeMedida.KG);
        clicar("Salvar Produto");
        assertEquals("CARNE-CONTRAFILE", buscar("Contra filé").codigo());
    }

    @Test
    void validacoesAparecemNaTela() {
        preencher(PROMPT_DESCRICAO, "Ovos");
        preencher(PROMPT_QUANTIDADE, "1,5");
        clicar("Salvar Produto");
        assertTrue(temTexto("✖ Em un a quantidade deve ser um número inteiro."));

        preencher(PROMPT_QUANTIDADE, "abc");
        clicar("Salvar Produto");
        assertTrue(temTextoComecandoCom("✖ Quantidade inválida"));

        preencher("Automático: P0003", "P0001");
        preencher(PROMPT_QUANTIDADE, "6");
        clicar("Salvar Produto");
        assertTrue(temTexto("✖ Já existe um produto com o código P0001."));
        assertEquals(2, estoque.listar().size());
    }

    @Test
    void buscaFiltraATabela() {
        preencher("Buscar por código ou descrição", "cafe");
        assertEquals(List.of("Café"), tabela().getItems().stream().map(Produto::descricao).toList());
        preencher("Buscar por código ou descrição", "P0001");
        assertEquals(List.of("Leite integral 1L"), tabela().getItems().stream().map(Produto::descricao).toList());
        preencher("Buscar por código ou descrição", "");
        assertEquals(2, tabela().getItems().size());
    }

    @Test
    void entradaESaidaDoProdutoSelecionado() {
        selecionar("Leite integral 1L");
        preencher("Quantidade", "6");
        clicar("+ Entrada");
        assertTrue(temTexto("✔ Entrada registrada: Leite integral 1L agora tem 18 un."));
        assertEquals("18 un", buscar("Leite integral 1L").quantidadeFormatada());

        selecionar("Leite integral 1L");
        preencher("Quantidade", "20");
        clicar("− Saída");
        assertTrue(temTexto("✖ Estoque insuficiente: há 18 un de Leite integral 1L."));

        preencher("Quantidade", "18");
        clicar("− Saída");
        assertEquals("0 un", buscar("Leite integral 1L").quantidadeFormatada());
        assertTrue(temTexto("2 produtos · 2 sem estoque"));
    }

    @Test
    void ajustarSemSelecionarMostraErro() {
        preencher("Quantidade", "1");
        clicar("+ Entrada");
        assertTrue(temTexto("✖ Selecione um produto na tabela."));
    }

    @Test
    void editarProduto() {
        selecionar("Café");
        clicar("Editar");
        preencher(PROMPT_DESCRICAO, "Café torrado 500 g");
        preencher(PROMPT_QUANTIDADE, "3");
        clicar("Atualizar Produto");
        assertTrue(temTexto("✔ Alterações salvas."));
        assertEquals("3 pct", buscar("Café torrado 500 g").quantidadeFormatada());
    }

    @Test
    void cardDaHomeMostraProdutosEOsQueAcabaram() {
        clicar("HOME");
        assertTrue(temTexto("Produtos cadastrados"));
        assertTrue(temTexto("• Café"), "produto sem estoque listado no card");
        clicar("Abrir Estoque →");
        assertTrue(temTexto("Estoque da casa"));
    }

    // --- auxiliares ---

    private Produto buscar(String descricao) {
        return estoque.listar().stream().filter(p -> p.descricao().equals(descricao)).findFirst()
                .orElseThrow(() -> new AssertionError("produto \"" + descricao + "\" não encontrado"));
    }

    @SuppressWarnings("unchecked")
    private TableView<Produto> tabela() {
        return (TableView<Produto>) exigir(n -> n instanceof TableView<?>, "tabela de produtos");
    }

    private void selecionar(String descricao) {
        TableView<Produto> t = tabela();
        interact(() -> {
            for (int i = 0; i < t.getItems().size(); i++)
                if (t.getItems().get(i).descricao().equals(descricao)) t.getSelectionModel().clearAndSelect(i);
        });
    }

    private void escolherUnidade(UnidadeMedida u) {
        @SuppressWarnings("unchecked")
        ComboBox<UnidadeMedida> c = (ComboBox<UnidadeMedida>) exigir(
                n -> n instanceof ComboBox<?> cb && cb.getValue() instanceof UnidadeMedida, "unidade");
        interact(() -> c.setValue(u));
    }

    private boolean campoPrompt(String prompt) {
        return achar(n -> n instanceof javafx.scene.control.TextField tf && prompt.equals(tf.getPromptText())).isPresent();
    }
}
