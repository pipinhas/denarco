package br.com.denarco.model;

/** Por onde a demanda chegou. Guardar isso é o que permite juntar todos os canais numa base só. */
public enum Canal {
    APP("Site"),
    TELEFONE("Telefone"),
    PRESENCIAL("Presencial"),
    WHATSAPP("WhatsApp");

    private final String descricao;

    Canal(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
