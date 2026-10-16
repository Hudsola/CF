package ui.financas.cadastros;

import service.ControleFinanceiro;
import ui.components.AbaCrud;

/** Aba de cadastro de Finanças: a base comum {@link AbaCrud} com acesso aos dados financeiros do usuário. */
abstract class AbaFinancas<T> extends AbaCrud<T> {

    protected final ControleFinanceiro cf;

    protected AbaFinancas(ControleFinanceiro cf) {
        this.cf = cf;
    }
}
