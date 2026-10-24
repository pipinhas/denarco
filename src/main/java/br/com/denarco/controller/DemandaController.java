package br.com.denarco.controller;

import br.com.denarco.dto.NovaDemandaForm;
import br.com.denarco.exception.NaoEncontradoException;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Alcance;
import br.com.denarco.model.Categoria;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.HistoricoDemanda;
import br.com.denarco.model.MotivoCancelamento;
import br.com.denarco.model.Urgencia;
import br.com.denarco.model.Usuario;
import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.ApoioService;
import br.com.denarco.service.AtendimentoService;
import br.com.denarco.service.CategoriaService;
import br.com.denarco.service.DemandaService;
import br.com.denarco.service.FotoService;
import br.com.denarco.service.HistoricoService;
import br.com.denarco.service.PrioridadeService;
import br.com.denarco.service.SetorService;
import br.com.denarco.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/** Telas de demanda do cidadão: registrar, ver detalhes, apoiar e listar as suas. */
@Controller
public class DemandaController {

    private final DemandaService demandaService;
    private final CategoriaService categoriaService;
    private final ApoioService apoioService;
    private final HistoricoService historicoService;
    private final PrioridadeService prioridadeService;
    private final FotoService fotoService;
    private final AtendimentoService atendimentoService;
    private final UsuarioService usuarioService;
    private final SetorService setorService;

    public DemandaController(DemandaService demandaService, CategoriaService categoriaService,
                             ApoioService apoioService, HistoricoService historicoService,
                             PrioridadeService prioridadeService, FotoService fotoService,
                             AtendimentoService atendimentoService, UsuarioService usuarioService,
                             SetorService setorService) {
        this.demandaService = demandaService;
        this.categoriaService = categoriaService;
        this.apoioService = apoioService;
        this.historicoService = historicoService;
        this.prioridadeService = prioridadeService;
        this.fotoService = fotoService;
        this.atendimentoService = atendimentoService;
        this.usuarioService = usuarioService;
        this.setorService = setorService;
    }

    @GetMapping("/demandas/nova")
    public String nova(Model model) {
        model.addAttribute("form", new NovaDemandaForm());
        prepararFormulario(model);
        return "demanda/nova";
    }

    @PostMapping("/demandas")
    public String registrar(@Valid @ModelAttribute("form") NovaDemandaForm form, BindingResult resultado,
                            @AuthenticationPrincipal UsuarioLogado logado, Model model,
                            RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Demanda demanda = demandaService.registrar(form, logado.getId());
                redirect.addFlashAttribute("criada", true);
                return "redirect:/demandas/" + demanda.getProtocolo();
            } catch (NegocioException e) {
                resultado.reject("negocio", e.getMessage());
            }
        }
        prepararFormulario(model);
        return "demanda/nova";
    }

    @GetMapping("/demandas/{protocolo:ARC-.+}")
    public String detalhe(@PathVariable String protocolo, @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        Demanda demanda = demandaService.buscarPorProtocolo(protocolo);
        Usuario usuario = usuarioService.buscar(logado.getId());
        boolean podeGerir = atendimentoService.podeGerir(demanda, usuario);

        model.addAttribute("demanda", demanda);
        model.addAttribute("historico", historicoService.listar(demanda.getId()));
        model.addAttribute("parcelas", prioridadeService.explicar(demanda));
        model.addAttribute("podeVerPrivado", demandaService.podeVerDadosPrivados(demanda, logado));
        model.addAttribute("ehAutor", demanda.getAutor().getId().equals(logado.getId()));
        model.addAttribute("jaApoiei", apoioService.jaApoiou(demanda.getId(), logado.getId()));
        model.addAttribute("podeAtender", atendimentoService.podeAtender(demanda, usuario));
        model.addAttribute("podeGerir", podeGerir);
        model.addAttribute("podeReabrir", atendimentoService.podeReabrir(demanda, usuario));

        // Listas dos formulários de decisão (só para quem pode gerir).
        if (podeGerir) {
            model.addAttribute("responsaveis", atendimentoService.possiveisResponsaveis(demanda.getSetor()));
            model.addAttribute("setores", setorService.listarAtivos());
            model.addAttribute("urgencias", Urgencia.values());
            model.addAttribute("alcances", Alcance.values());
            model.addAttribute("motivos", MotivoCancelamento.values());
            model.addAttribute("hoje", LocalDate.now());
            model.addAttribute("prazoSugerido", LocalDate.now().plusDays(demanda.getCategoria().getPrazoPadraoDias()));
        }
        return "demanda/detalhe";
    }

    @PostMapping("/demandas/{id}/reabrir")
    public String reabrir(@PathVariable Long id, @RequestParam(required = false) String motivo,
                          @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Demanda demanda = demandaService.buscar(id);
        try {
            atendimentoService.reabrir(id, motivo, usuarioService.buscar(logado.getId()));
            redirect.addFlashAttribute("mensagem", "Demanda reaberta. A equipe vai analisar de novo.");
        } catch (NegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/demandas/" + demanda.getProtocolo();
    }

    /** Foto anexada a uma providência. Mesma regra da foto da demanda: só autor e equipe do setor. */
    @GetMapping("/historico/{id}/foto")
    public ResponseEntity<Resource> fotoDoHistorico(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado) {
        HistoricoDemanda passo = historicoService.buscar(id);
        if (passo.getFoto() == null || !demandaService.podeVerDadosPrivados(passo.getDemanda(), logado)) {
            throw new NaoEncontradoException("Foto não encontrada.");
        }
        return arquivoDeFoto(passo.getFoto());
    }

    @GetMapping("/demandas/minhas")
    public String minhas(@AuthenticationPrincipal UsuarioLogado logado, Model model) {
        model.addAttribute("demandas", demandaService.minhas(logado.getId()));
        model.addAttribute("menuAtivo", "minhas");
        return "demanda/minhas";
    }

    @PostMapping("/demandas/{id}/apoio")
    public String apoiar(@PathVariable Long id, @RequestParam(required = false) String voltar,
                         @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        try {
            boolean apoiou = apoioService.alternar(id, logado.getId());
            redirect.addFlashAttribute("mensagem", apoiou
                    ? "Obrigado! Seu apoio mostra que o problema continua."
                    : "Apoio retirado.");
        } catch (NegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        // Só volta para endereços internos, para ninguém usar o link para mandar a pessoa para outro site.
        boolean voltaValida = voltar != null && voltar.startsWith("/") && !voltar.startsWith("//");
        return "redirect:" + (voltaValida ? voltar : "/");
    }

    @GetMapping("/demandas/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado) {
        Demanda demanda = demandaService.buscar(id);
        if (demanda.getFoto() == null || !demandaService.podeVerDadosPrivados(demanda, logado)) {
            throw new NaoEncontradoException("Foto não encontrada.");
        }
        return arquivoDeFoto(demanda.getFoto());
    }

    private ResponseEntity<Resource> arquivoDeFoto(String nome) {
        Resource arquivo = new FileSystemResource(fotoService.caminho(nome));
        if (!arquivo.exists()) {
            throw new NaoEncontradoException("Foto não encontrada.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(arquivo);
    }

    private void prepararFormulario(Model model) {
        List<Categoria> categorias = categoriaService.listarAtivas();
        model.addAttribute("categorias", categorias);
        model.addAttribute("urgencias", Urgencia.values());
        model.addAttribute("alcances", Alcance.values());
        model.addAttribute("menuAtivo", "nova");
    }
}
