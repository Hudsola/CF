package model;

import java.math.BigDecimal;

public class Conta {
    private int id;
    private final String nome;
    /** Saldo da conta antes do primeiro lançamento registrado no app (pode ser negativo). */
    private final BigDecimal saldoInicial;

    public Conta(String nome) { this(0, nome, Dinheiro.ZERO); }
    public Conta(int id, String nome) { this(id, nome, Dinheiro.ZERO); }

    public Conta(int id, String nome, BigDecimal saldoInicial) {
        this.id = id;
        this.nome = nome;
        this.saldoInicial = Dinheiro.arredondar(saldoInicial != null ? saldoInicial : BigDecimal.ZERO);
    }

    public int getId()                  { return id; }
    public String getNome()             { return nome; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }

    @Override public String toString() { return nome; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Conta outro && id != 0 && outro.id == id);
    }

    @Override public int hashCode() { return Integer.hashCode(id); }
}
