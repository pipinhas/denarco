package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.TokenRedefinicaoSenha;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.TokenRedefinicaoSenhaRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SenhaServiceTest {

    private UsuarioRepository usuarioRepository;
    private TokenRedefinicaoSenhaRepository tokenRepository;
    private EmailService emailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private SenhaService senhaService;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(TokenRedefinicaoSenhaRepository.class);
        emailService = mock(EmailService.class);
        senhaService = new SenhaService(usuarioRepository, tokenRepository, passwordEncoder, emailService, "http://localhost");
    }

    @Test
    void emailInexistenteNaoGeraTokenNemEnviaEmail() {
        when(usuarioRepository.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        assertTrue(senhaService.solicitarRedefinicao("ninguem@x.com").isEmpty());
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void tokenValidoTrocaSenhaEFicaUsado() {
        Usuario usuario = new Usuario();
        TokenRedefinicaoSenha token = token(usuario, LocalDateTime.now().plusMinutes(10), false);
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        senhaService.redefinir("abc", "novaSenha123");

        assertTrue(passwordEncoder.matches("novaSenha123", usuario.getSenhaHash()));
        assertTrue(token.isUsado());
    }

    @Test
    void tokenExpiradoNaoTrocaSenha() {
        TokenRedefinicaoSenha token = token(new Usuario(), LocalDateTime.now().minusMinutes(1), false);
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThrows(NegocioException.class, () -> senhaService.redefinir("abc", "novaSenha123"));
        assertFalse(senhaService.tokenValido("abc"));
    }

    @Test
    void tokenJaUsadoNaoPodeSerReutilizado() {
        TokenRedefinicaoSenha token = token(new Usuario(), LocalDateTime.now().plusMinutes(10), true);
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        NegocioException erro = assertThrows(NegocioException.class, () -> senhaService.redefinir("abc", "novaSenha123"));
        assertEquals("Este link expirou ou já foi usado. Peça um novo.", erro.getMessage());
    }

    private TokenRedefinicaoSenha token(Usuario usuario, LocalDateTime expiraEm, boolean usado) {
        TokenRedefinicaoSenha token = new TokenRedefinicaoSenha();
        token.setToken("abc");
        token.setUsuario(usuario);
        token.setExpiraEm(expiraEm);
        token.setUsado(usado);
        return token;
    }
}
