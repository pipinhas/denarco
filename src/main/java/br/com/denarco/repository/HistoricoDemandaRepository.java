package br.com.denarco.repository;

import br.com.denarco.model.HistoricoDemanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoDemandaRepository extends JpaRepository<HistoricoDemanda, Long> {

    List<HistoricoDemanda> findByDemandaIdOrderByCriadoEmAscIdAsc(Long demandaId);
}
