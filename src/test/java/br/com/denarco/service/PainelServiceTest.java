package br.com.denarco.service;

import br.com.denarco.dto.Intervalo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PainelServiceTest {

    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 1, 8, 0);

    @Test
    void tempoMedioEmDiasComVirgula() {
        // Uma levou 2 dias e a outra 3: média de 2,5 dias.
        List<Intervalo> concluidas = List.of(new Intervalo(inicio, inicio.plusDays(2)), new Intervalo(inicio, inicio.plusDays(3)));

        assertEquals("2,5 dias", PainelService.tempoMedio(concluidas));
    }

    @Test
    void tempoMedioCurtoEmHoras() {
        assertEquals("6 horas", PainelService.tempoMedio(List.of(new Intervalo(inicio, inicio.plusHours(6)))));
    }

    @Test
    void semConcluidasMostraTraco() {
        assertEquals("—", PainelService.tempoMedio(List.of()));
    }

    @Test
    void periodoForaDaListaVoltaParaTrintaDias() {
        assertEquals(7, PainelService.periodoValido(7));
        assertEquals(30, PainelService.periodoValido(45));
        assertEquals(30, PainelService.periodoValido(null));
    }
}
