package br.com.denarco.controller;

import br.com.denarco.dto.DemandaProxima;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.Usuario;
import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.ApoioService;
import br.com.denarco.service.DemandaService;
import br.com.denarco.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/** Tela inicial depois do login. Cada perfil vê o seu painel. */
@Controller
public class InicioController {

    private final UsuarioService usuarioService;
    private final DemandaService demandaService;
    private final ApoioService apoioService;

    public InicioController(UsuarioService usuarioService, DemandaService demandaService, ApoioService apoioService) {
        this.usuarioService = usuarioService;
        this.demandaService = demandaService;
        this.apoioService = apoioService;
    }

    /**
     * Início do cidadão: números, minhas demandas abertas e demandas de outras pessoas.
     * Se o navegador mandar a localização (lat/lng), mostra as mais próximas; senão, as mais apoiadas.
     */
    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UsuarioLogado logado,
                         @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
                         @RequestParam(required = false) String tutorial, Model model) {
        // Gestor e admin começam pelo painel; o servidor, pela fila. O "?tutorial" segue junto.
        String sufixo = tutorial != null ? "?tutorial=1" : "";
        if (logado.isGestorOuAcima()) {
            return "redirect:/painel" + sufixo;
        }
        if (logado.isServidorOuAcima()) {
            return "redirect:/atendimento" + sufixo;
        }
        Usuario usuario = usuarioService.buscar(logado.getId());

        List<Demanda> outras;
        List<DemandaProxima> perto = null;
        if (lat != null && lng != null) {
            perto = demandaService.pertoDe(lat, lng, logado.getId());
            outras = perto.stream().map(DemandaProxima::demanda).toList();
        } else {
            outras = demandaService.maisApoiadasDeOutros(logado.getId());
        }

        List<Demanda> minhasAbertas = demandaService.minhasEmAberto(logado.getId());
        List<Demanda> todas = new ArrayList<>(outras);
        todas.addAll(minhasAbertas);

        model.addAttribute("usuario", usuario);
        model.addAttribute("resumo", demandaService.resumoDoCidadao(logado.getId()));
        model.addAttribute("minhasAbertas", minhasAbertas.stream().limit(5).toList());
        model.addAttribute("outras", outras);
        model.addAttribute("perto", perto);
        model.addAttribute("idsApoiados", apoioService.idsApoiados(logado.getId(), todas));
        model.addAttribute("mostrarTutorial", !usuario.isTutorialConcluido() || tutorial != null);
        model.addAttribute("menuAtivo", "inicio");
        return "inicio/cidadao";
    }
}
