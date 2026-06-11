package util;

import model.Categoria;
import model.Conta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class InputValidator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Scanner sc;

    public InputValidator(Scanner sc) { this.sc = sc; }

    public String lerTexto(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String v = sc.nextLine().trim();
            if (!v.isEmpty()) return v;
            erro("O campo não pode estar vazio.");
        }
    }

    public String lerTextoOpcional(String prompt) {
        System.out.print(prompt + " (ENTER para pular): ");
        return sc.nextLine().trim();
    }

    public int lerInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { erro("Digite um número inteiro válido."); }
        }
    }

    public int lerIntNoIntervalo(String prompt, int min, int max) {
        while (true) {
            int v = lerInt(prompt + " (" + min + "-" + max + ")");
            if (v >= min && v <= max) return v;
            erro("O valor deve estar entre " + min + " e " + max + ".");
        }
    }

    public double lerValor(String prompt) {
        while (true) {
            System.out.print(prompt + " (ex: 1500,00): ");
            try {
                double v = Double.parseDouble(sc.nextLine().trim().replace(",", "."));
                if (v > 0) return v;
                erro("O valor deve ser maior que zero.");
            } catch (NumberFormatException e) {
                erro("Valor inválido. Use apenas números e vírgula/ponto decimal (ex: 1500,00).");
            }
        }
    }

    public LocalDate lerData(String prompt) {
        while (true) {
            System.out.print(prompt + " (dd/MM/yyyy): ");
            String linha = sc.nextLine().trim();

            if (!linha.matches("\\d{2}/\\d{2}/\\d{4}")) {
                erro("Formato inválido. Use dd/MM/yyyy (ex: 25/12/2025)."); continue;
            }

            String[] p = linha.split("/");
            int dia = Integer.parseInt(p[0]);
            int mes = Integer.parseInt(p[1]);
            int ano = Integer.parseInt(p[2]);

            if (mes < 1 || mes > 12) { erro("Mês inválido: " + mes + ". Use 01-12."); continue; }
            if (ano < 2000 || ano > 2100) { erro("Ano inválido: " + ano + ". Use 2000-2100."); continue; }

            try { return LocalDate.parse(linha, FMT); }
            catch (DateTimeParseException e) {
                int max = LocalDate.of(ano, mes, 1).lengthOfMonth();
                erro("Dia inválido: " + dia + ". Para " + mes + "/" + ano + " use 01-" + max + ".");
            }
        }
    }

    public int lerAnoRelatorio(String prompt, List<Integer> anosDisponiveis) {
        if (!anosDisponiveis.isEmpty()) System.out.println("Anos com dados: " + anosDisponiveis);
        while (true) {
            int ano = lerInt(prompt);
            if (ano >= 2000 && ano <= 2100) return ano;
            erro("Ano inválido. Use entre 2000 e 2100.");
        }
    }

    public Conta selecionarConta(List<Conta> contas) {
        if (contas.isEmpty())
            throw new IllegalStateException("Nenhuma conta cadastrada. Cadastre uma conta antes de continuar.");
        System.out.println("Contas disponíveis:");
        contas.forEach(c -> System.out.println("  " + c));
        while (true) {
            int id = lerInt("ID da conta");
            for (Conta c : contas) if (c.getId() == id) return c;
            erro("Conta não encontrada. Escolha um ID da lista acima.");
        }
    }

    public Categoria selecionarCategoria(List<Categoria> categorias) {
        if (categorias.isEmpty())
            throw new IllegalStateException("Nenhuma categoria cadastrada. Cadastre uma categoria antes de continuar.");
        System.out.println("Categorias disponíveis:");
        categorias.forEach(c -> System.out.println("  " + c));
        while (true) {
            int id = lerInt("ID da categoria");
            for (Categoria c : categorias) if (c.getId() == id) return c;
            erro("Categoria não encontrada. Escolha um ID da lista acima.");
        }
    }

    public boolean confirmar(String pergunta) {
        while (true) {
            System.out.print(pergunta + " (S/N): ");
            String r = sc.nextLine().trim().toUpperCase();
            if (r.equals("S")) return true;
            if (r.equals("N")) return false;
            erro("Digite S para Sim ou N para Não.");
        }
    }

    private void erro(String msg) { System.out.println("  ✖ Erro: " + msg); }
}
