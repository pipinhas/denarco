package br.com.denarco.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados da tela "Criar conta". Também é a base do cadastro corporativo. */
public class CadastroForm {

    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "Use no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Digite um e-mail válido, como nome@exemplo.com.")
    private String email;

    @Size(min = 8, max = 72, message = "A senha precisa ter pelo menos 8 caracteres.")
    private String senha;

    @Pattern(regexp = "^[0-9()+\\s-]{10,20}$", message = "Digite o telefone com DDD, como (87) 99999-9999.")
    private String telefone;

    @AssertTrue(message = "Para criar a conta, aceite o aviso de privacidade.")
    private boolean aceitouPrivacidade;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public boolean isAceitouPrivacidade() {
        return aceitouPrivacidade;
    }

    public void setAceitouPrivacidade(boolean aceitouPrivacidade) {
        this.aceitouPrivacidade = aceitouPrivacidade;
    }
}
