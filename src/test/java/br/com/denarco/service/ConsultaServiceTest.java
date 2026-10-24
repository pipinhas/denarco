package br.com.denarco.service;

import br.com.denarco.dto.ResultadoConsulta;
import br.com.denarco.model.Categoria;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.DemandaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A frase vira filtros que a pessoa consegue conferir. */
class ConsultaServiceTest {

    private final Setor iluminacao = setor(1L, "Iluminação pública");
    private final Setor drenagem = setor(2L, "Drenagem");
    private ConsultaService consultaService;

    @BeforeEach
    void preparar() {
        DemandaRepository demandaRepository = mock(DemandaRepository.class);
        when(demandaRepository.bairrosConhecidos()).thenReturn(List.of("Centro", "São Geraldo"));
        when(demandaRepository.buscarConsulta(any(), any(), anyBoolean(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(Page.empty());

        CategoriaService categoriaService = mock(CategoriaService.class);
        when(categoriaService.listarTodas()).thenReturn(List.of(categoria(1L, "Buraco na rua"), categoria(2L, "Poste apagado")));
        SetorService setorService = mock(SetorService.class);
        when(setorService.listarTodos()).thenReturn(List.of(iluminacao, drenagem));

        consultaService = new ConsultaService(demandaRepository, categoriaService, setorService);
    }

    @Test
    void entendeSetorSituacaoEBairroSemSeImportarComAcentos() {
        ResultadoConsulta resultado = consultaService.consultar("iluminacao ATRASADA no centro", admin());

        assertEquals(List.of("Setor: Iluminação pública", "Situação: atrasadas", "Bairro: Centro"), resultado.entendido());
    }

    @Test
    void entendeTipoDeProblemaPeloPluralEPeriodo() {
        ResultadoConsulta resultado = consultaService.consultar("buracos abertos este mês em São Geraldo", admin());

        assertTrue(resultado.entendido().contains("Tipo: Buraco na rua"));
        assertTrue(resultado.entendido().contains("Situação: em aberto"));
        assertTrue(resultado.entendido().contains("Período: este mês"));
        assertTrue(resultado.entendido().contains("Bairro: São Geraldo"));
    }

    @Test
    void servidorNaoConsultaOutroSetor() {
        Usuario servidor = usuario(Perfil.SERVIDOR, iluminacao);

        ResultadoConsulta resultado = consultaService.consultar("drenagem atrasada", servidor);

        assertEquals(List.of("Você só consulta o setor Iluminação pública."), resultado.avisos());
    }

    @Test
    void normalizarTiraAcentosEPontuacao() {
        assertEquals("iluminacao publica", ConsultaService.normalizar("  Iluminação   Pública! "));
    }

    private Usuario admin() {
        return usuario(Perfil.ADMIN, null);
    }

    private static Usuario usuario(Perfil perfil, Setor setor) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setPerfil(perfil);
        usuario.setSetor(setor);
        return usuario;
    }

    private static Setor setor(Long id, String nome) {
        Setor setor = new Setor();
        ReflectionTestUtils.setField(setor, "id", id);
        setor.setNome(nome);
        return setor;
    }

    private static Categoria categoria(Long id, String nome) {
        Categoria categoria = new Categoria();
        ReflectionTestUtils.setField(categoria, "id", id);
        categoria.setNome(nome);
        return categoria;
    }
}
