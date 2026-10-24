package br.com.denarco.dto;

/** Abas da fila de atendimento. */
public enum AbaFila {
    EM_ABERTO("Em aberto"),
    MINHAS("Minhas tarefas"),
    SEM_RESPONSAVEL("Sem responsável"),
    ATRASADAS("Atrasadas"),
    CONCLUIDAS("Concluídas");

    private final String descricao;

    AbaFila(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
