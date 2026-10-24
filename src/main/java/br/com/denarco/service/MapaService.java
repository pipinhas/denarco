package br.com.denarco.service;

import br.com.denarco.dto.PontoMapa;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.repository.DemandaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** Pontos do mapa territorial. Mostra só dados públicos, para qualquer pessoa logada. */
@Service
public class MapaService {

    private final DemandaRepository demandaRepository;

    public MapaService(DemandaRepository demandaRepository) {
        this.demandaRepository = demandaRepository;
    }

    /**
     * @param situacao "abertas" (padrão), "concluidas" ou "todas"
     * @param dias     só demandas criadas nos últimos N dias; nulo = qualquer data
     */
    public List<PontoMapa> pontos(String situacao, Long categoriaId, Integer dias) {
        List<StatusDemanda> status = switch (situacao == null ? "" : situacao) {
            case "concluidas" -> List.of(StatusDemanda.CONCLUIDA);
            case "todas" -> List.of(StatusDemanda.values());
            default -> StatusDemanda.emAberto();
        };
        LocalDateTime desde = dias == null ? null : LocalDateTime.now().minusDays(dias);

        return demandaRepository.buscarParaMapa(status, categoriaId, desde).stream()
                .filter(d -> AreaAtendimento.contem(d.getLatitude(), d.getLongitude()))
                .map(d -> new PontoMapa(d.getId(), d.getProtocolo(), d.getCategoria().getNome(),
                        d.getStatus().getDescricao(), d.getPontuacao(), d.getFaixaPrioridade(),
                        d.getLatitude(), d.getLongitude(), d.getLocalResumido(), d.getTotalApoios(), d.isAtrasada()))
                .toList();
    }
}
