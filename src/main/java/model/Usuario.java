package model;

import java.time.LocalDate;
import java.time.Period;

public class Usuario {
    private final int id;
    private final String nome;
    private final LocalDate dataNascimento;

    public Usuario(int id, String nome, LocalDate dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
    }

    public int getId()                    { return id; }
    public String getNome()               { return nome; }
    public LocalDate getDataNascimento()  { return dataNascimento; }

    public int getIdade() {
        if (dataNascimento == null) return 0;
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }
}
