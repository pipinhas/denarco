package br.com.denarco.dto;

/** Uma demanda no mapa. Só dados públicos: nada de autor, contato ou foto. */
public record PontoMapa(Long id, String protocolo, String categoria, String status, int pontuacao, String faixa,
                        double latitude, double longitude, String local, int apoios, boolean atrasada) {
}
