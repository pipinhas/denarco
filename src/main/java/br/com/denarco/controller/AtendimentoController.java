package br.com.denarco.controller;

import br.com.denarco.dto.AbaFila;
import br.com.denarco.dto.FiltroFila;
import br.com.denarco.dto.NovaDemandaForm;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Alcance;
import br.com.denarco.model.Canal;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Urgencia;
import br.com.denarco.model.Usuario;
import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.AtendimentoService;
import br.com.denarco.service.CategoriaService;
import br.com.denarco.service.DemandaService;
import br.com.denarco.service.SetorService;
import br.com.denarco.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.function.Supplier;

/** Telas da equipe (servidor ou acima): fila do setor, registro por telefone e ações de execução. */
@Controller
@RequestMapping("/atendimento")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;
    private final DemandaService demandaService;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final SetorService setorService;

    public AtendimentoController(AtendimentoService atendimentoService, DemandaService demandaService,
                                 UsuarioService usuarioService, CategoriaService categoriaService,
                                 SetorService setorService) {
        this.atendimentoService = atendimentoService;
        this.demandaService = demandaService;
        this.usuarioService = usuarioService;
        this.categoriaService = categoriaService;
        this.setorService = setorService;
    }

    @GetMapping
    public String fila(@ModelAttribute("filtro") FiltroFila filtro, @RequestParam(required = false) String tutorial,
                       @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        Usuario usuario = usuarioService.buscar(logado.getId());
        boolean enxergaTodos = AtendimentoService.enxergaTodosOsSetores(usuario);

        String titulo;
        if (!enxergaTodos) {
            titulo = usuario.getSetor().getNome();
        } else if (filtro.getSetorId() != null) {
            titulo = setorService.buscar(filtro.getSetorId()).getNome();
        } else {
            titulo = "Todos os setores";
        }

        model.addAttribute("pagina", atendimentoService.fila(filtro, usuario));
        model.addAttribute("resumo", atendimentoService.resumo(filtro, usuario));
        model.addAttribute("abas", AbaFila.values());
        model.addAttribute("categorias", categoriaService.listarAtivas());
        model.addAttribute("setores", setorService.listarAtivos());
        model.addAttribute("enxergaTodos", enxergaTodos);
        model.addAttribute("tituloFila", titulo);
        // A fila é a tela inicial do servidor: o tutorial dele roda aqui (o do gestor roda no painel).
        model.addAttribute("mostrarTutorial", usuario.getPerfil() == Perfil.SERVIDOR
                && (!usuario.isTutorialConcluido() || tutorial != null));
        model.addAttribute("menuAtivo", "fila");
        return "atendimento/fila";
    }

    // ---------- Registrar demanda recebida por telefone, presencial ou WhatsApp ----------

    @GetMapping("/registrar")
    public String registrar(Model model) {
        NovaDemandaForm form = new NovaDemandaForm();
        form.setCanal(Canal.TELEFONE);
        model.addAttribute("form", form);
        prepararFormulario(model);
        return "demanda/nova";
    }

    @PostMapping("/registrar")
    public String salvarRegistro(@Valid @ModelAttribute("form") NovaDemandaForm form, BindingResult resultado,
                                 @AuthenticationPrincipal UsuarioLogado logado, Model model,
                                 RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Demanda demanda = demandaService.registrarPorAtendente(form, logado.getId());
                redirect.addFlashAttribute("criada", true);
                return "redirect:/demandas/" + demanda.getProtocolo();
            } catch (NegocioException e) {
                resultado.reject("negocio", e.getMessage());
            }
        }
        prepararFormulario(model);
        return "demanda/nova";
    }

    private void prepararFormulario(Model model) {
        model.addAttribute("categorias", categoriaService.listarAtivas());
        model.addAttribute("urgencias", Urgencia.values());
        model.addAttribute("alcances", Alcance.values());
        // O mesmo formulário do cidadão, com os campos do atendente (canal e quem ligou).
        model.addAttribute("modoAtendente", true);
        model.addAttribute("canais", Arrays.stream(Canal.values()).filter(c -> c != Canal.APP).toList());
        model.addAttribute("menuAtivo", "registrar");
    }

    // ---------- Ações de execução ----------

    @PostMapping("/demandas/{id}/iniciar")
    public String iniciar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                          RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "Atendimento iniciado.", () -> atendimentoService.iniciar(id, ator));
    }

    @PostMapping("/demandas/{id}/providencia")
    public String providencia(@PathVariable Long id, @RequestParam(required = false) String texto,
                              @RequestParam(required = false) MultipartFile foto,
                              @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "Providência registrada na linha do tempo.",
                () -> atendimentoService.registrarProvidencia(id, texto, foto, ator));
    }

    @PostMapping("/demandas/{id}/concluir")
    public String concluir(@PathVariable Long id, @RequestParam(required = false) String texto,
                           @RequestParam(required = false) MultipartFile foto,
                           @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "Demanda concluída. Quem registrou foi avisado.",
                () -> atendimentoService.concluir(id, texto, foto, ator));
    }

    /** Roda a ação e volta para a página da demanda, com mensagem de sucesso ou com o erro de regra de negócio. */
    private String executar(Long demandaId, RedirectAttributes redirect, String sucesso, Supplier<Demanda> acao) {
        try {
            Demanda demanda = acao.get();
            redirect.addFlashAttribute("mensagem", sucesso);
            return "redirect:/demandas/" + demanda.getProtocolo();
        } catch (NegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/demandas/" + demandaService.buscar(demandaId).getProtocolo();
        }
    }
}
