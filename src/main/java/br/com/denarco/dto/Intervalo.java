package br.com.denarco.dto;

import java.time.LocalDateTime;

/** Quando uma demanda foi criada e quando foi concluída (para o tempo médio de atendimento). */
public record Intervalo(LocalDateTime inicio, LocalDateTime fim) {
}
