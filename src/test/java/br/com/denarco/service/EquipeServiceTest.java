package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.AlteracaoPerfilRepository;
import br.com.denarco.repository.ConviteRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regras de "quem pode promover quem" (seção 3 do plano). */
class EquipeServiceTest {

    private final Setor iluminacao = setor(1L, "Iluminação pública");
    private final Setor drenagem = setor(2L, "Drenagem");

    private final Usuario admin = usuario(1L, Perfil.ADMIN, null);
    private final Usuario outroAdmin = usuario(2L, Perfil.ADMIN, null);
    private final Usuario gestorIluminacao = usuario(3L, Perfil.GESTOR, iluminacao);
    private final Usuario servidorIluminacao = usuario(4L, Perfil.SERVIDOR, iluminacao);
    private final Usuario servidorDrenagem = usuario(5L, Perfil.SERVIDOR, drenagem);
    private final Usuario cidadao = usuario(6L, Perfil.CIDADAO, null);

    @Test
    void cadaUmSoDaPerfisAbaixoDoSeu() {
        assertTrue(EquipeService.podeConceder(gestorIluminacao, Perfil.SERVIDOR));
        assertFalse(EquipeService.podeConceder(gestorIluminacao, Perfil.GESTOR));
        assertFalse(EquipeService.podeConceder(gestorIluminacao, Perfil.ADMIN));
        assertTrue(EquipeService.podeConceder(admin, Perfil.ADMIN));
    }

    @Test
    void ninguemMexeNoProprioPerfilNemEmQuemEstaNoMesmoNivel() {
        assertFalse(EquipeService.podeGerenciar(admin, admin));
        assertFalse(EquipeService.podeGerenciar(admin, outroAdmin));
        assertFalse(EquipeService.podeGerenciar(servidorIluminacao, cidadao));
    }

    @Test
    void gestorSoCuidaDoProprioSetorEDeCidadaos() {
        assertTrue(EquipeService.podeGerenciar(gestorIluminacao, servidorIluminacao));
        assertTrue(EquipeService.podeGerenciar(gestorIluminacao, cidadao));
        assertFalse(EquipeService.podeGerenciar(gestorIluminacao, servidorDrenagem));
        assertTrue(EquipeService.podeGerenciar(admin, servidorDrenagem));
    }

    @Test
    void gestorPromoveCidadaoSempreParaOProprioSetor() {
        EquipeService equipeService = criarService(cidadao);

        // Mesmo pedindo Drenagem, o gestor de Iluminação só consegue colocar a pessoa no setor dele.
        equipeService.alterarPerfil(cidadao.getId(), Perfil.SERVIDOR, drenagem.getId(), gestorIluminacao);

        assertEquals(Perfil.SERVIDOR, cidadao.getPerfil());
        assertEquals(iluminacao, cidadao.getSetor());
    }

    @Test
    void gestorNaoPromoveNinguemAGestor() {
        EquipeService equipeService = criarService(cidadao);

        assertThrows(NegocioException.class,
                () -> equipeService.alterarPerfil(cidadao.getId(), Perfil.GESTOR, null, gestorIluminacao));
    }

    private EquipeService criarService(Usuario alvo) {
        UsuarioService usuarioService = mock(UsuarioService.class);
        when(usuarioService.buscar(alvo.getId())).thenReturn(alvo);
        return new EquipeService(mock(UsuarioRepository.class), mock(ConviteRepository.class),
                mock(AlteracaoPerfilRepository.class), usuarioService, mock(SetorService.class));
    }

    private static Usuario usuario(Long id, Perfil perfil, Setor setor) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
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
}
