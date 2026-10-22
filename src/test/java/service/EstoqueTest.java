package service;

import model.Produto;
import model.UnidadeMedida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstoqueTest {

    private int usuario;
    private Estoque estoque;

    @BeforeEach
    void setUp() {
        usuario = DatabaseTestHelper.setup();
        estoque = new Estoque(usuario);
    }

    private static Produto produto(String codigo, String descricao, String quantidade, UnidadeMedida unidade) {
        return new Produto(0, codigo, descricao, new BigDecimal(quantidade), unidade);
    }

    @Test
    void cadastraComCodigoAutomaticoSequencialOuDigitado() {
        assertEquals("P0001", estoque.proximoCodigo());
        Produto leite = estoque.cadastrar(produto("", "Leite integral 1L", "12", UnidadeMedida.UN));
        assertEquals("P0001", leite.codigo());
        assertEquals("P0002", estoque.proximoCodigo());

        Produto carne = estoque.cadastrar(produto(" carne-contrafile ", "Contra filé", "1.028", UnidadeMedida.KG));
        assertEquals("CARNE-CONTRAFILE", carne.codigo(), "código em maiúsculas e sem espaços");
        assertEquals("P0002", estoque.proximoCodigo(), "códigos digitados não contam na sequência");

        estoque.cadastrar(produto("P0010", "Arroz 5kg", "1", UnidadeMedida.PCT));
        assertEquals("P0011", estoque.proximoCodigo(), "continua do maior número usado");
    }

    @Test
    void guardaQuantidadeDecimalExataEFormata() {
        Produto carne = estoque.cadastrar(produto("", "Contra filé", "1.028", UnidadeMedida.KG));
        Produto lido = estoque.buscarPorId(carne.id()).orElseThrow();
        assertEquals(0, new BigDecimal("1.028").compareTo(lido.quantidade()));
        assertEquals("1,028 kg", lido.quantidadeFormatada());
        assertEquals("12 un", produto("X", "Leite", "12", UnidadeMedida.UN).quantidadeFormatada());
        assertEquals("0,5 L", produto("X", "Suco", "0.5", UnidadeMedida.L).quantidadeFormatada());
        assertEquals("0 cx", produto("X", "Ovos", "0", UnidadeMedida.CX).quantidadeFormatada());
    }

    @Test
    void listaEmOrdemAlfabeticaEBuscaPorCodigoOuDescricaoSemAcento() {
        estoque.cadastrar(produto("", "Feijão", "2", UnidadeMedida.PCT));
        estoque.cadastrar(produto("", "Açúcar", "1", UnidadeMedida.KG));
        estoque.cadastrar(produto("CAFE", "Café", "500", UnidadeMedida.G));

        assertEquals(List.of("Açúcar", "Café", "Feijão"), estoque.listar().stream().map(Produto::descricao).toList());
        assertEquals(List.of("Feijão"), estoque.buscar("feijao").stream().map(Produto::descricao).toList());
        assertEquals(List.of("Açúcar"), estoque.buscar("ACUCAR").stream().map(Produto::descricao).toList());
        assertEquals(List.of("Café"), estoque.buscar("cafe").stream().map(Produto::descricao).toList(), "pelo código");
        assertEquals(3, estoque.buscar("  ").size());
    }

    @Test
    void alteraEExclui() {
        Produto p = estoque.cadastrar(produto("", "Leite", "1", UnidadeMedida.UN));
        estoque.atualizar(new Produto(p.id(), "LEITE-1L", "Leite integral 1L", new BigDecimal("6"), UnidadeMedida.UN));
        Produto lido = estoque.buscarPorId(p.id()).orElseThrow();
        assertEquals("LEITE-1L", lido.codigo());
        assertEquals("6 un", lido.quantidadeFormatada());

        estoque.excluir(p.id());
        assertTrue(estoque.listar().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> estoque.atualizar(lido));
    }

    @Test
    void validaOsCampos() {
        estoque.cadastrar(produto("P0001", "Leite", "1", UnidadeMedida.UN));
        assertErro("Já existe um produto com o código P0001", produto("p0001", "Outro", "1", UnidadeMedida.UN));
        assertErro("Código deve ter", produto("COM ESPAÇO", "X", "1", UnidadeMedida.UN));
        assertErro("Código deve ter", produto("A".repeat(21), "X", "1", UnidadeMedida.UN));
        assertErro("Informe a descrição", produto("X1", "  ", "1", UnidadeMedida.UN));
        assertErro("mais de 100", produto("X1", "d".repeat(101), "1", UnidadeMedida.UN));
        assertErro("Escolha a unidade", produto("X1", "Leite", "1", null));
        assertErro("não pode ser negativa", produto("X1", "Leite", "-1", UnidadeMedida.UN));
        assertErro("número inteiro", produto("X1", "Leite", "1.5", UnidadeMedida.UN));
        assertEquals(1, estoque.listar().size(), "nada inválido foi gravado");

        estoque.cadastrar(produto("X1", "Carne", "1.5", UnidadeMedida.KG));   // fração vale para peso
        estoque.cadastrar(produto("X2", "d".repeat(100), "0", UnidadeMedida.CX));
    }

    private void assertErro(String trecho, Produto p) {
        var ex = assertThrows(IllegalArgumentException.class, () -> estoque.cadastrar(p));
        assertTrue(ex.getMessage().contains(trecho), "esperado \"" + trecho + "\", veio \"" + ex.getMessage() + "\"");
    }

    @Test
    void entradaESaidaAjustamAQuantidadeSemDeixarNegativa() {
        Produto carne = estoque.cadastrar(produto("", "Contra filé", "1.028", UnidadeMedida.KG));
        assertEquals(0, new BigDecimal("2.028").compareTo(estoque.entrada(carne.id(), BigDecimal.ONE)));
        assertEquals(0, new BigDecimal("1.528").compareTo(estoque.saida(carne.id(), new BigDecimal("0.5"))));

        var ex = assertThrows(IllegalArgumentException.class, () -> estoque.saida(carne.id(), new BigDecimal("2")));
        assertEquals("Estoque insuficiente: há 1,528 kg de Contra filé.", ex.getMessage());
        assertEquals(0, BigDecimal.ZERO.compareTo(estoque.saida(carne.id(), new BigDecimal("1.528"))), "pode zerar");
        assertTrue(estoque.buscarPorId(carne.id()).orElseThrow().semEstoque());
    }

    @Test
    void movimentoInvalidoEhRecusado() {
        Produto leite = estoque.cadastrar(produto("", "Leite", "2", UnidadeMedida.UN));
        assertThrows(IllegalArgumentException.class, () -> estoque.entrada(leite.id(), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> estoque.entrada(leite.id(), new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> estoque.entrada(leite.id(), null));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> estoque.entrada(leite.id(), new BigDecimal("0.5")))
                .getMessage().contains("número inteiro"));
        Produto carne = estoque.cadastrar(produto("", "Carne", "1", UnidadeMedida.KG));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> estoque.entrada(carne.id(), new BigDecimal("0.0001")))
                .getMessage().contains("3 casas"));
        assertThrows(IllegalArgumentException.class, () -> estoque.saida(999, BigDecimal.ONE));
        assertEquals("2 un", estoque.buscarPorId(leite.id()).orElseThrow().quantidadeFormatada());
    }

    @Test
    void lerQuantidadeNoFormatoBrasileiro() {
        assertEquals(new BigDecimal("1.028"), Estoque.lerQuantidade("1,028"));
        assertEquals(new BigDecimal("1500.5"), Estoque.lerQuantidade("1.500,5"));
        assertEquals(new BigDecimal("12"), Estoque.lerQuantidade(" 12 "));
        assertEquals(new BigDecimal("0.5"), Estoque.lerQuantidade("0.5"));
        for (String invalido : new String[]{"", "  ", "abc", "1,2,3"})
            assertThrows(IllegalArgumentException.class, () -> Estoque.lerQuantidade(invalido), "\"" + invalido + "\"");
        assertThrows(IllegalArgumentException.class, () -> Estoque.lerQuantidade(null));
    }

    @Test
    void produtosSaoSeparadosPorUsuario() {
        Produto meu = estoque.cadastrar(produto("", "Leite", "2", UnidadeMedida.UN));
        Estoque dele = new Estoque(DatabaseTestHelper.novoUsuario("outro"));

        assertTrue(dele.listar().isEmpty());
        assertEquals("P0001", dele.proximoCodigo(), "cada usuário tem a sua sequência");
        dele.cadastrar(produto("P0001", "Pão", "1", UnidadeMedida.UN));   // mesmo código, outro usuário
        assertThrows(IllegalArgumentException.class, () -> dele.entrada(meu.id(), BigDecimal.ONE));
        assertThrows(RuntimeException.class, () -> dele.excluir(meu.id()));
        assertEquals("2 un", estoque.buscarPorId(meu.id()).orElseThrow().quantidadeFormatada());
    }
}
