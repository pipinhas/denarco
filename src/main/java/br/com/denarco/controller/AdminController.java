package br.com.denarco.controller;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.service.CategoriaService;
import br.com.denarco.service.SetorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Configurações do admin: setores e tipos de problema (com setor padrão e prazo sugerido). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SetorService setorService;
    private final CategoriaService categoriaService;

    public AdminController(SetorService setorService, CategoriaService categoriaService) {
        this.setorService = setorService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("setores", setorService.listarTodos());
        model.addAttribute("setoresAtivos", setorService.listarAtivos());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("icones", CategoriaService.ICONES);
        model.addAttribute("menuAtivo", "configuracoes");
        return "admin/configuracoes";
    }

    @PostMapping("/setores")
    public String criarSetor(@RequestParam(required = false) String nome, RedirectAttributes redirect) {
        return executar(redirect, "Setor criado.", () -> setorService.criar(nome));
    }

    @PostMapping("/setores/{id}/ativo")
    public String alternarSetor(@PathVariable Long id, RedirectAttributes redirect) {
        return executar(redirect, "Setor atualizado.", () -> setorService.alternarAtivo(id));
    }

    @PostMapping("/categorias")
    public String criarCategoria(@RequestParam(required = false) String nome, @RequestParam(required = false) String icone,
                                 @RequestParam(required = false) Long setorPadraoId,
                                 @RequestParam(required = false) Integer prazoDias, RedirectAttributes redirect) {
        return executar(redirect, "Tipo de problema criado.",
                () -> categoriaService.criar(nome, icone, setorPadraoId, prazoDias));
    }

    @PostMapping("/categorias/{id}")
    public String atualizarCategoria(@PathVariable Long id, @RequestParam(required = false) String icone,
                                     @RequestParam(required = false) Long setorPadraoId,
                                     @RequestParam(required = false) Integer prazoDias, RedirectAttributes redirect) {
        return executar(redirect, "Tipo de problema atualizado.",
                () -> categoriaService.atualizar(id, icone, setorPadraoId, prazoDias));
    }

    @PostMapping("/categorias/{id}/ativa")
    public String alternarCategoria(@PathVariable Long id, RedirectAttributes redirect) {
        return executar(redirect, "Tipo de problema atualizado.", () -> categoriaService.alternarAtiva(id));
    }

    private String executar(RedirectAttributes redirect, String sucesso, Runnable acao) {
        try {
            acao.run();
            redirect.addFlashAttribute("mensagem", sucesso);
        } catch (NegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/configuracoes";
    }
}
