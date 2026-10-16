package model;

import java.time.LocalDate;

/** Uma missão num dia em que ela deve ser cumprida (o que aparece na Agenda). */
public record OcorrenciaMissao(Missao missao, LocalDate data, boolean concluida) {

    /** Passou do dia e não foi cumprida. */
    public boolean atrasada(LocalDate hoje) {
        return !concluida && data.isBefore(hoje);
    }
}
