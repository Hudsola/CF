package service;

import db.DatabaseManager;
import model.*;
import repository.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ControleFinanceiro {

    public static final List<String> MESES = List.of(
            "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
            "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO");

    private final CategoriaRepository categoriaRepo = new CategoriaRepository();
    private final ContaRepository contaRepo = new ContaRepository();
    private final ReceitaRepository receitaRepo = new ReceitaRepository();
    private final DespesaRepository despesaRepo = new DespesaRepository();
    private final InvestimentoRepository investRepo = new InvestimentoRepository();
    private final LancamentoFixoRepository fixoRepo = new LancamentoFixoRepository();
    private final UsuarioRepository usuarioRepo = new UsuarioRepository();
    private final MapeamentoRepository mapeamentoRepo = new MapeamentoRepository();

    // --- Usuário ---
    public Usuario getUsuario() {
        return usuarioRepo.buscarPrincipal();
    }

    public void atualizarUsuario(Usuario u) {
        usuarioRepo.atualizar(u);
    }

    // --- Categorias ---
    public void salvarCategoria(Categoria c) {
        categoriaRepo.salvar(c);
    }

    public void atualizarCategoria(Categoria c) {
        categoriaRepo.atualizar(c);
    }

    public void excluirCategoria(int id) {
        categoriaRepo.excluir(id);
    }

    public List<Categoria> getCategorias() {
        return categoriaRepo.listarTodos();
    }

    // --- Contas ---
    public void salvarConta(Conta c) {
        contaRepo.salvar(c);
    }

    public void atualizarConta(Conta c) {
        contaRepo.atualizar(c);
    }

    public void excluirConta(int id) {
        contaRepo.excluir(id);
    }

    public List<Conta> getContas() {
        return contaRepo.listarTodos();
    }

    // --- Receitas ---
    public void salvarReceita(Receita r) {
        receitaRepo.salvar(r);
    }

    public void atualizarReceita(Receita r) {
        receitaRepo.atualizar(r);
    }

    public void excluirReceita(int id) {
        receitaRepo.excluir(id);
    }

    public List<Receita> getReceitas() {
        return receitaRepo.listarTodos();
    }

    // --- Despesas ---
    public void salvarDespesa(Despesa d) {
        despesaRepo.salvar(d);
    }

    public void atualizarDespesa(Despesa d) {
        despesaRepo.atualizar(d);
    }

    public void excluirDespesa(int id) {
        despesaRepo.excluir(id);
    }

    public List<Despesa> getDespesas() {
        return despesaRepo.listarTodos();
    }

    // --- Investimentos ---
    public void salvarInvestimento(Investimento i) {
        investRepo.salvar(i);
    }

    public void atualizarInvestimento(Investimento i) {
        investRepo.atualizar(i);
    }

    public void excluirInvestimento(int id) {
        investRepo.excluir(id);
    }

    public List<Investimento> getInvestimentos() {
        return investRepo.listarTodos();
    }

    // --- Lançamentos Fixos ---
    public void salvarLancamentoFixo(LancamentoFixo lf) {
        validarFixo(lf);
        fixoRepo.salvar(lf);
    }

    public void atualizarLancamentoFixo(LancamentoFixo lf) {
        validarFixo(lf);
        fixoRepo.atualizar(lf);
    }

    private void validarFixo(LancamentoFixo lf) {
        if (lf.getTipo() == LancamentoFixo.Tipo.DESPESA && lf.getCategoriaId() <= 0)
            throw new IllegalArgumentException("Lançamento fixo de despesa precisa de uma categoria.");
        if (lf.getValor() <= 0)
            throw new IllegalArgumentException("Valor deve ser maior que zero.");
    }

    /** Converte "março", "Março" ou "MARÇO" no nome padronizado; erro se não for um mês. */
    private static String normalizarMes(String mes) {
        String m = mes == null ? "" : mes.trim().toUpperCase(Locale.ROOT);
        if (!MESES.contains(m)) throw new IllegalArgumentException("Mês inválido: \"" + mes + "\".");
        return m;
    }

    public void excluirLancamentoFixo(int id) {
        fixoRepo.excluir(id);
    }

    public void alternarAtivoFixo(int id) {
        fixoRepo.alternarAtivo(id);
    }

    public List<LancamentoFixo> getLancamentosFixos() {
        return fixoRepo.listarTodos();
    }

    public List<LancamentoFixo> getLancamentosFixosAtivos() {
        return fixoRepo.listarAtivos();
    }

    public int aplicarFixosMes(String mesInformado, int ano) {
        String mes = normalizarMes(mesInformado);
        YearMonth ym = YearMonth.of(ano, MESES.indexOf(mes) + 1);
        int aplicados = 0;
        for (LancamentoFixo lf : fixoRepo.listarAtivos()) {
            if (fixoRepo.jaAplicado(lf.getId(), mes, ano)) continue;
            int dia = Math.min(lf.getDiaVencimento(), ym.lengthOfMonth());
            LocalDate data = LocalDate.of(ano, ym.getMonthValue(), dia);
            switch (lf.getTipo()) {
                case RECEITA ->
                        salvarReceita(new Receita(lf.getDescricao(), lf.getValor(), lf.getContaId(), data, mes, ano));
                case DESPESA ->
                        salvarDespesa(new Despesa(lf.getCategoriaId(), lf.getDescricao(), lf.getValor(), lf.getContaId(), data, mes, ano));
                case INVESTIMENTO ->
                        salvarInvestimento(new Investimento(lf.getDescricao(), lf.getValor(), lf.getContaId(), data, mes, ano));
            }
            fixoRepo.registrarAplicacao(lf.getId(), mes, ano);
            aplicados++;
        }
        return aplicados;
    }

    public int aplicarFixosIntervalo(String mesInicio, int anoInicio, String mesFim, int anoFim) {
        int total = 0;
        YearMonth atual = YearMonth.of(anoInicio, MESES.indexOf(normalizarMes(mesInicio)) + 1);
        YearMonth fim = YearMonth.of(anoFim, MESES.indexOf(normalizarMes(mesFim)) + 1);
        if (atual.isAfter(fim))
            throw new IllegalArgumentException("O mês inicial deve ser anterior ou igual ao mês final.");

        while (!atual.isAfter(fim)) {
            String mes = MESES.get(atual.getMonthValue() - 1);
            total += aplicarFixosMes(mes, atual.getYear());
            atual = atual.plusMonths(1);
        }
        return total;
    }

    // --- Importação CSV ---

    /**
     * Lê um extrato CSV (ex: fatura do Nubank: date,title,amount) e monta o preview da importação.
     * Linhas com erro são puladas e descritas em {@code erros}; valores negativos (pagamentos,
     * estornos) e despesas que já existem na conta vêm desmarcadas.
     */
    public ResultadoLeituraCsv lerCsv(String caminho, int contaId) throws IOException {
        List<String> texto = lerLinhasArquivo(Path.of(caminho));
        List<LinhaImportacao> linhas = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        if (texto.isEmpty()) return new ResultadoLeituraCsv(linhas, erros);

        String cabecalho = texto.get(0);
        char sep = cabecalho.indexOf(';') >= 0 && cabecalho.indexOf(',') < 0 ? ';' : ',';
        int[] col = localizarColunas(parseLinhaCsv(cabecalho, sep));
        int ultimaColuna = Math.max(col[0], Math.max(col[1], col[2]));

        List<Categoria> categorias = getCategorias();
        List<MapeamentoDescricao> mapeamentos = mapeamentoRepo.listarTodos();

        for (int n = 1; n < texto.size(); n++) {
            String linha = texto.get(n);
            if (linha.isBlank()) continue;
            try {
                String[] partes = parseLinhaCsv(linha, sep);
                if (partes.length <= ultimaColuna)
                    throw new IllegalArgumentException("colunas insuficientes.");
                LocalDate data = Conversor.parseData(partes[col[0]]);
                String titulo  = partes[col[1]].trim();
                double valor   = Conversor.parseValor(partes[col[2]]);

                LinhaImportacao li = new LinhaImportacao(titulo, valor, data);
                MapeamentoDescricao map = buscarMapeamento(mapeamentos, titulo);
                if (map != null) {
                    li.setMapeamentoOriginal(map);
                    categorias.stream().filter(c -> c.getId() == map.getCategoriaId())
                            .findFirst().ifPresent(li::setCategoria);
                    li.setDetalhe(map.getDetalhe());
                } else {
                    li.setMapeamentoNovo(true);
                }
                if (valor <= 0) {
                    li.setImportar(false);
                    li.setObservacao("Crédito/estorno");
                }
                linhas.add(li);
            } catch (IllegalArgumentException e) {
                erros.add("Linha " + (n + 1) + ": " + e.getMessage());
            }
        }
        marcarDuplicadas(linhas, contaId);
        return new ResultadoLeituraCsv(linhas, erros);
    }

    /**
     * Grava as linhas marcadas como despesas da conta, numa única transação: ou tudo é importado
     * ou nada é. Também aprende mapeamentos novos e atualiza os existentes cuja categoria mudou.
     */
    public int confirmarImportacao(List<LinhaImportacao> linhas, int contaId) {
        List<LinhaImportacao> selecionadas = linhas.stream().filter(LinhaImportacao::isImportar).toList();
        for (LinhaImportacao li : selecionadas) {
            if (li.getCategoria() == null)
                throw new IllegalArgumentException("\"" + li.getTitulo() + "\" está sem categoria.");
            if (li.getValor() <= 0)
                throw new IllegalArgumentException("\"" + li.getTitulo() + "\" tem valor negativo ou zero e não pode virar despesa.");
            if (li.getDetalhe() == null || li.getDetalhe().isBlank())
                throw new IllegalArgumentException("\"" + li.getTitulo() + "\" está sem detalhe.");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (LinhaImportacao li : selecionadas) {
                    int catId = li.getCategoria().getId();
                    despesaRepo.salvar(conn, new Despesa(catId, li.getDetalhe(), li.getValor(), contaId,
                            li.getData(), Conversor.nomeMes(li.getData()), li.getData().getYear()));

                    MapeamentoDescricao original = li.getMapeamentoOriginal();
                    if (original == null) {
                        mapeamentoRepo.salvar(conn, new MapeamentoDescricao(li.getTitulo(), catId, li.getDetalhe()));
                    } else if (original.getCategoriaId() != catId) {
                        mapeamentoRepo.salvar(conn, new MapeamentoDescricao(original.getPadrao(), catId, li.getDetalhe()));
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao importar despesas (nada foi gravado): " + e.getMessage(), e);
        }
        return selecionadas.size();
    }

    /** Lê o arquivo como UTF-8; se não for UTF-8 válido, usa Windows-1252 (padrão do Excel no Windows). */
    static List<String> lerLinhasArquivo(Path arquivo) throws IOException {
        byte[] bytes = Files.readAllBytes(arquivo);
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            texto = new String(bytes, Charset.forName("windows-1252"));
        }
        if (texto.startsWith("﻿")) texto = texto.substring(1);
        return texto.lines().toList();
    }

    /** Índices das colunas [data, descrição, valor] pelo cabeçalho; sem cabeçalho reconhecido usa 0, 1, 2. */
    private static int[] localizarColunas(String[] cabecalho) {
        int data = -1, titulo = -1, valor = -1;
        for (int i = 0; i < cabecalho.length; i++) {
            String h = cabecalho[i].trim().toLowerCase(Locale.ROOT);
            if (data < 0 && (h.equals("date") || h.equals("data"))) data = i;
            else if (titulo < 0 && (h.equals("title") || h.startsWith("descri") || h.startsWith("hist"))) titulo = i;
            else if (valor < 0 && (h.equals("amount") || h.equals("valor"))) valor = i;
        }
        if (data < 0 || titulo < 0 || valor < 0) return new int[]{0, 1, 2};
        return new int[]{data, titulo, valor};
    }

    /** Mapeamento cujo padrão aparece no título; havendo vários, o mais específico (mais longo). */
    private static MapeamentoDescricao buscarMapeamento(List<MapeamentoDescricao> mapeamentos, String titulo) {
        String t = titulo.toLowerCase(Locale.ROOT);
        return mapeamentos.stream()
                .filter(m -> t.contains(m.getPadrao().toLowerCase(Locale.ROOT)))
                .max(Comparator.comparingInt(m -> m.getPadrao().length()))
                .orElse(null);
    }

    /**
     * Desmarca linhas que já existem como despesa na conta (mesma data e valor). A contagem é por
     * ocorrência: se o banco tem 1 despesa igual e o CSV tem 2, só a primeira é desmarcada.
     */
    private void marcarDuplicadas(List<LinhaImportacao> linhas, int contaId) {
        List<LinhaImportacao> candidatas = linhas.stream().filter(l -> l.getValor() > 0).toList();
        if (candidatas.isEmpty()) return;
        LocalDate ini = candidatas.stream().map(LinhaImportacao::getData).min(LocalDate::compareTo).orElseThrow();
        LocalDate fim = candidatas.stream().map(LinhaImportacao::getData).max(LocalDate::compareTo).orElseThrow();

        Map<String, Integer> existentes = new HashMap<>();
        for (Despesa d : despesaRepo.listarPorContaEPeriodo(contaId, ini, fim))
            existentes.merge(chaveDuplicidade(d.getData(), d.getValor()), 1, Integer::sum);

        for (LinhaImportacao li : candidatas) {
            String chave = chaveDuplicidade(li.getData(), li.getValor());
            int qtd = existentes.getOrDefault(chave, 0);
            if (qtd > 0) {
                existentes.put(chave, qtd - 1);
                li.setImportar(false);
                li.setObservacao("Já importada?");
            }
        }
    }

    private static String chaveDuplicidade(LocalDate data, double valor) {
        return data + "|" + Math.round(valor * 100);
    }

    /** Divide uma linha CSV respeitando aspas ("a, b") e aspas escapadas (""). */
    private static String[] parseLinhaCsv(String linha, char sep) {
        List<String> campos = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean dentroAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (dentroAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else dentroAspas = !dentroAspas;
            }
            else if (c == sep && !dentroAspas) { campos.add(sb.toString()); sb.setLength(0); }
            else { sb.append(c); }
        }
        campos.add(sb.toString());
        return campos.toArray(new String[0]);
    }

    // --- Cálculos ---

    public double somarReceitas(String mes, int ano) {
        return receitaRepo.listarPorAno(ano).stream()
                .filter(r -> r.getMes().equalsIgnoreCase(mes)).mapToDouble(Receita::getValor).sum();
    }

    public double somarReceitasAno(int ano) {
        return receitaRepo.listarPorAno(ano).stream().mapToDouble(Receita::getValor).sum();
    }

    public double somarInvestimentos(String mes, int ano) {
        return investRepo.listarPorAno(ano).stream()
                .filter(i -> i.getMes().equalsIgnoreCase(mes)).mapToDouble(Investimento::getValor).sum();
    }

    public double somarInvestimentosAno(int ano) {
        return investRepo.listarPorAno(ano).stream().mapToDouble(Investimento::getValor).sum();
    }

    public double somarDespesas(String nomeCategoria, String mes, int ano) {
        return despesaRepo.listarPorAno(ano).stream()
                .filter(d -> d.getCategoriaNome().equalsIgnoreCase(nomeCategoria) && d.getMes().equalsIgnoreCase(mes))
                .mapToDouble(Despesa::getValor).sum();
    }

    public double somarDespesasCategoriaAno(String nomeCategoria, int ano) {
        return despesaRepo.listarPorAno(ano).stream()
                .filter(d -> d.getCategoriaNome().equalsIgnoreCase(nomeCategoria))
                .mapToDouble(Despesa::getValor).sum();
    }

    public double somarTotalDespesas(String mes, int ano) {
        return despesaRepo.listarPorAno(ano).stream()
                .filter(d -> d.getMes().equalsIgnoreCase(mes)).mapToDouble(Despesa::getValor).sum();
    }

    public double somarTotalDespesasAno(int ano) {
        return despesaRepo.listarPorAno(ano).stream().mapToDouble(Despesa::getValor).sum();
    }

    public double saldoTotal() {
        double rec = getReceitas().stream().mapToDouble(Receita::getValor).sum();
        double desp = getDespesas().stream().mapToDouble(Despesa::getValor).sum();
        double inv = getInvestimentos().stream().mapToDouble(Investimento::getValor).sum();
        return rec - desp - inv;
    }

    private boolean todos(String mes) {
        return "Todos".equalsIgnoreCase(mes);
    }

    public double porcentagemRendaGasta(int ano, String mes) {
        double rec = todos(mes) ? somarReceitasAno(ano) : somarReceitas(mes, ano);
        double desp = todos(mes) ? somarTotalDespesasAno(ano) : somarTotalDespesas(mes, ano);
        return rec == 0 ? 0 : desp / rec;
    }

    public double porcentagemRendaInvestida(int ano, String mes) {
        double rec = todos(mes) ? somarReceitasAno(ano) : somarReceitas(mes, ano);
        double inv = todos(mes) ? somarInvestimentosAno(ano) : somarInvestimentos(mes, ano);
        return rec == 0 ? 0 : inv / rec;
    }

    public double saldoEmConta(int ano, String mes) {
        double rec = todos(mes) ? somarReceitasAno(ano) : somarReceitas(mes, ano);
        double desp = todos(mes) ? somarTotalDespesasAno(ano) : somarTotalDespesas(mes, ano);
        double inv = todos(mes) ? somarInvestimentosAno(ano) : somarInvestimentos(mes, ano);
        return rec - desp - inv;
    }

    // --- Divisões ---

    public Map<String, Double> divisaoReceitasPorOrigem(int ano, String mes) {
        Map<String, Double> r = new LinkedHashMap<>();
        receitaRepo.listarPorAno(ano).stream()
                .filter(x -> todos(mes) || x.getMes().equalsIgnoreCase(mes))
                .collect(Collectors.groupingBy(Receita::getOrigem, Collectors.summingDouble(Receita::getValor)))
                .entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> r.put(e.getKey(), e.getValue()));
        return r;
    }

    public Map<String, Double> divisaoGastosPorCategoria(int ano, String mes) {
        Map<String, Double> r = new LinkedHashMap<>();
        getCategorias().forEach(cat -> {
            double v = todos(mes) ? somarDespesasCategoriaAno(cat.getNome(), ano)
                    : somarDespesas(cat.getNome(), mes, ano);
            if (v > 0) r.put(cat.getNome(), v);
        });
        return r;
    }

    public Map<String, Double> divisaoInvestimentosPorTipo(int ano, String mes) {
        Map<String, Double> r = new LinkedHashMap<>();
        investRepo.listarPorAno(ano).stream()
                .filter(x -> todos(mes) || x.getMes().equalsIgnoreCase(mes))
                .collect(Collectors.groupingBy(Investimento::getTipo, Collectors.summingDouble(Investimento::getValor)))
                .entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> r.put(e.getKey(), e.getValue()));
        return r;
    }

    // --- Resumo anual ---

    public List<ResumoMensal> gerarResumoAnual(int ano) {
        List<ResumoMensal> resumos = new ArrayList<>();
        double saldoAcum = 0;
        for (String mes : MESES) {
            ResumoMensal rm = new ResumoMensal(mes);
            double rec = somarReceitas(mes, ano);
            double inv = somarInvestimentos(mes, ano);
            double desp = somarTotalDespesas(mes, ano);
            rm.setReceita(rec);
            rm.setInvestimentos(inv);
            rm.setDespesaTotal(desp);
            rm.setAlimentacao(somarDespesas("Alimentação", mes, ano));
            rm.setMoradia(somarDespesas("Moradia", mes, ano));
            rm.setEducacao(somarDespesas("Educação", mes, ano));
            rm.setPet(somarDespesas("Pet", mes, ano));
            rm.setSaude(somarDespesas("Saúde", mes, ano));
            rm.setTransporte(somarDespesas("Transporte", mes, ano));
            rm.setPessoais(somarDespesas("Pessoais", mes, ano));
            rm.setLazer(somarDespesas("Lazer", mes, ano));
            rm.setFinanceiros(somarDespesas("Financeiros", mes, ano));
            double saldo = rec - desp - inv;   // mesma regra de saldoEmConta()
            rm.setSaldo(saldo);
            saldoAcum += saldo;
            rm.setSaldoAcumulado(saldoAcum);
            resumos.add(rm);
        }
        return resumos;
    }

    public List<Integer> anosDisponiveis() {
        Set<Integer> anos = new TreeSet<>();
        getReceitas().forEach(r -> anos.add(r.getAno()));
        getDespesas().forEach(d -> anos.add(d.getAno()));
        getInvestimentos().forEach(i -> anos.add(i.getAno()));
        return new ArrayList<>(anos);
    }

    public List<Despesa> pesquisarDespesas(Integer categoriaId, String mes, int ano) {
        return despesaRepo.pesquisar(categoriaId, mes, ano);
    }

    public List<Receita> pesquisarReceitas(String origem, String mes, int ano) {
        return receitaRepo.pesquisar(origem, mes, ano);
    }

    public List<Investimento> pesquisarInvestimentos(String tipo, String mes, int ano) {
        return investRepo.pesquisar(tipo, mes, ano);
    }
}
