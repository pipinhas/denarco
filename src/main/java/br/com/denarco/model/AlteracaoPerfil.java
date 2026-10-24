package br.com.denarco.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/** Registro de quem mudou o perfil de quem, e quando. Nunca é alterado depois de salvo. */
@Entity
public class AlteracaoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private Perfil perfilAnterior;

    @Enumerated(EnumType.STRING)
    private Perfil perfilNovo;

    @ManyToOne
    private Setor setor;

    @ManyToOne
    private Usuario alteradoPor;

    private LocalDateTime criadoEm;

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Perfil getPerfilAnterior() {
        return perfilAnterior;
    }

    public void setPerfilAnterior(Perfil perfilAnterior) {
        this.perfilAnterior = perfilAnterior;
    }

    public Perfil getPerfilNovo() {
        return perfilNovo;
    }

    public void setPerfilNovo(Perfil perfilNovo) {
        this.perfilNovo = perfilNovo;
    }

    public Setor getSetor() {
        return setor;
    }

    public void setSetor(Setor setor) {
        this.setor = setor;
    }

    public Usuario getAlteradoPor() {
        return alteradoPor;
    }

    public void setAlteradoPor(Usuario alteradoPor) {
        this.alteradoPor = alteradoPor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
