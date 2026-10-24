package br.com.denarco.controller;

import br.com.denarco.dto.CadastroCorporativoForm;
import br.com.denarco.dto.CadastroForm;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Atuacao;
import br.com.denarco.model.Setor;
import br.com.denarco.model.Usuario;
import br.com.denarco.service.SetorService;
import br.com.denarco.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Telas "Criar conta" (cidadão) e "Criar conta corporativa" (servidor da Prefeitura). */
@Controller
public class CadastroController {

    private final UsuarioService usuarioService;
    private final SetorService setorService;

    public CadastroController(UsuarioService usuarioService, SetorService setorService) {
        this.usuarioService = usuarioService;
        this.setorService = setorService;
    }

    // Listas usadas pelos selects do cadastro corporativo.
    @ModelAttribute("setores")
    public List<Setor> setores() {
        return setorService.listarAtivos();
    }

    @ModelAttribute("atuacoes")
    public Atuacao[] atuacoes() {
        return Atuacao.values();
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("form", new CadastroForm());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult resultado,
                            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "auth/cadastro";
        }
        try {
            usuarioService.cadastrarCidadao(form);
        } catch (NegocioException e) {
            resultado.reject("negocio", e.getMessage());
            return "auth/cadastro";
        }
        redirect.addFlashAttribute("mensagem", "Conta criada! Agora é só entrar.");
        redirect.addFlashAttribute("email", form.getEmail());
        return "redirect:/login";
    }

    @GetMapping("/cadastro/corporativo")
    public String cadastroCorporativo(Model model) {
        model.addAttribute("form", new CadastroCorporativoForm());
        return "auth/cadastro-corporativo";
    }

    @PostMapping("/cadastro/corporativo")
    public String cadastrarCorporativo(@Valid @ModelAttribute("form") CadastroCorporativoForm form,
                                       BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "auth/cadastro-corporativo";
        }
        Usuario usuario;
        try {
            usuario = usuarioService.cadastrarCorporativo(form);
        } catch (NegocioException e) {
            resultado.reject("negocio", e.getMessage());
            return "auth/cadastro-corporativo";
        }
        redirect.addFlashAttribute("mensagem", usuario.isPendente()
                ? "Conta criada! Ela será liberada quando um gestor aprovar. Enquanto isso, você já pode entrar como cidadão."
                : "Conta criada e já liberada pelo seu convite. É só entrar.");
        redirect.addFlashAttribute("email", form.getEmail());
        return "redirect:/login";
    }
}
