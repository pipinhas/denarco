package br.com.denarco.dto;

import br.com.denarco.model.Demanda;

import java.util.List;

/**
 * Resposta da consulta em linguagem simples.
 * "entendido" lista os filtros que o sistema tirou da frase, para a pessoa conferir.
 */
public record ResultadoConsulta(String pergunta, List<String> entendido, List<String> avisos,
                                long total, List<Demanda> demandas) {
}
