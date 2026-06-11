import db.DatabaseManager;
import model.*;
import model.LancamentoFixo.Tipo;
import service.ControleFinanceiro;
import util.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final InputValidator iv = new InputValidator(sc);
    private static final ControleFinanceiro cf = new ControleFinanceiro();

    public static void main(String[] args) {
        DatabaseManager.inicializar();
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   CONTROLE FINANCEIRO PESSOAL          ║");
        System.out.println("╚════════════════════════════════════════╝");

        boolean rodando = true;
        while (rodando) {
            exibirMenuPrincipal();
            int opcao = iv.lerIntNoIntervalo("Opção", 0, 7);
            switch (opcao) {
                case 1 -> menuCategorias();
                case 2 -> menuContas();
                case 3 -> menuReceitas();
                case 4 -> menuDespesas();
                case 5 -> menuInvestimentos();
                case 6 -> menuFixos();
                case 7 -> menuRelatorios();
                case 0 -> rodando = false;
            }
        }
        System.out.println("\nAté logo!");
        sc.close();
    }

    static void exibirMenuPrincipal() {
        System.out.println("\n──────────────────────────────────────────");
        System.out.println(" 1. Categorias");
        System.out.println(" 2. Contas");
        System.out.println(" 3. Receitas");
        System.out.println(" 4. Despesas");
        System.out.println(" 5. Investimentos");
        System.out.println(" 6. Lançamentos Fixos");
        System.out.println(" 7. Relatórios");
        System.out.println(" 0. Sair");
        System.out.println("──────────────────────────────────────────");
    }

    // =========================================================================
    // CATEGORIAS
    // =========================================================================

    static void menuCategorias() {
        System.out.println("\n─── CATEGORIAS ───");
        System.out.println(" 1. Adicionar  2. Listar  3. Excluir  0. Voltar");
        int op = iv.lerIntNoIntervalo("Opção", 0, 3);
        switch (op) {
            case 1 -> {
                do {
                    try {
                        String nome = iv.lerTexto("Nome da categoria");
                        cf.salvarCategoria(new Categoria(nome));
                        System.out.println("✔ Categoria \"" + nome + "\" salva!");
                    } catch (RuntimeException e) {
                        System.out.println("  ✖ " + e.getMessage());
                    }
                } while (iv.confirmar("Deseja cadastrar outra categoria?"));
            }
            case 2 -> listarCategorias();
            case 3 -> {
                listarCategorias();
                if (cf.getCategorias().isEmpty()) return;
                int id = iv.lerInt("ID da categoria a excluir (0 para cancelar)");
                if (id != 0) {
                    try { cf.excluirCategoria(id); System.out.println("✔ Categoria excluída."); }
                    catch (RuntimeException e) { System.out.println("  ✖ " + e.getMessage()); }
                }
            }
        }
    }

    static void listarCategorias() {
        List<Categoria> lista = cf.getCategorias();
        if (lista.isEmpty()) { System.out.println("Nenhuma categoria cadastrada."); return; }
        System.out.println("\n[ Categorias ]");
        lista.forEach(System.out::println);
    }

    // =========================================================================
    // CONTAS
    // =========================================================================

    static void menuContas() {
        System.out.println("\n─── CONTAS ───");
        System.out.println(" 1. Adicionar  2. Listar  3. Excluir  0. Voltar");
        int op = iv.lerIntNoIntervalo("Opção", 0, 3);
        switch (op) {
            case 1 -> {
                do {
                    try {
                        String nome = iv.lerTexto("Nome da conta (ex: Nubank, Itaú, Carteira)");
                        cf.salvarConta(new Conta(nome));
                        System.out.println("✔ Conta \"" + nome + "\" salva!");
                    } catch (RuntimeException e) {
                        System.out.println("  ✖ " + e.getMessage());
                    }
                } while (iv.confirmar("Deseja cadastrar outra conta?"));
            }
            case 2 -> listarContas();
            case 3 -> {
                listarContas();
                if (cf.getContas().isEmpty()) return;
                int id = iv.lerInt("ID da conta a excluir (0 para cancelar)");
                if (id != 0) {
                    try { cf.excluirConta(id); System.out.println("✔ Conta excluída."); }
                    catch (RuntimeException e) { System.out.println("  ✖ " + e.getMessage()); }
                }
            }
        }
    }

    static void listarContas() {
        List<Conta> lista = cf.getContas();
        if (lista.isEmpty()) { System.out.println("Nenhuma conta cadastrada."); return; }
        System.out.println("\n[ Contas ]");
        lista.forEach(System.out::println);
    }

    // =========================================================================
    // RECEITAS
    // =========================================================================

    static void menuReceitas() {
        System.out.println("\n─── RECEITAS ───");
        System.out.println(" 1. Adicionar  2. Listar  3. Excluir  0. Voltar");
        int op = iv.lerIntNoIntervalo("Opção", 0, 3);
        switch (op) {
            case 1 -> {
                try {
                    do { adicionarReceita(); } while (iv.confirmar("Deseja cadastrar outra receita?"));
                } catch (IllegalStateException e) { System.out.println("  ✖ " + e.getMessage()); }
            }
            case 2 -> listarReceitas();
            case 3 -> excluirReceita();
        }
    }

    static void adicionarReceita() {
        System.out.println("\n[ Nova Receita ]");
        String origem  = iv.lerTexto("Descrição/Origem (ex: Salário, Freelance)");
        double valor   = iv.lerValor("Valor R$");
        Conta conta    = iv.selecionarConta(cf.getContas());
        LocalDate data = iv.lerData("Data");
        String mes     = nomeMes(data);
        cf.salvarReceita(new Receita(origem, valor, conta.getId(), data, mes, data.getYear()));
        System.out.printf("✔ Receita salva! (Conta: %s | %s/%d)%n", conta.getNome(), mes, data.getYear());
    }

    static void listarReceitas() {
        List<Receita> lista = cf.getReceitas();
        if (lista.isEmpty()) { System.out.println("Nenhuma receita cadastrada."); return; }
        System.out.println("\n[ Receitas ]");
        lista.forEach(System.out::println);
    }

    static void excluirReceita() {
        listarReceitas();
        if (cf.getReceitas().isEmpty()) return;
        int id = iv.lerInt("ID da receita a excluir (0 para cancelar)");
        if (id != 0) {
            try { cf.excluirReceita(id); System.out.println("✔ Receita excluída."); }
            catch (RuntimeException e) { System.out.println("  ✖ " + e.getMessage()); }
        }
    }

    // =========================================================================
    // DESPESAS
    // =========================================================================

    static void menuDespesas() {
        System.out.println("\n─── DESPESAS ───");
        System.out.println(" 1. Adicionar  2. Listar  3. Excluir  0. Voltar");
        int op = iv.lerIntNoIntervalo("Opção", 0, 3);
        switch (op) {
            case 1 -> {
                try {
                    do { adicionarDespesa(); } while (iv.confirmar("Deseja cadastrar outra despesa?"));
                } catch (IllegalStateException e) { System.out.println("  ✖ " + e.getMessage()); }
            }
            case 2 -> listarDespesas();
            case 3 -> excluirDespesa();
        }
    }

    static void adicionarDespesa() {
        System.out.println("\n[ Nova Despesa ]");
        Categoria cat  = iv.selecionarCategoria(cf.getCategorias());
        String detalhe = iv.lerTexto("Detalhamento (ex: Supermercado, Netflix, Água)");
        double valor   = iv.lerValor("Valor R$");
        Conta conta    = iv.selecionarConta(cf.getContas());
        LocalDate data = iv.lerData("Data");
        String mes     = nomeMes(data);
        cf.salvarDespesa(new Despesa(cat.getId(), detalhe, valor, conta.getId(), data, mes, data.getYear()));
        System.out.printf("✔ Despesa salva! (Categoria: %s | Conta: %s | %s/%d)%n",
                cat.getNome(), conta.getNome(), mes, data.getYear());
    }

    static void listarDespesas() {
        List<Despesa> lista = cf.getDespesas();
        if (lista.isEmpty()) { System.out.println("Nenhuma despesa cadastrada."); return; }
        System.out.println("\n[ Despesas ]");
        lista.forEach(System.out::println);
    }

    static void excluirDespesa() {
        listarDespesas();
        if (cf.getDespesas().isEmpty()) return;
        int id = iv.lerInt("ID da despesa a excluir (0 para cancelar)");
        if (id != 0) {
            try { cf.excluirDespesa(id); System.out.println("✔ Despesa excluída."); }
            catch (RuntimeException e) { System.out.println("  ✖ " + e.getMessage()); }
        }
    }

    // =========================================================================
    // INVESTIMENTOS
    // =========================================================================

    static void menuInvestimentos() {
        System.out.println("\n─── INVESTIMENTOS ───");
        System.out.println(" 1. Adicionar  2. Listar  3. Excluir  0. Voltar");
        int op = iv.lerIntNoIntervalo("Opção", 0, 3);
        switch (op) {
            case 1 -> {
                try {
                    do { adicionarInvestimento(); } while (iv.confirmar("Deseja cadastrar outro investimento?"));
                } catch (IllegalStateException e) { System.out.println("  ✖ " + e.getMessage()); }
            }
            case 2 -> listarInvestimentos();
            case 3 -> excluirInvestimento();
        }
    }

    static void adicionarInvestimento() {
        System.out.println("\n[ Novo Investimento ]");
        String tipo    = iv.lerTexto("Tipo (ex: Poupança, Tesouro Direto, Ações, CDB)");
        double valor   = iv.lerValor("Valor R$");
        Conta conta    = iv.selecionarConta(cf.getContas());
        LocalDate data = iv.lerData("Data");
        String mes     = nomeMes(data);
        cf.salvarInvestimento(new Investimento(tipo, valor, conta.getId(), data, mes, data.getYear()));
        System.out.printf("✔ Investimento salvo! (Conta: %s | %s/%d)%n", conta.getNome(), mes, data.getYear());
    }

    static void listarInvestimentos() {
        List<Investimento> lista = cf.getInvestimentos();
        if (lista.isEmpty()) { System.out.println("Nenhum investimento cadastrado."); return; }
        System.out.println("\n[ Investimentos ]");
        lista.forEach(System.out::println);
    }

    static void excluirInvestimento() {
        listarInvestimentos();
        if (cf.getInvestimentos().isEmpty()) return;
        int id = iv.lerInt("ID do investimento a excluir (0 para cancelar)");
        if (id != 0) {
            try { cf.excluirInvestimento(id); System.out.println("✔ Investimento excluído."); }
            catch (RuntimeException e) { System.out.println("  ✖ " + e.getMessage()); }
        }
    }

    // =========================================================================
    // LANÇAMENTOS FIXOS
    // =========================================================================

    static void menuFixos() {
        System.out.println("\n─── LANÇAMENTOS FIXOS ───");
        System.out.println(" 1. Cadastrar  2. Listar  3. Ativar/Desativar  4. Excluir  5. Aplicar em mês  0. Voltar");
        int op = iv.lerIntNoIntervalo("Opção", 0, 5);
        switch (op) {
            case 1 -> {
                try {
                    do { cadastrarFixo(); } while (iv.confirmar("Deseja cadastrar outro lançamento fixo?"));
                } catch (IllegalStateException e) { System.out.println("  ✖ " + e.getMessage()); }
            }
            case 2 -> listarFixos();
            case 3 -> {
                listarFixos();
                if (cf.getLancamentosFixos().isEmpty()) return;
                int id = iv.lerInt("ID para ativar/desativar (0 para cancelar)");
                if (id != 0) {
                    try { cf.alternarAtivoFixo(id); System.out.println("✔ Status atualizado."); }
                    catch (RuntimeException e) { System.out.println("  ✖ " + e.getMessage()); }
                }
            }
            case 4 -> {
                listarFixos();
                if (cf.getLancamentosFixos().isEmpty()) return;
                int id = iv.lerInt("ID para excluir (0 para cancelar)");
                if (id != 0) {
                    try { cf.excluirLancamentoFixo(id); System.out.println("✔ Lançamento fixo excluído."); }
                    catch (RuntimeException e) { System.out.println("  ✖ " + e.getMessage()); }
                }
            }
            case 5 -> aplicarFixosMes();
        }
    }

    static void cadastrarFixo() {
        System.out.println("\n[ Novo Lançamento Fixo ]");
        System.out.println("Tipo: 1-RECEITA  2-DESPESA  3-INVESTIMENTO");
        Tipo tipo = switch (iv.lerIntNoIntervalo("Tipo", 1, 3)) {
            case 1  -> Tipo.RECEITA;
            case 3  -> Tipo.INVESTIMENTO;
            default -> Tipo.DESPESA;
        };
        String descricao = iv.lerTexto("Descrição (ex: Salário, Condomínio, Netflix)");
        int categoriaId  = 0;
        if (tipo == Tipo.DESPESA) categoriaId = iv.selecionarCategoria(cf.getCategorias()).getId();
        double valor      = iv.lerValor("Valor R$");
        Conta conta       = iv.selecionarConta(cf.getContas());
        int diaVencimento = iv.lerIntNoIntervalo("Dia do mês para lançar", 1, 31);
        cf.salvarLancamentoFixo(new LancamentoFixo(tipo, descricao, categoriaId, valor, conta.getId(), diaVencimento));
        System.out.println("✔ Lançamento fixo cadastrado!");
    }

    static void listarFixos() {
        List<LancamentoFixo> lista = cf.getLancamentosFixos();
        if (lista.isEmpty()) { System.out.println("Nenhum lançamento fixo cadastrado."); return; }
        System.out.println("\n[ Lançamentos Fixos ]");
        lista.forEach(System.out::println);
    }

    static void aplicarFixosMes() {
        System.out.println("\n[ Aplicar Fixos em um Mês ]");
        System.out.println("Lançamentos já aplicados neste mês não serão duplicados.");
        ControleFinanceiro.MESES.forEach(m -> System.out.print(m + "  "));
        System.out.println();
        String mes;
        while (true) {
            mes = iv.lerTexto("Mês (ex: JANEIRO)").toUpperCase().trim();
            if (ControleFinanceiro.MESES.contains(mes)) break;
            System.out.println("  ✖ Mês inválido. Digite o nome completo em português (ex: MARÇO).");
        }
        int ano = iv.lerAnoRelatorio("Ano", cf.anosDisponiveis());
        int aplicados = cf.aplicarFixosMes(mes, ano);
        if (aplicados == 0)
            System.out.println("Nenhum novo lançamento aplicado (já aplicados ou nenhum ativo).");
        else
            System.out.printf("✔ %d lançamento(s) aplicado(s) em %s/%d!%n", aplicados, mes, ano);
    }

    // =========================================================================
    // RELATÓRIOS
    // =========================================================================

    static void menuRelatorios() {
        List<Integer> anos = cf.anosDisponiveis();
        if (anos.isEmpty()) { System.out.println("\nNenhum dado cadastrado ainda."); return; }

        int ano = iv.lerAnoRelatorio("\nAno para o relatório", anos);

        System.out.println("\nFiltrar por mês? (ENTER para ano todo)");
        ControleFinanceiro.MESES.forEach(m -> System.out.print(m + "  "));
        System.out.println();
        String mesInput = iv.lerTextoOpcional("Mês").toUpperCase().trim();
        String mes = mesInput.isEmpty() ? "Todos"
                : ControleFinanceiro.MESES.contains(mesInput) ? mesInput
                : "Todos";

        if (!mesInput.isEmpty() && !ControleFinanceiro.MESES.contains(mesInput))
            System.out.println("  Mês não reconhecido — exibindo ano todo.");

        System.out.println("\n══════════════════════════════════════════════════════");
        System.out.printf("  RESUMO — %s / %d%n", mes.equals("Todos") ? "ANO TODO" : mes, ano);
        System.out.println("══════════════════════════════════════════════════════");

        System.out.printf("  %% renda gasta:      %6.2f%%%n", cf.porcentagemRendaGasta(ano, mes) * 100);
        System.out.printf("  %% renda investida:  %6.2f%%%n", cf.porcentagemRendaInvestida(ano, mes) * 100);
        System.out.printf("  Saldo em conta:     R$ %,.2f%n", cf.saldoEmConta(ano, mes));

        if (mes.equals("Todos")) {
            System.out.println("\n  ─── Resumo Mês a Mês ───");
            System.out.printf("  %-12s %12s %13s %12s %12s %14s%n",
                    "Mês","Receita","Investimentos","Despesas","Saldo","Saldo Acum.");
            System.out.println("  " + "─".repeat(77));
            for (ResumoMensal rm : cf.gerarResumoAnual(ano)) {
                if (rm.getReceita()==0 && rm.getDespesaTotal()==0 && rm.getInvestimentos()==0) continue;
                System.out.printf("  %-12s %12.2f %13.2f %12.2f %12.2f %14.2f%n",
                        rm.getMes(), rm.getReceita(), rm.getInvestimentos(),
                        rm.getDespesaTotal(), rm.getSaldo(), rm.getSaldoAcumulado());
            }
        }

        System.out.println("\n  ─── Receitas por Origem ───");
        Map<String,Double> divRec = cf.divisaoReceitasPorOrigem(ano, mes);
        if (divRec.isEmpty()) System.out.println("  (sem receitas no período)");
        else divRec.forEach((k,v) -> System.out.printf("  %-30s R$ %,.2f%n", k, v));

        System.out.println("\n  ─── Gastos por Categoria ───");
        Map<String,Double> divGas = cf.divisaoGastosPorCategoria(ano, mes);
        if (divGas.isEmpty()) System.out.println("  (sem despesas no período)");
        else divGas.forEach((k,v) -> System.out.printf("  %-30s R$ %,.2f%n", k, v));

        System.out.println("\n  ─── Investimentos por Tipo ───");
        Map<String,Double> divInv = cf.divisaoInvestimentosPorTipo(ano, mes);
        if (divInv.isEmpty()) System.out.println("  (sem investimentos no período)");
        else divInv.forEach((k,v) -> System.out.printf("  %-30s R$ %,.2f%n", k, v));

        System.out.println("══════════════════════════════════════════════════════");
    }

    // =========================================================================
    // UTILITÁRIO
    // =========================================================================

    static String nomeMes(LocalDate data) {
        return data.getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("pt","BR"))
                .toUpperCase();
    }
}
