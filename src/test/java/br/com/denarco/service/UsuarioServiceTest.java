package br.com.denarco.service;

import br.com.denarco.dto.CadastroCorporativoForm;
import br.com.denarco.dto.CadastroForm;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Atuacao;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.StatusConta;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.AlteracaoPerfilRepository;
import br.com.denarco.repository.ConviteRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        usuarioService = new UsuarioService(usuarioRepository, mock(ConviteRepository.class),
                mock(AlteracaoPerfilRepository.class), mock(SetorService.class), new BCryptPasswordEncoder());
    }

    @Test
    void cidadaoNasceAtivoComSenhaCriptografadaEEmailNormalizado() {
        Usuario usuario = usuarioService.cadastrarCidadao(form(" Maria@Exemplo.COM "));

        assertEquals("maria@exemplo.com", usuario.getEmail());
        assertEquals(Perfil.CIDADAO, usuario.getPerfil());
        assertEquals(StatusConta.ATIVA, usuario.getStatusConta());
        assertNotEquals("senha1234", usuario.getSenhaHash());
        assertTrue(usuario.getSenhaHash().startsWith("$2a$"));
    }

    @Test
    void emailRepetidoNaoPodeCriarOutraConta() {
        when(usuarioRepository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThrows(NegocioException.class, () -> usuarioService.cadastrarCidadao(form("maria@exemplo.com")));
    }

    @Test
    void contaCorporativaNasceComoCidadaoPendente() {
        CadastroCorporativoForm form = new CadastroCorporativoForm();
        preencher(form, "joao@exemplo.com");
        form.setAtuacao(Atuacao.PLANEJAMENTO);

        Usuario usuario = usuarioService.cadastrarCorporativo(form);

        assertEquals(Perfil.CIDADAO, usuario.getPerfil());
        assertEquals(StatusConta.PENDENTE, usuario.getStatusConta());
        assertEquals(Atuacao.PLANEJAMENTO, usuario.getAtuacao());
    }

    @Test
    void contaCorporativaDeCampoPrecisaDeSetor() {
        CadastroCorporativoForm form = new CadastroCorporativoForm();
        preencher(form, "joao@exemplo.com");
        form.setAtuacao(Atuacao.EQUIPE_CAMPO);

        assertThrows(NegocioException.class, () -> usuarioService.cadastrarCorporativo(form));
    }

    private CadastroForm form(String email) {
        CadastroForm form = new CadastroForm();
        preencher(form, email);
        return form;
    }

    private void preencher(CadastroForm form, String email) {
        form.setNome("Maria Teste");
        form.setEmail(email);
        form.setSenha("senha1234");
        form.setTelefone("(87) 99999-0000");
        form.setAceitouPrivacidade(true);
    }
}
