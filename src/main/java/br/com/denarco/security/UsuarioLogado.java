package br.com.denarco.security;

import br.com.denarco.model.Perfil;
import br.com.denarco.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Dados do usuário guardados na sessão depois do login.
 * Mudanças de perfil passam a valer no próximo login.
 */
public class UsuarioLogado implements UserDetails {

    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;
    private final Long setorId;

    public UsuarioLogado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.perfil = usuario.getPerfil();
        this.setorId = usuario.getSetor() != null ? usuario.getSetor().getId() : null;
    }

    // O Spring Security trabalha com "ROLE_" + nome do perfil (ex.: ROLE_GESTOR).
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getPrimeiroNome() {
        return nome.split(" ")[0];
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public Long getSetorId() {
        return setorId;
    }

    public boolean isServidorOuAcima() {
        return perfil.peloMenos(Perfil.SERVIDOR);
    }

    public boolean isGestorOuAcima() {
        return perfil.peloMenos(Perfil.GESTOR);
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }
}
