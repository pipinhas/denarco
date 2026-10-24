package br.com.denarco.config;

import br.com.denarco.model.Perfil;
import br.com.denarco.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador quando o sistema sobe pela primeira vez.
 * E-mail e senha vêm da configuração (ADMIN_EMAIL e ADMIN_SENHA), nunca do código.
 */
@Component
@Order(1)
public class DadosIniciais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final UsuarioService usuarioService;
    private final String nome;
    private final String email;
    private final String senha;

    public DadosIniciais(UsuarioService usuarioService,
                         @Value("${app.admin.nome}") String nome,
                         @Value("${app.admin.email}") String email,
                         @Value("${app.admin.senha}") String senha) {
        this.usuarioService = usuarioService;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (usuarioService.existeAdmin()) {
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Nenhum administrador cadastrado. Defina ADMIN_EMAIL e ADMIN_SENHA para criar o primeiro.");
            return;
        }
        usuarioService.criarContaAtiva(nome, email, senha, "(87) 0000-0000", Perfil.ADMIN, null);
        log.info("Administrador inicial criado: {}", email);
    }
}
