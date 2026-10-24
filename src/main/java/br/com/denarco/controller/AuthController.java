package br.com.denarco.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Páginas públicas de entrada. O POST do login é tratado pelo próprio Spring Security. */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        boolean jaLogado = autenticacao != null && autenticacao.isAuthenticated()
                && !"anonymousUser".equals(autenticacao.getPrincipal());
        return jaLogado ? "redirect:/" : "auth/login";
    }

    @GetMapping("/privacidade")
    public String privacidade() {
        return "auth/privacidade";
    }
}
