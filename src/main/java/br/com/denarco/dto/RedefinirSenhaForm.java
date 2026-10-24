package br.com.denarco.dto;

import jakarta.validation.constraints.Size;

public class RedefinirSenhaForm {

    private String token;

    @Size(min = 8, max = 72, message = "A senha precisa ter pelo menos 8 caracteres.")
    private String senha;

    private String confirmacao;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacao() {
        return confirmacao;
    }

    public void setConfirmacao(String confirmacao) {
        this.confirmacao = confirmacao;
    }
}
