package br.com.denarco.dto;

/** Os quatro números grandes do topo do painel. */
public record ResumoPainel(long emAberto, long atrasadas, long concluidasNoPeriodo, String tempoMedio) {
}
