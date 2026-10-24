package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Apoio;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.ApoioRepository;
import br.com.denarco.repository.DemandaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Botão "Ainda presente": cada pessoa apoia uma demanda uma vez; clicar de novo desfaz.
 * Os apoios entram na prioridade e mostram que o problema continua incomodando.
 */
@Service
public class ApoioService {

    private final ApoioRepository apoioRepository;
    private final DemandaRepository demandaRepository;
    private final DemandaService demandaService;
    private final UsuarioService usuarioService;
    private final PrioridadeService prioridadeService;

    public ApoioService(ApoioRepository apoioRepository, DemandaRepository demandaRepository,
                        DemandaService demandaService, UsuarioService usuarioService,
                        PrioridadeService prioridadeService) {
        this.apoioRepository = apoioRepository;
        this.demandaRepository = demandaRepository;
        this.demandaService = demandaService;
        this.usuarioService = usuarioService;
        this.prioridadeService = prioridadeService;
    }

    /** Liga ou desliga o apoio. Devolve true se a pessoa passou a apoiar. */
    @Transactional
    public boolean alternar(Long demandaId, Long usuarioId) {
        Demanda demanda = demandaService.buscar(demandaId);
        if (demanda.getAutor().getId().equals(usuarioId)) {
            throw new NegocioException("Esta demanda é sua: ela já conta como registrada por você.");
        }
        if (!demanda.getStatus().isEmAberto()) {
            throw new NegocioException("Esta demanda já foi encerrada e não recebe mais apoios.");
        }

        boolean apoiou;
        Optional<Apoio> existente = apoioRepository.findByDemandaIdAndUsuarioId(demandaId, usuarioId);
        if (existente.isPresent()) {
            apoioRepository.delete(existente.get());
            demanda.setTotalApoios(demanda.getTotalApoios() - 1);
            apoiou = false;
        } else {
            Apoio apoio = new Apoio();
            apoio.setDemanda(demanda);
            apoio.setUsuario(usuarioService.buscar(usuarioId));
            apoio.setCriadoEm(LocalDateTime.now());
            apoioRepository.save(apoio);
            demanda.setTotalApoios(demanda.getTotalApoios() + 1);
            demanda.setUltimoApoioEm(apoio.getCriadoEm());
            apoiou = true;
        }

        prioridadeService.atualizar(demanda);
        demandaRepository.save(demanda);
        return apoiou;
    }

    /**
     * Quando uma demanda é cancelada por ser duplicada, quem a registrou passa a apoiar a original:
     * assim a "voz" dessa pessoa não se perde. Devolve false se ela já era autora ou apoiadora da original.
     */
    @Transactional
    public boolean transferirApoio(Demanda original, Usuario usuario) {
        if (original.getAutor().getId().equals(usuario.getId())
                || apoioRepository.existsByDemandaIdAndUsuarioId(original.getId(), usuario.getId())) {
            return false;
        }
        Apoio apoio = new Apoio();
        apoio.setDemanda(original);
        apoio.setUsuario(usuario);
        apoio.setCriadoEm(LocalDateTime.now());
        apoioRepository.save(apoio);
        original.setTotalApoios(original.getTotalApoios() + 1);
        original.setUltimoApoioEm(apoio.getCriadoEm());
        prioridadeService.atualizar(original);
        demandaRepository.save(original);
        return true;
    }

    public boolean jaApoiou(Long demandaId, Long usuarioId) {
        return apoioRepository.existsByDemandaIdAndUsuarioId(demandaId, usuarioId);
    }

    public Set<Long> idsApoiados(Long usuarioId, Collection<Demanda> demandas) {
        List<Long> ids = demandas.stream().map(Demanda::getId).toList();
        return ids.isEmpty() ? Set.of() : apoioRepository.idsApoiados(usuarioId, ids);
    }
}
