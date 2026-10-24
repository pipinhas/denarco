package br.com.denarco.dto;

import br.com.denarco.model.Demanda;

/** Demanda encontrada perto de um ponto, com a distância até ele. */
public record DemandaProxima(Demanda demanda, int distanciaMetros) {
}
