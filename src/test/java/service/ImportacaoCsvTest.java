package service;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.MapeamentoRepository;
import util.DatabaseTestHelper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportacaoCsvTest {

    @TempDir Path dir;

    private ControleFinanceiro cf;
    private int contaId;
    private Categoria alimentacao;
    private Categoria lazer;

    @BeforeEach
    void setUp() {
        DatabaseTestHelper.setup();
        cf = new ControleFinanceiro();
        cf.salvarConta(new Conta("Nubank"));
        contaId = cf.getContas().get(0).getId();
        alimentacao = categoria("Alimentação");
        lazer = categoria("Lazer");
    }

    private Categoria categoria(String nome) {
        return cf.getCategorias().stream().filter(c -> c.getNome().equals(nome)).findFirst().orElseThrow();
    }

    private String csv(String conteudo, Charset charset) throws Exception {
        Path p = dir.resolve("extrato.csv");
        Files.writeString(p, conteudo, charset);
        return p.toString();
    }

    @Test
    void deveLerCsvDoNubankComAcentosEmUtf8() throws Exception {
        String arq = csv("date,title,amount\n2026-07-01,Padaria São João,15.50\n2026-07-02,\"Mercado, Centro\",120.00\n",
                StandardCharsets.UTF_8);

        ResultadoLeituraCsv r = cf.lerCsv(arq, contaId);

        assertTrue(r.erros().isEmpty());
        assertEquals(2, r.linhas().size());
        assertEquals("Padaria São João", r.linhas().get(0).getTitulo());
        assertEquals("Mercado, Centro", r.linhas().get(1).getTitulo());
        util.Assercoes.assertValor("120.00", r.linhas().get(1).getValor());
    }

    @Test
    void deveLerCsvSalvoNoExcelEmWindows1252ComPontoEVirgula() throws Exception {
        String arq = csv("Data;Descrição;Valor\n01/07/2026;Açougue;1.234,56\n",
                Charset.forName("windows-1252"));

        ResultadoLeituraCsv r = cf.lerCsv(arq, contaId);

        assertTrue(r.erros().isEmpty(), r.erros().toString());
        LinhaImportacao li = r.linhas().get(0);
        assertEquals("Açougue", li.getTitulo());
        util.Assercoes.assertValor("1234.56", li.getValor());
        assertEquals(LocalDate.of(2026, 7, 1), li.getData());
    }

    @Test
    void devePularLinhasInvalidasSemAbortarAImportacao() throws Exception {
        String arq = csv("date,title,amount\n2026-07-01,Ok,10.00\nlixo,Ruim,10.00\n2026-07-03,SemValor,abc\n2026-07-04,Ok2,20.00\n",
                StandardCharsets.UTF_8);

        ResultadoLeituraCsv r = cf.lerCsv(arq, contaId);

        assertEquals(2, r.linhas().size());
        assertEquals(2, r.erros().size());
        assertTrue(r.erros().get(0).startsWith("Linha 3"));
    }

    @Test
    void deveDesmarcarEstornosEPagamentos() throws Exception {
        String arq = csv("date,title,amount\n2026-07-01,Pagamento recebido,-500.00\n", StandardCharsets.UTF_8);

        LinhaImportacao li = cf.lerCsv(arq, contaId).linhas().get(0);

        assertFalse(li.isImportar());
        assertEquals("Crédito/estorno", li.getObservacao());
    }

    @Test
    void deveImportarEAprenderMapeamento() throws Exception {
        String arq = csv("date,title,amount\n2026-07-01,iFood *Pizzaria,45.00\n", StandardCharsets.UTF_8);
        ResultadoLeituraCsv r = cf.lerCsv(arq, contaId);
        LinhaImportacao li = r.linhas().get(0);
        assertTrue(li.isMapeamentoNovo());
        li.setCategoria(alimentacao);
        li.setDetalhe("Delivery");

        assertEquals(1, cf.confirmarImportacao(r.linhas(), contaId));

        Despesa d = cf.getDespesas().get(0);
        assertEquals("Delivery", d.getDetalhamento());
        assertEquals("JULHO", d.getMes());
        assertEquals(2026, d.getAno());

        // Na próxima leitura o mapeamento já é reconhecido
        String arq2 = csv("date,title,amount\n2026-08-01,IFOOD *PIZZARIA,50.00\n", StandardCharsets.UTF_8);
        LinhaImportacao li2 = cf.lerCsv(arq2, contaId).linhas().get(0);
        assertFalse(li2.isMapeamentoNovo());
        assertEquals(alimentacao.getId(), li2.getCategoria().getId());
        assertEquals("Delivery", li2.getDetalhe());
    }

    @Test
    void deveAtualizarMapeamentoQuandoACategoriaForTrocada() throws Exception {
        new MapeamentoRepository().salvar(new MapeamentoDescricao("Cinema", alimentacao.getId(), "Cinema"));
        String arq = csv("date,title,amount\n2026-07-01,Cinema Shopping,30.00\n", StandardCharsets.UTF_8);
        ResultadoLeituraCsv r = cf.lerCsv(arq, contaId);
        r.linhas().get(0).setCategoria(lazer);

        cf.confirmarImportacao(r.linhas(), contaId);

        List<MapeamentoDescricao> maps = new MapeamentoRepository().listarTodos();
        assertEquals(1, maps.size());
        assertEquals("Cinema", maps.get(0).getPadrao());
        assertEquals(lazer.getId(), maps.get(0).getCategoriaId());
    }

    @Test
    void deveDesmarcarDespesasJaImportadas() throws Exception {
        String conteudo = "date,title,amount\n2026-07-01,Padaria,10.00\n2026-07-01,Padaria,10.00\n2026-07-02,Farmácia,30.00\n";
        String arq = csv(conteudo, StandardCharsets.UTF_8);
        ResultadoLeituraCsv primeira = cf.lerCsv(arq, contaId);
        primeira.linhas().forEach(l -> l.setCategoria(alimentacao));
        primeira.linhas().get(1).setImportar(false);       // só uma das duas padarias
        cf.confirmarImportacao(primeira.linhas(), contaId);

        List<LinhaImportacao> segunda = cf.lerCsv(arq, contaId).linhas();

        assertFalse(segunda.get(0).isImportar());          // padaria já existe
        assertTrue(segunda.get(1).isImportar());           // a segunda padaria ainda não
        assertFalse(segunda.get(2).isImportar());          // farmácia já existe
        assertEquals("Já importada?", segunda.get(2).getObservacao());
    }

    @Test
    void naoDeveGravarNadaSeAlgumaLinhaForInvalida() throws Exception {
        String arq = csv("date,title,amount\n2026-07-01,A,10.00\n2026-07-02,B,-5.00\n", StandardCharsets.UTF_8);
        List<LinhaImportacao> linhas = cf.lerCsv(arq, contaId).linhas();
        linhas.forEach(l -> { l.setCategoria(alimentacao); l.setImportar(true); });

        assertThrows(IllegalArgumentException.class, () -> cf.confirmarImportacao(linhas, contaId));
        assertTrue(cf.getDespesas().isEmpty());
    }

    @Test
    void deveDesfazerTudoSeOBancoFalharNoMeio() throws Exception {
        String arq = csv("date,title,amount\n2026-07-01,A,10.00\n2026-07-02,B,20.00\n", StandardCharsets.UTF_8);
        List<LinhaImportacao> linhas = cf.lerCsv(arq, contaId).linhas();
        linhas.get(0).setCategoria(alimentacao);
        linhas.get(1).setCategoria(new Categoria(9999, "Inexistente"));   // viola a chave estrangeira

        assertThrows(RuntimeException.class, () -> cf.confirmarImportacao(linhas, contaId));
        assertTrue(cf.getDespesas().isEmpty());
    }
}
