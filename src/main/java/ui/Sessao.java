package ui;

import model.Usuario;
import service.ControleFinanceiro;

/** Usuário logado no app. As telas usam {@link #financeiro()} para ler e gravar só os dados dele. */
public final class Sessao {

    private static Usuario usuario;

    private Sessao() {}

    public static void iniciar(Usuario u) { usuario = u; }

    public static void encerrar() { usuario = null; }

    public static boolean ativa() { return usuario != null; }

    public static Usuario usuario() {
        if (usuario == null) throw new IllegalStateException("Nenhum usuário logado.");
        return usuario;
    }

    /** Serviço com os dados do usuário logado. */
    public static ControleFinanceiro financeiro() {
        return new ControleFinanceiro(usuario().getId());
    }
}
