package br.com.denarco.controller;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Alcance;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.MotivoCancelamento;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Urgencia;
import br.com.denarco.model.Usuario;
import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.AtendimentoService;
import br.com.denarco.service.DemandaService;
import br.com.denarco.service.EquipeService;
import br.com.denarco.service.SetorService;
import br.com.denarco.service.UsuarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Telas e ações do gestor (e do admin): decidir sobre as demandas e cuidar da equipe. */
@Controller
@RequestMapping("/gestao")
public class GestaoController {

    private final AtendimentoService atendimentoService;
    private final EquipeService equipeService;
    private final DemandaService demandaService;
    private final UsuarioService usuarioService;
    private final SetorService setorService;

    public GestaoController(AtendimentoService atendimentoService, EquipeService equipeService,
                            DemandaService demandaService, UsuarioService usuarioService, SetorService setorService) {
        this.atendimentoService = atendimentoService;
        this.equipeService = equipeService;
        this.demandaService = demandaService;
        this.usuarioService = usuarioService;
        this.setorService = setorService;
    }

    // ---------- Decisões sobre a demanda ----------

    @PostMapping("/demandas/{id}/encaminhar")
    public String encaminhar(@PathVariable Long id, @RequestParam(required = false) Long responsavelId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazo,
                             @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "Responsável e prazo definidos.",
                () -> atendimentoService.encaminhar(id, responsavelId, prazo, ator));
    }

    @PostMapping("/demandas/{id}/transferir")
    public String transferir(@PathVariable Long id, @RequestParam(required = false) Long setorId,
                             @RequestParam(required = false) String motivo,
                             @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        try {
            if (setorId == null) {
                throw new NegocioException("Escolha o setor de destino.");
            }
            Demanda demanda = atendimentoService.transferirSetor(id, setorId, motivo, ator);
            redirect.addFlashAttribute("mensagem", "Demanda " + demanda.getProtocolo() + " transferida para "
                    + demanda.getSetor().getNome() + ".");
            // Depois de transferir, a demanda pode sair do alcance do gestor: volta para a fila.
            return "redirect:/atendimento";
        } catch (NegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/demandas/" + demandaService.buscar(id).getProtocolo();
        }
    }

    @PostMapping("/demandas/{id}/classificacao")
    public String classificacao(@PathVariable Long id, @RequestParam(required = false) Urgencia urgencia,
                                @RequestParam(required = false) Alcance alcance,
                                @RequestParam(defaultValue = "false") boolean risco,
                                @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "Classificação corrigida e prioridade recalculada.",
                () -> atendimentoService.corrigirClassificacao(id, urgencia, alcance, risco, ator));
    }

    @PostMapping("/demandas/{id}/prioridade")
    public String prioridade(@PathVariable Long id, @RequestParam(required = false) Integer valor,
                             @RequestParam(required = false) String justificativa,
                             @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "Prioridade ajustada.",
                () -> atendimentoService.fixarPrioridade(id, valor, justificativa, ator));
    }

    @PostMapping("/demandas/{id}/prioridade/automatica")
    public String prioridadeAutomatica(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                                       RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "A prioridade voltou a ser calculada automaticamente.",
                () -> atendimentoService.voltarPrioridadeAutomatica(id, ator));
    }

    @PostMapping("/demandas/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(required = false) MotivoCancelamento motivo,
                           @RequestParam(required = false) String protocoloOriginal,
                           @RequestParam(required = false) String observacao,
                           @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return executar(id, redirect, "Demanda cancelada.",
                () -> atendimentoService.cancelar(id, motivo, protocoloOriginal, observacao, ator));
    }

    // ---------- Equipe: pedidos de acesso, convites e perfis ----------

    @GetMapping("/equipe")
    public String equipe(@RequestParam(required = false) String email, @AuthenticationPrincipal UsuarioLogado logado,
                         Model model) {
        Usuario ator = usuarioService.buscar(logado.getId());
        List<Usuario> equipe = equipeService.equipe(ator);
        Usuario encontrado = email == null || email.isBlank() ? null : equipeService.buscarPorEmail(email).orElse(null);

        // Ids das pessoas que este gestor/admin pode alterar (a regra fica no service, não no template).
        Set<Long> gerenciaveis = new HashSet<>();
        equipe.stream().filter(u -> EquipeService.podeGerenciar(ator, u)).forEach(u -> gerenciaveis.add(u.getId()));
        if (encontrado != null && EquipeService.podeGerenciar(ator, encontrado)) {
            gerenciaveis.add(encontrado.getId());
        }

        model.addAttribute("ator", ator);
        model.addAttribute("pendentes", equipeService.pedidosPendentes(ator));
        model.addAttribute("equipe", equipe);
        model.addAttribute("gerenciaveis", gerenciaveis);
        model.addAttribute("convites", equipeService.convitesPendentes(ator));
        model.addAttribute("alteracoes", equipeService.ultimasAlteracoes(ator));
        model.addAttribute("perfis", equipeService.perfisQuePodeConceder(ator));
        model.addAttribute("perfisConvite", equipeService.perfisQuePodeConceder(ator).stream()
                .filter(p -> p != Perfil.CIDADAO).toList());
        model.addAttribute("setores", setorService.listarAtivos());
        // Só o admin e o gestor sem setor escolhem o setor; os demais usam o próprio.
        model.addAttribute("escolheSetor", AtendimentoService.enxergaTodosOsSetores(ator));
        model.addAttribute("busca", email);
        model.addAttribute("buscou", email != null && !email.isBlank());
        model.addAttribute("encontrado", encontrado);
        model.addAttribute("menuAtivo", "equipe");
        return "gestao/equipe";
    }

    @PostMapping("/equipe/{id}/aprovar")
    public String aprovar(@PathVariable Long id, @RequestParam(required = false) Perfil perfil,
                          @RequestParam(required = false) Long setorId,
                          @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return naEquipe(redirect, null, "Acesso aprovado. Vale a partir do próximo login da pessoa.",
                () -> equipeService.aprovar(id, perfil, setorId, ator));
    }

    @PostMapping("/equipe/{id}/recusar")
    public String recusar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                          RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return naEquipe(redirect, null, "Pedido recusado. A pessoa continua com acesso de cidadão.",
                () -> equipeService.recusar(id, ator));
    }

    @PostMapping("/equipe/{id}/perfil")
    public String alterarPerfil(@PathVariable Long id, @RequestParam(required = false) Perfil perfil,
                                @RequestParam(required = false) Long setorId,
                                @RequestParam(required = false) String busca,
                                @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return naEquipe(redirect, busca, "Perfil alterado. Vale a partir do próximo login da pessoa.",
                () -> equipeService.alterarPerfil(id, perfil, setorId, ator));
    }

    @PostMapping("/convites")
    public String convidar(@RequestParam(required = false) String email, @RequestParam(required = false) Perfil perfil,
                           @RequestParam(required = false) Long setorId,
                           @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return naEquipe(redirect, null, "Convite criado. Quando " + email
                        + " criar a conta corporativa, o acesso já estará liberado.",
                () -> equipeService.convidar(email, perfil, setorId, ator));
    }

    @PostMapping("/convites/{id}/cancelar")
    public String cancelarConvite(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                                  RedirectAttributes redirect) {
        Usuario ator = usuarioService.buscar(logado.getId());
        return naEquipe(redirect, null, "Convite cancelado.", () -> equipeService.cancelarConvite(id, ator));
    }

    // ---------- Apoio ----------

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

    private String naEquipe(RedirectAttributes redirect, String busca, String sucesso, Runnable acao) {
        try {
            acao.run();
            redirect.addFlashAttribute("mensagem", sucesso);
        } catch (NegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return busca == null || busca.isBlank()
                ? "redirect:/gestao/equipe"
                : "redirect:/gestao/equipe?email=" + UriUtils.encodeQueryParam(busca, StandardCharsets.UTF_8);
    }
}
