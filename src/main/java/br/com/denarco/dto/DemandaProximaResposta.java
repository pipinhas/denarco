package br.com.denarco.dto;

/** Resposta em JSON do aviso de "possível duplicada" na tela de nova demanda. */
public record DemandaProximaResposta(Long id, String protocolo, String categoria, String descricao,
                                     String local, int distanciaMetros, int totalApoios,
                                     boolean jaApoiei, boolean minha) {
}
