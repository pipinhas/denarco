package br.com.denarco.model;

/**
 * Perfis de acesso, do menor para o maior.
 * A ordem das constantes importa: ela é usada para comparar níveis (quem pode promover quem).
 */
public enum Perfil {
    CIDADAO("Cidadão"),
    SERVIDOR("Servidor"),
    GESTOR("Gestor"),
    ADMIN("Administrador");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean acimaDe(Perfil outro) {
        return this.ordinal() > outro.ordinal();
    }

    public boolean peloMenos(Perfil outro) {
        return this.ordinal() >= outro.ordinal();
    }
}
