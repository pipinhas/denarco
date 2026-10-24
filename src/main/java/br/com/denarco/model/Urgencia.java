package br.com.denarco.model;

/** Urgência informada pelo cidadão; a equipe pode corrigir na triagem. */
public enum Urgencia {
    BAIXA("Baixa", 10),
    MEDIA("Média", 20),
    ALTA("Alta", 35);

    private final String descricao;
    private final int pontos;

    Urgencia(String descricao, int pontos) {
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
