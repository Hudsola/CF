package model;

public class Conta {
    private int id;
    private String nome;

    public Conta(String nome) { this.nome = nome; }
    public Conta(int id, String nome) { this.id = id; this.nome = nome; }

    public int getId()      { return id; }
    public String getNome() { return nome; }

    @Override public String toString() { return String.format("[%d] %s", id, nome); }
}
