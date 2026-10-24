package br.com.denarco.dto;

/** Um nome e quantas vezes ele aparece (ex.: "Centro", 12). Preenchido direto pela consulta JPQL. */
public record Contagem(String nome, Long total) {
}
