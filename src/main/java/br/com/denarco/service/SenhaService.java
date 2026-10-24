package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.TokenRedefinicaoSenha;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.TokenRedefinicaoSenhaRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Fluxo de "esqueci minha senha": gera o link, envia por e-mail e troca a senha. */
@Service
public class SenhaService {

    private static final int MINUTOS_DE_VALIDADE = 30;

    private final UsuarioRepository usuarioRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String urlBase;

    public SenhaService(UsuarioRepository usuarioRepository, TokenRedefinicaoSenhaRepository tokenRepository,
                        PasswordEncoder passwordEncoder, EmailService emailService,
                        @Value("${app.url-base}") String urlBase) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.urlBase = urlBase;
    }

    /**
     * Cria o token e envia o link por e-mail.
     * Devolve o caminho do link (usado só no modo demonstração) ou vazio se o e-mail não existe.
     * A tela mostra a mesma mensagem nos dois casos, para não revelar quem tem conta.
     */
    @Transactional
    public Optional<String> solicitarRedefinicao(String email) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(UsuarioService.normalizarEmail(email));
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = encontrado.get();

        TokenRedefinicaoSenha token = new TokenRedefinicaoSenha();
        token.setToken(UUID.randomUUID().toString());
        token.setUsuario(usuario);
        token.setExpiraEm(LocalDateTime.now().plusMinutes(MINUTOS_DE_VALIDADE));
        tokenRepository.save(token);

        String caminho = "/senha/redefinir?token=" + token.getToken();
        emailService.enviar(usuario.getEmail(), "Denarco: criar nova senha",
                "Olá, " + usuario.getNome() + ".\n\n"
                        + "Para criar uma nova senha, abra o link abaixo. Ele vale por " + MINUTOS_DE_VALIDADE + " minutos.\n"
                        + urlBase + caminho + "\n\n"
                        + "Se você não pediu a troca, é só ignorar este e-mail.");
        return Optional.of(caminho);
    }

    public boolean tokenValido(String token) {
        return tokenRepository.findByToken(token)
                .map(TokenRedefinicaoSenha::isValido)
                .orElse(false);
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenRedefinicaoSenha encontrado = tokenRepository.findByToken(token)
                .filter(TokenRedefinicaoSenha::isValido)
                .orElseThrow(() -> new NegocioException("Este link expirou ou já foi usado. Peça um novo."));

        Usuario usuario = encontrado.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        encontrado.setUsado(true);
        tokenRepository.save(encontrado);
    }
}
