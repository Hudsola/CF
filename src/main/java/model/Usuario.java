package model;

import java.time.LocalDate;
import java.time.Period;

/**
 * Usuário do app. O hash da senha nunca sai do repositório: aqui só se sabe se existe senha local
 * e se há uma conta Google vinculada.
 */
public class Usuario {
    private final int id;
    private final String nome;
    private final LocalDate dataNascimento;
    private final String usuario;
    private final String email;
    private final boolean temSenha;
    private final boolean googleVinculado;

    public Usuario(int id, String nome, LocalDate dataNascimento, String usuario, String email,
                   boolean temSenha, boolean googleVinculado) {
        this.id = id;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.usuario = usuario;
        this.email = email;
        this.temSenha = temSenha;
        this.googleVinculado = googleVinculado;
    }

    public int getId()                    { return id; }
    public String getNome()               { return nome; }
    public LocalDate getDataNascimento()  { return dataNascimento; }
    /** Nome de login (pode ser nulo no perfil antigo, antes de o acesso ser criado). */
    public String getUsuario()            { return usuario; }
    public String getEmail()              { return email; }
    public boolean temSenha()             { return temSenha; }
    public boolean isGoogleVinculado()    { return googleVinculado; }

    /** Perfil criado antes do login existir: tem dados, mas ainda não tem senha nem Google. */
    public boolean semAcesso()            { return !temSenha && !googleVinculado; }

    public int getIdade() {
        if (dataNascimento == null) return 0;
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }
}
