package model;

/** Unidade em que a quantidade de um produto do estoque é contada. */
public enum UnidadeMedida {
    UN("un", "unidade", true),
    CX("cx", "caixa", true),
    PCT("pct", "pacote", true),
    KG("kg", "quilo", false),
    G("g", "grama", false),
    L("L", "litro", false),
    ML("ml", "mililitro", false);

    private final String sigla;
    private final String nome;
    private final boolean inteira;

    UnidadeMedida(String sigla, String nome, boolean inteira) {
        this.sigla = sigla;
        this.nome = nome;
        this.inteira = inteira;
    }

    public String sigla() { return sigla; }

    /** Unidades contadas (un, caixa, pacote) só aceitam quantidades inteiras; peso e volume aceitam frações. */
    public boolean inteira() { return inteira; }

    @Override public String toString() { return sigla + " (" + nome + ")"; }
}
