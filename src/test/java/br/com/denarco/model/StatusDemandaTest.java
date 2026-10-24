package br.com.denarco.model;

import org.junit.jupiter.api.Test;

import static br.com.denarco.model.StatusDemanda.ABERTA;
import static br.com.denarco.model.StatusDemanda.CANCELADA;
import static br.com.denarco.model.StatusDemanda.CONCLUIDA;
import static br.com.denarco.model.StatusDemanda.EM_ANDAMENTO;
import static br.com.denarco.model.StatusDemanda.ENCAMINHADA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Confere o diagrama de status da seção 4.2 do plano. */
class StatusDemandaTest {

    @Test
    void caminhoNormalDaDemanda() {
        assertTrue(ABERTA.podeMudarPara(ENCAMINHADA));
        assertTrue(ENCAMINHADA.podeMudarPara(EM_ANDAMENTO));
        assertTrue(EM_ANDAMENTO.podeMudarPara(CONCLUIDA));
    }

    @Test
    void atalhosPermitidos() {
        assertTrue(ENCAMINHADA.podeMudarPara(CONCLUIDA));
        assertTrue(ENCAMINHADA.podeMudarPara(ABERTA));
        assertTrue(CONCLUIDA.podeMudarPara(ABERTA));
    }

    @Test
    void naoPulaEtapasNemVoltaDeCancelada() {
        assertFalse(ABERTA.podeMudarPara(CONCLUIDA));
        assertFalse(ABERTA.podeMudarPara(EM_ANDAMENTO));
        assertFalse(EM_ANDAMENTO.podeMudarPara(CANCELADA));
        for (StatusDemanda status : StatusDemanda.values()) {
            assertFalse(CANCELADA.podeMudarPara(status));
        }
    }
}
