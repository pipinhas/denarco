package br.com.denarco.dto;

/** Números grandes do topo da fila. */
public record ResumoFila(long emAberto, long semResponsavel, long atrasadas, long concluidas30Dias) {
}
