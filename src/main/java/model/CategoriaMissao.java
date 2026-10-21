package model;

/** Importância da missão, como nos jogos: a principal (história) vale o dobro de XP da secundária. */
public enum CategoriaMissao {
    PRINCIPAL("Principal", 2),
    SECUNDARIA("Secundária", 1);

    private final String nome;
    private final int multiplicadorXp;

    CategoriaMissao(String nome, int multiplicadorXp) {
        this.nome = nome;
        this.multiplicadorXp = multiplicadorXp;
    }

    public String nome() { return nome; }

    /** Quantas vezes o XP base a missão rende (ver service.CalculadoraXp). */
    public int multiplicadorXp() { return multiplicadorXp; }

    @Override public String toString() { return nome; }
}
