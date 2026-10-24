package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Alcance;
import br.com.denarco.model.Canal;
import br.com.denarco.model.Categoria;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.model.Urgencia;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.DemandaRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtendimentoServiceTest {

    private final Setor iluminacao = setor(1L);
    private final Setor drenagem = setor(2L);
    private final Usuario autor = usuario(10L, Perfil.CIDADAO, null);
    private final Usuario servidorIluminacao = usuario(11L, Perfil.SERVIDOR, iluminacao);
    private final Usuario servidorDrenagem = usuario(12L, Perfil.SERVIDOR, drenagem);
    private final Usuario gestorIluminacao = usuario(13L, Perfil.GESTOR, iluminacao);

    private Demanda demanda;
    private EmailService emailService;
    private AtendimentoService atendimentoService;

    @BeforeEach
    void preparar() {
        Categoria categoria = new Categoria();
        categoria.setNome("Poste apagado");
        demanda = new Demanda();
        ReflectionTestUtils.setField(demanda, "id", 100L);
        demanda.setCategoria(categoria);
        demanda.setSetor(iluminacao);
        demanda.setAutor(autor);
        demanda.setStatus(StatusDemanda.ABERTA);
        demanda.setUrgencia(Urgencia.MEDIA);
        demanda.setAlcance(Alcance.RUA);
        demanda.setCanal(Canal.APP);
        demanda.setCriadaEm(LocalDateTime.now().minusDays(2));

        DemandaService demandaService = mock(DemandaService.class);
        when(demandaService.buscar(100L)).thenReturn(demanda);
        UsuarioService usuarioService = mock(UsuarioService.class);
        when(usuarioService.buscar(11L)).thenReturn(servidorIluminacao);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        when(usuarioRepository.findBySetorIdAndPerfilInAndStatusContaOrderByNomeAsc(eq(1L), any(), any()))
                .thenReturn(List.of(servidorIluminacao, gestorIluminacao));
        emailService = mock(EmailService.class);

        atendimentoService = new AtendimentoService(mock(DemandaRepository.class), usuarioRepository, demandaService,
                usuarioService, mock(SetorService.class), mock(HistoricoService.class), new PrioridadeService(),
                mock(ApoioService.class), mock(FotoService.class), emailService, "http://localhost");
    }

    @Test
    void soAEquipeDoSetorAtende() {
        assertTrue(atendimentoService.podeAtender(demanda, servidorIluminacao));
        assertFalse(atendimentoService.podeAtender(demanda, servidorDrenagem));
        assertFalse(atendimentoService.podeAtender(demanda, autor));
        assertFalse(atendimentoService.podeGerir(demanda, servidorIluminacao));
        assertTrue(atendimentoService.podeGerir(demanda, gestorIluminacao));
    }

    @Test
    void servidorDeOutroSetorRecebeAcessoNegado() {
        assertThrows(AccessDeniedException.class, () -> atendimentoService.iniciar(100L, servidorDrenagem));
    }

    @Test
    void encaminharDefineResponsavelPrazoEStatus() {
        LocalDate prazo = LocalDate.now().plusDays(7);

        atendimentoService.encaminhar(100L, 11L, prazo, gestorIluminacao);

        assertEquals(StatusDemanda.ENCAMINHADA, demanda.getStatus());
        assertEquals(servidorIluminacao, demanda.getResponsavel());
        assertEquals(prazo, demanda.getPrazo());
    }

    @Test
    void prazoNoPassadoNaoEhAceito() {
        assertThrows(NegocioException.class,
                () -> atendimentoService.encaminhar(100L, 11L, LocalDate.now().minusDays(1), gestorIluminacao));
    }

    @Test
    void naoConcluiDemandaSemResponsavel() {
        assertThrows(NegocioException.class,
                () -> atendimentoService.concluir(100L, "feito", null, servidorIluminacao));
    }

    @Test
    void concluirFechaADemandaEAvisaQuemRegistrou() {
        demanda.setStatus(StatusDemanda.EM_ANDAMENTO);
        autor.setEmail("maria@exemplo.com");

        atendimentoService.concluir(100L, "Lâmpada trocada.", null, servidorIluminacao);

        assertEquals(StatusDemanda.CONCLUIDA, demanda.getStatus());
        assertNotNull(demanda.getConcluidaEm());
        verify(emailService).enviar(eq("maria@exemplo.com"), anyString(), anyString());
    }

    @Test
    void ajusteManualDePrioridadeExigeMotivo() {
        assertThrows(NegocioException.class, () -> atendimentoService.fixarPrioridade(100L, 90, " ", gestorIluminacao));

        atendimentoService.fixarPrioridade(100L, 90, "Escola na rua", gestorIluminacao);
        assertEquals(90, demanda.getPontuacao());
        assertTrue(demanda.isPontuacaoFixada());
    }

    private static Usuario usuario(Long id, Perfil perfil, Setor setor) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNome("Pessoa " + id);
        usuario.setPerfil(perfil);
        usuario.setSetor(setor);
        return usuario;
    }

    private static Setor setor(Long id) {
        Setor setor = new Setor();
        ReflectionTestUtils.setField(setor, "id", id);
        setor.setNome("Setor " + id);
        return setor;
    }
}
