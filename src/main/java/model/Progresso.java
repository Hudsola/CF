package model;

/**
 * Nível e XP do usuário, calculados a partir dos lançamentos (ver ControleFinanceiro.calcularProgresso).
 *
 * @param xpTotal         XP acumulado desde o início
 * @param xpNoNivel       XP já conquistado dentro do nível atual
 * @param xpParaProximo   XP necessário para passar do nível atual para o próximo
 */
public record Progresso(int nivel, int xpTotal, int xpNoNivel, int xpParaProximo) {

    /** Fração do nível atual já concluída (0 a 1), para a barra de progresso. */
    public double fracaoNivel() {
        return xpParaProximo == 0 ? 0 : (double) xpNoNivel / xpParaProximo;
    }
}
