package model;

public class Categoria {
    private int id;
    private String nome;

    public Categoria(String nome) { this.nome = nome; }
    public Categoria(int id, String nome) { this.id = id; this.nome = nome; }

    public int getId()      { return id; }
    public String getNome() { return nome; }

    @Override public String toString() { return nome; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Categoria outro && id != 0 && outro.id == id);
    }

    @Override public int hashCode() { return Integer.hashCode(id); }
}
