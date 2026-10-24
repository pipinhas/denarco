package br.com.denarco.dto;

import br.com.denarco.model.Atuacao;
import jakarta.validation.constraints.NotNull;

/** Cadastro de servidor da Prefeitura: os dados do cadastro comum + área de atuação e setor. */
public class CadastroCorporativoForm extends CadastroForm {

    @NotNull(message = "Escolha sua área de atuação.")
    private Atuacao atuacao;

    // Opcional só para "Planejamento e controle", que acompanha todos os setores.
    private Long setorId;

    public Atuacao getAtuacao() {
        return atuacao;
    }

    public void setAtuacao(Atuacao atuacao) {
        this.atuacao = atuacao;
    }

    public Long getSetorId() {
        return setorId;
    }

    public void setSetorId(Long setorId) {
        this.setorId = setorId;
    }
}
