package br.com.denarco.controller;

import br.com.denarco.dto.NovaDemandaForm;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.CategoriaRepository;
import br.com.denarco.repository.SetorRepository;
import br.com.denarco.repository.UsuarioRepository;
import br.com.denarco.security.UsuarioLogado;
import br.com.denarco.service.DemandaService;
import br.com.denarco.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cada perfil só entra onde pode (seção 3 do plano). Usa o sistema inteiro com banco em memória,
 * então também confere que as telas da equipe abrem sem erro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissoesRotasTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private DemandaService demandaService;

    @Test
    void quemNaoEntrouVaiParaOLogin() throws Exception {
        mvc.perform(get("/atendimento")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/gestao/equipe")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void cidadaoNaoEntraNasTelasDaEquipe() throws Exception {
        UsuarioLogado cidadao = logado("cidadao@teste.com", Perfil.CIDADAO, null);
        mvc.perform(get("/").with(user(cidadao))).andExpect(status().isOk());
        mvc.perform(get("/atendimento").with(user(cidadao))).andExpect(status().isForbidden());
        mvc.perform(get("/gestao/equipe").with(user(cidadao))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/configuracoes").with(user(cidadao))).andExpect(status().isForbidden());
        mvc.perform(get("/painel").with(user(cidadao))).andExpect(status().isForbidden());
        mvc.perform(get("/consulta").with(user(cidadao))).andExpect(status().isForbidden());
        // O mapa é público para quem está logado (só mostra dados públicos).
        mvc.perform(get("/mapa").with(user(cidadao))).andExpect(status().isOk());
        mvc.perform(get("/api/mapa").with(user(cidadao))).andExpect(status().isOk());
    }

    @Test
    void painelEConsultaAbremParaAEquipe() throws Exception {
        UsuarioLogado gestor = logado("gestor@teste.com", Perfil.GESTOR, "Iluminação pública");
        mvc.perform(get("/painel").with(user(gestor))).andExpect(status().isOk());
        mvc.perform(get("/api/painel").param("dias", "90").with(user(gestor))).andExpect(status().isOk());
        mvc.perform(get("/consulta").param("q", "poste atrasado no Centro").with(user(gestor))).andExpect(status().isOk());
        // Gestor e admin começam pelo painel; o servidor, pela fila.
        mvc.perform(get("/").with(user(gestor))).andExpect(redirectedUrl("/painel"));
        UsuarioLogado servidor = logado("servidor@teste.com", Perfil.SERVIDOR, "Iluminação pública");
        mvc.perform(get("/").with(user(servidor))).andExpect(redirectedUrl("/atendimento"));
    }

    @Test
    void servidorVeAFilaMasNaoAEquipe() throws Exception {
        UsuarioLogado servidor = logado("servidor@teste.com", Perfil.SERVIDOR, "Iluminação pública");
        mvc.perform(get("/atendimento").with(user(servidor))).andExpect(status().isOk());
        mvc.perform(get("/atendimento/registrar").with(user(servidor))).andExpect(status().isOk());
        mvc.perform(get("/gestao/equipe").with(user(servidor))).andExpect(status().isForbidden());
        mvc.perform(post("/gestao/demandas/1/cancelar").with(user(servidor)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void gestorVeAEquipeMasNaoAsConfiguracoes() throws Exception {
        UsuarioLogado gestor = logado("gestor@teste.com", Perfil.GESTOR, "Iluminação pública");
        mvc.perform(get("/atendimento").with(user(gestor))).andExpect(status().isOk());
        mvc.perform(get("/gestao/equipe").with(user(gestor))).andExpect(status().isOk());
        mvc.perform(get("/admin/configuracoes").with(user(gestor))).andExpect(status().isForbidden());
    }

    @Test
    void adminEntraEmTudo() throws Exception {
        UsuarioLogado admin = logado("admin@teste.com", Perfil.ADMIN, null);
        mvc.perform(get("/atendimento").with(user(admin))).andExpect(status().isOk());
        mvc.perform(get("/gestao/equipe").with(user(admin))).andExpect(status().isOk());
        mvc.perform(get("/admin/configuracoes").with(user(admin))).andExpect(status().isOk());
    }

    @Test
    void servidorNaoMexeEmDemandaDeOutroSetorMesmoPelaUrl() throws Exception {
        UsuarioLogado cidadao = logado("autora@teste.com", Perfil.CIDADAO, null);
        UsuarioLogado servidorDrenagem = logado("drenagem@teste.com", Perfil.SERVIDOR, "Drenagem");

        NovaDemandaForm form = new NovaDemandaForm();
        form.setCategoriaId(categoriaRepository.findByNome("Poste apagado").orElseThrow().getId());
        form.setDescricao("Poste apagado de teste");
        form.setEndereco("Rua de teste");
        Demanda demanda = demandaService.registrar(form, cidadao.getId());

        mvc.perform(get("/demandas/" + demanda.getProtocolo()).with(user(servidorDrenagem))).andExpect(status().isOk());
        mvc.perform(post("/atendimento/demandas/" + demanda.getId() + "/concluir").param("texto", "feito")
                .with(user(servidorDrenagem)).with(csrf())).andExpect(status().isForbidden());
    }

    private UsuarioLogado logado(String email, Perfil perfil, String nomeSetor) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Setor setor = nomeSetor == null ? null : setorRepository.findByNome(nomeSetor).orElseThrow();
            return usuarioService.criarContaAtiva("Teste " + perfil, email, "senha1234", "(87) 90000-0000", perfil, setor);
        });
        return new UsuarioLogado(usuario);
    }
}
