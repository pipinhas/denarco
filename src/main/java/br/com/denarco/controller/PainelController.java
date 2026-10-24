package br.com.denarco.controller;

import br.com.denarco.dto.DadosGraficos;
import br.com.denarco.dto.ResultadoConsulta;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Usuario;
import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.AtendimentoService;
import br.com.denarco.service.ConsultaService;
import br.com.denarco.service.PainelService;
import br.com.denarco.service.SetorService;
import br.com.denarco.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Painel de gestão (números, gráficos, ranking de bairros) e a consulta em linguagem simples. */
@Controller
public class PainelController {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PainelService painelService;
    private final ConsultaService consultaService;
    private final UsuarioService usuarioService;
    private final SetorService setorService;

    public PainelController(PainelService painelService, ConsultaService consultaService,
                            UsuarioService usuarioService, SetorService setorService) {
        this.painelService = painelService;
        this.consultaService = consultaService;
        this.usuarioService = usuarioService;
        this.setorService = setorService;
    }

    @GetMapping("/painel")
    public String painel(@RequestParam(required = false) Integer dias, @RequestParam(required = false) Long setorId,
                         @RequestParam(required = false) String tutorial,
                         @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        Usuario usuario = usuarioService.buscar(logado.getId());
        int periodo = PainelService.periodoValido(dias);
        Long escopo = escopo(usuario, setorId);

        model.addAttribute("resumo", painelService.resumo(escopo, periodo));
        model.addAttribute("dias", periodo);
        model.addAttribute("periodos", PainelService.PERIODOS);
        model.addAttribute("setorId", escopo);
        model.addAttribute("enxergaTodos", AtendimentoService.enxergaTodosOsSetores(usuario));
        model.addAttribute("setores", setorService.listarAtivos());
        model.addAttribute("tituloEscopo", escopo == null ? "Todos os setores" : setorService.buscar(escopo).getNome());
        // "Fonte e período" em todo número, como pede o caderno de contexto.
        model.addAttribute("fonte", "Fonte: base do Denarco · " + LocalDate.now().minusDays(periodo).format(DATA)
                + " a " + LocalDate.now().format(DATA) + " · atualizado às "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        // O painel é a tela inicial do gestor e do admin: o tutorial deles roda aqui.
        model.addAttribute("mostrarTutorial", usuario.getPerfil().peloMenos(Perfil.GESTOR)
                && (!usuario.isTutorialConcluido() || tutorial != null));
        model.addAttribute("menuAtivo", "painel");
        return "painel/painel";
    }

    /** Dados dos gráficos em JSON, lidos pelo painel.js. */
    @GetMapping("/api/painel")
    @ResponseBody
    public DadosGraficos dadosDosGraficos(@RequestParam(required = false) Integer dias,
                                          @RequestParam(required = false) Long setorId,
                                          @AuthenticationPrincipal UsuarioLogado logado) {
        Usuario usuario = usuarioService.buscar(logado.getId());
        return painelService.graficos(escopo(usuario, setorId), PainelService.periodoValido(dias));
    }

    @GetMapping("/consulta")
    public String consulta(@RequestParam(required = false) String q, @AuthenticationPrincipal UsuarioLogado logado,
                           Model model) {
        if (q != null && !q.isBlank()) {
            ResultadoConsulta resultado = consultaService.consultar(q, usuarioService.buscar(logado.getId()));
            model.addAttribute("resultado", resultado);
        }
        model.addAttribute("q", q);
        model.addAttribute("menuAtivo", "painel");
        return "painel/consulta";
    }

    // Mesma regra da fila: quem é de um setor só vê o seu.
    private Long escopo(Usuario usuario, Long setorPedido) {
        if (AtendimentoService.enxergaTodosOsSetores(usuario)) {
            return setorPedido;
        }
        return usuario.getSetor() != null ? usuario.getSetor().getId() : -1L;
    }
}
