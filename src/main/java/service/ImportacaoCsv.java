package service;

import db.DatabaseManager;
import model.*;
import repository.CategoriaRepository;
import repository.DespesaRepository;
import repository.MapeamentoRepository;

import java.io.IOException;
import java.math.BigDecimal;
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
import java.util.*;

/** Importação de extratos CSV como despesas, com aprendizado de categorias por descrição. */
public class ImportacaoCsv {

    private final CategoriaRepository categoriaRepo;
    private final DespesaRepository despesaRepo;
    private final MapeamentoRepository mapeamentoRepo;
    private final repository.ContaRepository contaRepo;

    /** Lê e grava somente dados do usuário informado. */
    public ImportacaoCsv(int usuarioId) {
        this.categoriaRepo = new CategoriaRepository(usuarioId);
        this.despesaRepo = new DespesaRepository(usuarioId);
        this.mapeamentoRepo = new MapeamentoRepository(usuarioId);
        this.contaRepo = new repository.ContaRepository(usuarioId);
    }

    /**
     * Lê um extrato CSV (ex: fatura do Nubank: date,title,amount) e monta o preview da importação.
     * Linhas com erro são puladas e descritas em {@code erros}; valores negativos (pagamentos,
     * estornos) e despesas que já existem na conta vêm desmarcadas.
     */
    public ResultadoLeituraCsv ler(String caminho, int contaId) throws IOException {
        List<String> texto = lerLinhasArquivo(Path.of(caminho));
        List<LinhaImportacao> linhas = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        if (texto.isEmpty()) return new ResultadoLeituraCsv(linhas, erros);

        String cabecalho = texto.get(0);
        char sep = cabecalho.indexOf(';') >= 0 && cabecalho.indexOf(',') < 0 ? ';' : ',';
        int[] col = localizarColunas(parseLinhaCsv(cabecalho, sep));
        int ultimaColuna = Math.max(col[0], Math.max(col[1], col[2]));

        List<Categoria> categorias = categoriaRepo.listarTodos();
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
                BigDecimal valor = Conversor.parseValor(partes[col[2]]);

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
                if (valor.signum() <= 0) {
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
    public int confirmar(List<LinhaImportacao> linhas, int contaId) {
        List<LinhaImportacao> selecionadas = linhas.stream().filter(LinhaImportacao::isImportar).toList();
        if (!contaRepo.pertence(contaId)) throw new IllegalArgumentException("Conta inválida. Selecione uma das suas contas.");
        Set<Integer> minhasCategorias = new HashSet<>();
        categoriaRepo.listarTodos().forEach(c -> minhasCategorias.add(c.getId()));
        for (LinhaImportacao li : selecionadas) {
            if (li.getCategoria() == null)
                throw new IllegalArgumentException("\"" + li.getTitulo() + "\" está sem categoria.");
            if (!minhasCategorias.contains(li.getCategoria().getId()))
                throw new IllegalArgumentException("\"" + li.getTitulo() + "\" está com uma categoria inválida.");
            if (li.getValor().signum() <= 0)
                throw new IllegalArgumentException("\"" + li.getTitulo() + "\" tem valor negativo ou zero e não pode virar despesa.");
            if (li.getDetalhe() == null || li.getDetalhe().isBlank())
                throw new IllegalArgumentException("\"" + li.getTitulo() + "\" está sem detalhe.");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (LinhaImportacao li : selecionadas) {
                    int catId = li.getCategoria().getId();
                    despesaRepo.salvar(conn, new Despesa(catId, li.getDetalhe(), li.getValor(), contaId, li.getData()));

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
        List<LinhaImportacao> candidatas = linhas.stream().filter(l -> l.getValor().signum() > 0).toList();
        if (candidatas.isEmpty()) return;
        LocalDate ini = candidatas.stream().map(LinhaImportacao::getData).min(LocalDate::compareTo).orElseThrow();
        LocalDate fim = candidatas.stream().map(LinhaImportacao::getData).max(LocalDate::compareTo).orElseThrow();

        Map<String, Integer> existentes = new HashMap<>();
        for (Despesa d : despesaRepo.listarPorContaEPeriodo(contaId, new Periodo(ini, fim)))
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

    private static String chaveDuplicidade(LocalDate data, BigDecimal valor) {
        return data + "|" + valor.toPlainString();
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
}
