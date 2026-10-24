package br.com.denarco.service;

import br.com.denarco.model.Demanda;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemandaServiceTest {

    @Test
    void distanciaEntreDoisPontosProximosEmArcoverde() {
        // 0,0009 grau de latitude são cerca de 100 metros.
        double metros = DemandaService.distanciaEmMetros(-8.4189, -37.0539, -8.4180, -37.0539);

        assertEquals(100, metros, 1.0);
    }

    @Test
    void protocoloUsaAnoEIdComSeisDigitos() {
        Demanda demanda = new Demanda();
        ReflectionTestUtils.setField(demanda, "id", 123L);
        demanda.setCriadaEm(LocalDateTime.of(2026, 10, 24, 10, 0));

        assertEquals("ARC-2026-000123", DemandaService.gerarProtocolo(demanda));
    }
}
