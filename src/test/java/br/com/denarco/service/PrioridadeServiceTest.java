package br.com.denarco.service;

import br.com.denarco.model.Alcance;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.Urgencia;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrioridadeServiceTest {

    private final PrioridadeService prioridadeService = new PrioridadeService();

    @Test
    void somaUrgenciaAlcanceApoiosEDias() {
        // Alta (35) + bairro (20) + 6 apoios (12) + 5 dias (5) = 72, o exemplo do plano.
        Demanda demanda = demanda(Urgencia.ALTA, Alcance.BAIRRO, 6, 5);

        assertEquals(72, prioridadeService.calcular(demanda));
        assertEquals("Alta", faixa(demanda));
    }

    @Test
    void apoiosEDiasTemLimite() {
        // Média (20) + local (5) + apoios no máximo 20 + dias no máximo 10 = 55.
        Demanda demanda = demanda(Urgencia.MEDIA, Alcance.LOCAL, 50, 90);

        assertEquals(55, prioridadeService.calcular(demanda));
    }

    @Test
    void riscoASegurancaVale50MesmoComUrgenciaBaixa() {
        Demanda demanda = demanda(Urgencia.BAIXA, Alcance.LOCAL, 0, 0);
        demanda.setRiscoSeguranca(true);

        assertEquals(55, prioridadeService.calcular(demanda));
    }

    @Test
    void prioridadeFixadaPorGestorNaoEhRecalculada() {
        Demanda demanda = demanda(Urgencia.BAIXA, Alcance.LOCAL, 0, 0);
        demanda.setPontuacao(90);
        demanda.setPontuacaoFixada(true);

        prioridadeService.atualizar(demanda);

        assertEquals(90, demanda.getPontuacao());
    }

    private String faixa(Demanda demanda) {
        prioridadeService.atualizar(demanda);
        return demanda.getFaixaPrioridade();
    }

    private Demanda demanda(Urgencia urgencia, Alcance alcance, int apoios, int diasAtras) {
        Demanda demanda = new Demanda();
        demanda.setUrgencia(urgencia);
        demanda.setAlcance(alcance);
        demanda.setTotalApoios(apoios);
        demanda.setCriadaEm(LocalDateTime.now().minusDays(diasAtras));
        return demanda;
    }
}
