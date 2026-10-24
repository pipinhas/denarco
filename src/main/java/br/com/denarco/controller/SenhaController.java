package br.com.denarco.controller;

import br.com.denarco.dto.RedefinirSenhaForm;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.service.SenhaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/** Telas "Esqueci minha senha" e "Criar nova senha". */
@Controller
public class SenhaController {

    private final SenhaService senhaService;
    private final boolean modoDemo;

    public SenhaController(SenhaService senhaService, @Value("${app.modo-demo}") boolean modoDemo) {
        this.senhaService = senhaService;
        this.modoDemo = modoDemo;
    }

    @GetMapping("/senha/esqueci")
    public String esqueci() {
        return "auth/esqueci-senha";
    }

    @PostMapping("/senha/esqueci")
    public String solicitar(@RequestParam String email, Model model) {
        if (email.isBlank()) {
            model.addAttribute("erro", "Informe o e-mail da sua conta.");
            return "auth/esqueci-senha";
        }
        Optional<String> link = senhaService.solicitarRedefinicao(email);
        model.addAttribute("enviado", true);
        // Só na demonstração: mostra o link na tela, já que não há servidor de e-mail.
        if (modoDemo) {
            link.ifPresent(caminho -> model.addAttribute("linkDemo", caminho));
        }
        return "auth/esqueci-senha";
    }

    @GetMapping("/senha/redefinir")
    public String redefinir(@RequestParam(defaultValue = "") String token, Model model) {
        RedefinirSenhaForm form = new RedefinirSenhaForm();
        form.setToken(token);
        model.addAttribute("form", form);
        model.addAttribute("linkInvalido", !senhaService.tokenValido(token));
        return "auth/redefinir-senha";
    }

    @PostMapping("/senha/redefinir")
    public String salvar(@Valid @ModelAttribute("form") RedefinirSenhaForm form, BindingResult resultado,
                         Model model, RedirectAttributes redirect) {
        if (form.getSenha() != null && !form.getSenha().equals(form.getConfirmacao())) {
            resultado.rejectValue("confirmacao", "diferente", "As duas senhas precisam ser iguais.");
        }
        if (resultado.hasErrors()) {
            model.addAttribute("linkInvalido", false);
            return "auth/redefinir-senha";
        }
        try {
            senhaService.redefinir(form.getToken(), form.getSenha());
        } catch (NegocioException e) {
            model.addAttribute("linkInvalido", true);
            return "auth/redefinir-senha";
        }
        redirect.addFlashAttribute("mensagem", "Senha alterada. Entre com a nova senha.");
        return "redirect:/login";
    }
}
