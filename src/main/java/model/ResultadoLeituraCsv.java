package model;

import java.util.List;

/** Linhas lidas de um CSV e as mensagens das linhas que não puderam ser lidas. */
public record ResultadoLeituraCsv(List<LinhaImportacao> linhas, List<String> erros) {}
