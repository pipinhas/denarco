package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Alcance;
import br.com.denarco.model.Apoio;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.model.Urgencia;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.ApoioRepository;
import br.com.denarco.repository.DemandaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApoioServiceTest {

    private static final Long AUTOR = 1L;
    private static final Long VIZINHO = 2L;

    private ApoioRepository apoioRepository;
    private DemandaService demandaService;
    private ApoioService apoioService;
    private Demanda demanda;

    @BeforeEach
    void preparar() {
        apoioRepository = mock(ApoioRepository.class);
        demandaService = mock(DemandaService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        apoioService = new ApoioService(apoioRepository, mock(DemandaRepository.class), demandaService,
                usuarioService, new PrioridadeService());

        demanda = new Demanda();
        demanda.setAutor(usuario(AUTOR));
        demanda.setStatus(StatusDemanda.ABERTA);
        demanda.setUrgencia(Urgencia.MEDIA);
        demanda.setAlcance(Alcance.RUA);
        demanda.setCriadaEm(LocalDateTime.now());
        when(demandaService.buscar(10L)).thenReturn(demanda);
        when(usuarioService.buscar(VIZINHO)).thenReturn(usuario(VIZINHO));
    }

    @Test
    void primeiroCliqueApoiaESobeAPrioridade() {
        int antes = new PrioridadeService().calcular(demanda);
        when(apoioRepository.findByDemandaIdAndUsuarioId(10L, VIZINHO)).thenReturn(Optional.empty());

        assertTrue(apoioService.alternar(10L, VIZINHO));
        assertEquals(1, demanda.getTotalApoios());
        assertEquals(antes + 2, demanda.getPontuacao());
    }

    @Test
    void segundoCliqueDesfazOApoio() {
        demanda.setTotalApoios(1);
        when(apoioRepository.findByDemandaIdAndUsuarioId(10L, VIZINHO)).thenReturn(Optional.of(new Apoio()));

        assertFalse(apoioService.alternar(10L, VIZINHO));
        assertEquals(0, demanda.getTotalApoios());
    }

    @Test
    void autorNaoApoiaAPropriaDemanda() {
        assertThrows(NegocioException.class, () -> apoioService.alternar(10L, AUTOR));
    }

    @Test
    void demandaConcluidaNaoRecebeApoio() {
        demanda.setStatus(StatusDemanda.CONCLUIDA);

        assertThrows(NegocioException.class, () -> apoioService.alternar(10L, VIZINHO));
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
