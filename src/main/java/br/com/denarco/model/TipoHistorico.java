package br.com.denarco.model;

/** Tipo de cada passo da linha do tempo da demanda. O ícone aparece na tela. */
public enum TipoHistorico {
    CRIACAO("bi-flag"),
    ENCAMINHAMENTO("bi-signpost-split"),
    STATUS("bi-arrow-repeat"),
    PROVIDENCIA("bi-tools"),
    PRIORIDADE("bi-sort-up"),
    CANCELAMENTO("bi-x-circle"),
    CONCLUSAO("bi-check-circle");

    private final String icone;

    TipoHistorico(String icone) {
        this.icone = icone;
    }

    public String getIcone() {
        return icone;
    }
}
