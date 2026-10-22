package service;

import model.Produto;
import repository.ProdutoRepository;

import java.math.BigDecimal;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Estoque da casa: cadastro de produtos (código do MyCareer, descrição, quantidade e unidade) e ajustes
 * manuais da quantidade (entrada e saída).
 */
public class Estoque {

    public static final int TAMANHO_MAXIMO_DESCRICAO = 100;
    /** Letras, números, ponto, hífen e _, até 20 caracteres (ex: P0001, CARNE-CONTRAFILE). */
    private static final Pattern CODIGO = Pattern.compile("[A-Z0-9._-]{1,20}");
    private static final Pattern AUTOMATICO = Pattern.compile("P(\\d+)");

    private final ProdutoRepository repo;

    public Estoque(int usuarioId) { this.repo = new ProdutoRepository(usuarioId); }

    /** Produtos em ordem alfabética de descrição (ignorando maiúsculas e acentos). */
    public List<Produto> listar() {
        Collator c = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        c.setStrength(Collator.PRIMARY);
        return repo.listarTodos().stream().sorted(Comparator.comparing(Produto::descricao, c::compare)).toList();
    }

    /** Produtos cujo código ou descrição contém o texto (ignorando maiúsculas e acentos). */
    public List<Produto> buscar(String texto) {
        String t = semAcento(texto == null ? "" : texto.trim());
        if (t.isEmpty()) return listar();
        return listar().stream()
                .filter(p -> semAcento(p.codigo()).contains(t) || semAcento(p.descricao()).contains(t)).toList();
    }

    public Optional<Produto> buscarPorId(int id) { return repo.buscar(id); }

    /** Próximo código automático livre: P0001, P0002… (o maior número em uso + 1). */
    public String proximoCodigo() {
        int maior = repo.codigosAutomaticos().stream()
                .map(AUTOMATICO::matcher).filter(java.util.regex.Matcher::matches)
                .mapToInt(m -> Integer.parseInt(m.group(1))).max().orElse(0);
        return String.format("P%04d", maior + 1);
    }

    /** Cadastra o produto; sem código, usa o próximo automático. Devolve o produto com id e código. */
    public Produto cadastrar(Produto p) {
        Produto novo = p.codigo().isEmpty() ? new Produto(0, proximoCodigo(), p.descricao(), p.quantidade(), p.unidade()) : p;
        validar(novo, 0);
        return novo.comId(repo.salvar(novo));
    }

    public void atualizar(Produto p) {
        if (repo.buscar(p.id()).isEmpty()) throw new IllegalArgumentException("Produto não encontrado.");
        validar(p, p.id());
        repo.atualizar(p);
    }

    public void excluir(int id) { repo.excluir(id); }

    /** Soma ao estoque (ex: uma compra). Devolve a nova quantidade. */
    public BigDecimal entrada(int id, BigDecimal quantidade) {
        Produto p = existente(id);
        validarMovimento(p, quantidade);
        BigDecimal nova = p.quantidade().add(quantidade);
        repo.definirQuantidade(id, nova);
        return nova;
    }

    /** Tira do estoque (ex: consumo). Não deixa ficar negativo. Devolve a nova quantidade. */
    public BigDecimal saida(int id, BigDecimal quantidade) {
        Produto p = existente(id);
        validarMovimento(p, quantidade);
        if (quantidade.compareTo(p.quantidade()) > 0)
            throw new IllegalArgumentException("Estoque insuficiente: há " + p.quantidadeFormatada() + " de " + p.descricao() + ".");
        BigDecimal nova = p.quantidade().subtract(quantidade);
        repo.definirQuantidade(id, nova);
        return nova;
    }

    private Produto existente(int id) {
        return repo.buscar(id).orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    }

    /**
     * Lê uma quantidade digitada no formato brasileiro: "1,028" = 1.028, "1.500,5" = 1500.5, "12" = 12.
     * Sem vírgula, o ponto é o separador decimal ("0.5" = 0.5).
     */
    public static BigDecimal lerQuantidade(String texto) {
        String t = texto == null ? "" : texto.trim().replace(" ", "");
        if (t.isEmpty()) throw new IllegalArgumentException("Informe a quantidade.");
        if (t.contains(",")) t = t.replace(".", "").replace(',', '.');
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantidade inválida: \"" + texto.trim() + "\". Use, por exemplo, 12 ou 1,028.");
        }
    }

    // --- Validação ---

    private void validar(Produto p, int idAtual) {
        if (!CODIGO.matcher(p.codigo()).matches())
            throw new IllegalArgumentException("Código deve ter de 1 a 20 caracteres: letras, números, ponto, hífen ou _.");
        if (repo.codigoEmUso(p.codigo(), idAtual))
            throw new IllegalArgumentException("Já existe um produto com o código " + p.codigo() + ".");
        if (p.descricao().isEmpty()) throw new IllegalArgumentException("Informe a descrição do produto.");
        if (p.descricao().length() > TAMANHO_MAXIMO_DESCRICAO)
            throw new IllegalArgumentException("Descrição com mais de " + TAMANHO_MAXIMO_DESCRICAO + " caracteres.");
        if (p.unidade() == null) throw new IllegalArgumentException("Escolha a unidade.");
        if (p.quantidade().signum() < 0) throw new IllegalArgumentException("A quantidade não pode ser negativa.");
        validarInteira(p, p.quantidade());
    }

    private static void validarMovimento(Produto p, BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0)
            throw new IllegalArgumentException("Informe uma quantidade maior que zero.");
        if (quantidade.stripTrailingZeros().scale() > Produto.CASAS_DECIMAIS)
            throw new IllegalArgumentException("Use no máximo " + Produto.CASAS_DECIMAIS + " casas decimais.");
        validarInteira(p, quantidade);
    }

    private static void validarInteira(Produto p, BigDecimal q) {
        if (p.unidade().inteira() && q.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("Em " + p.unidade().sigla() + " a quantidade deve ser um número inteiro.");
    }

    private static String semAcento(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
