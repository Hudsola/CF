package model;

import java.time.LocalDate;
import java.time.Period;

public class Usuario {
    private int id;
    private String nome;
    private LocalDate dataNascimento;
    private int nivel;
    private int xp;
    private int xpProximoNivel;

    public Usuario(String nome, LocalDate dataNascimento) {
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.nivel = 1;
        this.xp = 0;
        this.xpProximoNivel = 100;
    }

    public Usuario(int id, String nome, LocalDate dataNascimento, int nivel, int xp, int xpProximoNivel) {
        this(nome, dataNascimento);
        this.id = id;
        this.nivel = nivel;
        this.xp = xp;
        this.xpProximoNivel = xpProximoNivel;
    }

    public int getId()                    { return id; }
    public String getNome()               { return nome; }
    public LocalDate getDataNascimento()  { return dataNascimento; }
    public int getNivel()                 { return nivel; }
    public int getXp()                    { return xp; }
    public int getXpProximoNivel()        { return xpProximoNivel; }

    public int getIdade() {
        if (dataNascimento == null) return 0;
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public double getProgressoXp() {
        if (xpProximoNivel == 0) return 0;
        return (double) xp / xpProximoNivel;
    }
}
