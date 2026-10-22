package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Produto do estoque da casa. O código é do MyCareer (não da loja): o mesmo produto comprado em lugares
 * diferentes fica com um código só.
 *
 * @param quantidade quantidade atual, com até 3 casas decimais (ex: 1,028 kg)
 */
public record Produto(int id, String codigo, String descricao, BigDecimal quantidade, UnidadeMedida unidade) {

    public static final int CASAS_DECIMAIS = 3;

    public Produto {
        codigo = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        descricao = descricao == null ? "" : descricao.trim();
        quantidade = (quantidade == null ? BigDecimal.ZERO : quantidade).setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);
    }

    public Produto comId(int novoId) { return new Produto(novoId, codigo, descricao, quantidade, unidade); }

    public Produto comQuantidade(BigDecimal nova) { return new Produto(id, codigo, descricao, nova, unidade); }

    public boolean semEstoque() { return quantidade.signum() == 0; }

    /** Quantidade com a unidade, sem zeros à direita: "12 un", "1,028 kg", "0,5 L". */
    public String quantidadeFormatada() {
        return formatar(quantidade) + " " + (unidade == null ? "" : unidade.sigla());
    }

    public static String formatar(BigDecimal q) {
        BigDecimal limpo = q.stripTrailingZeros();
        if (limpo.scale() < 0) limpo = limpo.setScale(0, RoundingMode.UNNECESSARY);
        return limpo.toPlainString().replace('.', ',');
    }

    @Override public String toString() { return codigo + " — " + descricao; }
}
