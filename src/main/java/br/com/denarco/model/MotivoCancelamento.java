package br.com.denarco.model;

public enum MotivoCancelamento {
    DUPLICADA("Duplicada de outra demanda"),
    IMPROCEDENTE("Problema não encontrado no local"),
    FORA_DA_COMPETENCIA("Não é responsabilidade da Prefeitura");

    private final String descricao;

    MotivoCancelamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
