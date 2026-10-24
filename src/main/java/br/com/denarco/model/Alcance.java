package br.com.denarco.model;

/** Quantas pessoas o problema atinge. */
public enum Alcance {
    LOCAL("Um ponto ou imóvel", 5),
    RUA("Uma rua", 10),
    BAIRRO("Bairro ou mais", 20);

    private final String descricao;
    private final int pontos;

    Alcance(String descricao, int pontos) {
        this.descricao = descricao;
        this.pontos = pontos;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPontos() {
        return pontos;
    }
}
