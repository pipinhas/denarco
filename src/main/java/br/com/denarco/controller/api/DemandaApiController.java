package br.com.denarco.controller.api;

import br.com.denarco.dto.DemandaProxima;
import br.com.denarco.dto.DemandaProximaResposta;
import br.com.denarco.model.Demanda;
import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.ApoioService;
import br.com.denarco.service.DemandaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints JSON usados pelo JavaScript das telas (não são páginas). */
@RestController
public class DemandaApiController {

    private final DemandaService demandaService;
    private final ApoioService apoioService;

    public DemandaApiController(DemandaService demandaService, ApoioService apoioService) {
        this.demandaService = demandaService;
        this.apoioService = apoioService;
    }

    /** Aviso de possível duplicada na tela de nova demanda. Nunca devolve dados de quem registrou. */
    @GetMapping("/api/demandas/proximas")
    public List<DemandaProximaResposta> proximas(@RequestParam Long categoriaId, @RequestParam double latitude,
                                                 @RequestParam double longitude,
                                                 @AuthenticationPrincipal UsuarioLogado logado) {
        return demandaService.possiveisDuplicadas(categoriaId, latitude, longitude).stream()
                .limit(3)
                .map(item -> paraResposta(item, logado))
                .toList();
    }

    private DemandaProximaResposta paraResposta(DemandaProxima item, UsuarioLogado logado) {
        Demanda d = item.demanda();
        return new DemandaProximaResposta(d.getId(), d.getProtocolo(), d.getCategoria().getNome(),
                d.getDescricao(), d.getLocalResumido(), item.distanciaMetros(), d.getTotalApoios(),
                apoioService.jaApoiou(d.getId(), logado.getId()), d.getAutor().getId().equals(logado.getId()));
    }
}
