package br.com.denarco.config;

import br.com.denarco.service.DemandaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Tarefas que rodam sozinhas no servidor, em horários fixos. */
@Configuration
@EnableScheduling
public class TarefasAgendadas {

    private static final Logger log = LoggerFactory.getLogger(TarefasAgendadas.class);

    private final DemandaService demandaService;

    public TarefasAgendadas(DemandaService demandaService) {
        this.demandaService = demandaService;
    }

    // Todo dia às 3h: os "dias em aberto" mudam e com eles a prioridade.
    @Scheduled(cron = "0 0 3 * * *")
    public void recalcularPrioridades() {
        int total = demandaService.recalcularPrioridades();
        log.info("Prioridades recalculadas: {} demandas em aberto.", total);
    }
}
