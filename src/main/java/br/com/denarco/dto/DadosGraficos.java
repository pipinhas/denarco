package br.com.denarco.dto;

import java.util.List;

/**
 * Dados dos gráficos do painel, enviados em JSON para o JavaScript (painel.js).
 * As listas "semanas", "registradas" e "concluidas" têm o mesmo tamanho: uma posição por semana.
 */
public record DadosGraficos(List<Contagem> porCategoria,
                            List<String> semanas,
                            List<Long> registradas,
                            List<Long> concluidas,
                            List<Contagem> porBairro) {
}
