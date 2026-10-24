package br.com.denarco.controller;

import br.com.denarco.security.UsuarioLogado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Coloca o usuário logado em todas as telas, para o cabeçalho e o menu (null se ninguém estiver logado). */
@ControllerAdvice
public class DadosGlobais {

    @ModelAttribute("usuarioLogado")
    public UsuarioLogado usuarioLogado(@AuthenticationPrincipal UsuarioLogado logado) {
        return logado;
    }
}
