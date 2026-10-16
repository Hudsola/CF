package model;

/** Como uma missão se repete (ou não) no tempo. */
public enum TipoMissao {
    /** Em um ou mais dias escolhidos (ex: levar o carro na revisão em 17/10). */
    DIARIA("Diária"),
    /** Em dias da semana fixos (ex: academia segunda, quarta e sexta). */
    SEMANAL("Semanal"),
    /** Num dia do mês (ex: pagar a fatura todo dia 5). */
    MENSAL("Mensal"),
    /** Objetivo sem data (ex: juntar R$ 10 mil, aprender inglês); concluído uma única vez. */
    SEM_PRAZO("Sem prazo");

    private final String nome;

    TipoMissao(String nome) { this.nome = nome; }

    public String nome() { return nome; }

    @Override public String toString() { return nome; }
}
