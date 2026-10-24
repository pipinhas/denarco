package br.com.denarco.model;

/**
 * Área de atuação escolhida no cadastro corporativo.
 * As opções vêm do público-alvo descrito no desafio.
 */
public enum Atuacao {
    ATENDIMENTO("Atendimento e triagem", Perfil.SERVIDOR),
    EQUIPE_CAMPO("Equipe de campo", Perfil.SERVIDOR),
    GESTAO("Gestão de secretaria", Perfil.GESTOR),
    PLANEJAMENTO("Planejamento e controle", Perfil.GESTOR);

    private final String descricao;

    // Perfil sugerido a quem for aprovar a conta; a decisão final é de quem aprova.
    private final Perfil perfilSugerido;

    Atuacao(String descricao, Perfil perfilSugerido) {
        this.descricao = descricao;
        this.perfilSugerido = perfilSugerido;
    }

    public String getDescricao() {
        return descricao;
    }

    public Perfil getPerfilSugerido() {
        return perfilSugerido;
    }
}
