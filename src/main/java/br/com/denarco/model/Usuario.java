package br.com.denarco.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/**
 * Pessoa que usa o sistema: cidadão ou servidor da Prefeitura.
 * A senha nunca é guardada; só o hash BCrypt dela.
 */
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String senhaHash;
    private String telefone;

    @Enumerated(EnumType.STRING)
    private Perfil perfil;

    // Só preenchido em contas corporativas.
    @Enumerated(EnumType.STRING)
    private Atuacao atuacao;

    @Enumerated(EnumType.STRING)
    private StatusConta statusConta;

    // Servidor e gestor pertencem a um setor. Gestor ou admin sem setor enxergam todos.
    @ManyToOne
    private Setor setor;

    private boolean tutorialConcluido;
    private LocalDateTime criadoEm;

    public boolean isPendente() {
        return statusConta == StatusConta.PENDENTE;
    }

    public Long getId() {
        return id;
    }

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

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public Atuacao getAtuacao() {
        return atuacao;
    }

    public void setAtuacao(Atuacao atuacao) {
        this.atuacao = atuacao;
    }

    public StatusConta getStatusConta() {
        return statusConta;
    }

    public void setStatusConta(StatusConta statusConta) {
        this.statusConta = statusConta;
    }

    public Setor getSetor() {
        return setor;
    }

    public void setSetor(Setor setor) {
        this.setor = setor;
    }

    public boolean isTutorialConcluido() {
        return tutorialConcluido;
    }

    public void setTutorialConcluido(boolean tutorialConcluido) {
        this.tutorialConcluido = tutorialConcluido;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
