package br.com.denarco.model;

import java.util.List;

/**
 * Situação da demanda. As mudanças permitidas seguem o diagrama da seção 4.2 do plano.
 * A classe CSS de cada status é usada nas etiquetas coloridas das telas.
 */
public enum StatusDemanda {
    ABERTA("Aberta", "status-aberta"),
    ENCAMINHADA("Encaminhada", "status-encaminhada"),
    EM_ANDAMENTO("Em andamento", "status-andamento"),
    CONCLUIDA("Concluída", "status-concluida"),
    CANCELADA("Cancelada", "status-cancelada");

    private final String descricao;
    private final String classeCss;

    StatusDemanda(String descricao, String classeCss) {
        this.descricao = descricao;
        this.classeCss = classeCss;
    }

    public static List<StatusDemanda> emAberto() {
        return List.of(ABERTA, ENCAMINHADA, EM_ANDAMENTO);
    }

    public boolean isEmAberto() {
        return emAberto().contains(this);
    }

    public boolean podeMudarPara(StatusDemanda novo) {
        return switch (this) {
            case ABERTA -> novo == ENCAMINHADA || novo == CANCELADA;
            // Volta para ABERTA quando é transferida para outro setor.
            case ENCAMINHADA -> novo == EM_ANDAMENTO || novo == CONCLUIDA || novo == CANCELADA || novo == ABERTA;
            case EM_ANDAMENTO -> novo == CONCLUIDA;
            case CONCLUIDA -> novo == ABERTA;
            case CANCELADA -> false;
        };
    }

    public String getDescricao() {
        return descricao;
    }

    public String getClasseCss() {
        return classeCss;
    }
}
