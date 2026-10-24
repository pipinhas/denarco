package br.com.denarco.controller;

import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Chamado pelo JavaScript do tutorial quando a pessoa termina ou pula o tour. */
@Controller
public class TutorialController {

    private final UsuarioService usuarioService;

    public TutorialController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/tutorial/concluido")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void concluir(@AuthenticationPrincipal UsuarioLogado logado) {
        usuarioService.concluirTutorial(logado.getId());
    }
}
