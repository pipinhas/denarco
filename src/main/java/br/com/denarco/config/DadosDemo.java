package br.com.denarco.config;

import br.com.denarco.dto.CadastroCorporativoForm;
import br.com.denarco.model.Alcance;
import br.com.denarco.model.Apoio;
import br.com.denarco.model.Atuacao;
import br.com.denarco.model.Canal;
import br.com.denarco.model.Categoria;
import br.com.denarco.model.Convite;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.HistoricoDemanda;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.model.TipoHistorico;
import br.com.denarco.model.Urgencia;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.ApoioRepository;
import br.com.denarco.repository.CategoriaRepository;
import br.com.denarco.repository.ConviteRepository;
import br.com.denarco.repository.DemandaRepository;
import br.com.denarco.repository.HistoricoDemandaRepository;
import br.com.denarco.repository.SetorRepository;
import br.com.denarco.repository.UsuarioRepository;
import br.com.denarco.service.DemandaService;
import br.com.denarco.service.PrioridadeService;
import br.com.denarco.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static br.com.denarco.model.StatusDemanda.ABERTA;
import static br.com.denarco.model.StatusDemanda.CONCLUIDA;
import static br.com.denarco.model.StatusDemanda.EM_ANDAMENTO;
import static br.com.denarco.model.StatusDemanda.ENCAMINHADA;

/**
 * Dados fictícios para testes e apresentação. Só roda no perfil dev,
 * como pede o caderno de contexto (nada de dados reais em demonstrações).
 * Todas as contas usam a senha "demo1234". Pessoas, endereços e problemas são inventados.
 */
@Component
@Profile("dev")
@Order(2)
public class DadosDemo implements CommandLineRunner {

    private static final String SENHA = "demo1234";

    /** Uma demanda de exemplo. Latitude e longitude são deslocamentos a partir do centro de Arcoverde. */
    private record Exemplo(String categoria, String descricao, double dLat, double dLng, String endereco, String bairro,
                           StatusDemanda status, int apoios, int diasAtras, Urgencia urgencia, Alcance alcance,
                           boolean risco, String autor, Canal canal, String solicitante) {

        // A maioria das demandas de exemplo veio pelo site.
        Exemplo(String categoria, String descricao, double dLat, double dLng, String endereco, String bairro,
                StatusDemanda status, int apoios, int diasAtras, Urgencia urgencia, Alcance alcance,
                boolean risco, String autor) {
            this(categoria, descricao, dLat, dLng, endereco, bairro, status, apoios, diasAtras, urgencia, alcance,
                    risco, autor, Canal.APP, null);
        }
    }

    private static final double LAT_CENTRO = -8.4189;
    private static final double LNG_CENTRO = -37.0539;

    private static final List<Exemplo> EXEMPLOS = List.of(
            new Exemplo("Poste apagado", "O poste em frente à escola está apagado há uma semana. A rua fica muito escura à noite.",
                    0.0012, -0.0015, "Rua Sete de Setembro, 210", "Centro", ENCAMINHADA, 9, 6, Urgencia.MEDIA, Alcance.RUA, false, "cidadao@demo.com"),
            new Exemplo("Lixo acumulado", "Lixo acumulado na esquina há vários dias, com mau cheiro e muitos insetos.",
                    -0.0041, 0.0052, "Travessa da Feira", "Boa Vista", ABERTA, 6, 3, Urgencia.MEDIA, Alcance.RUA, false, "cidadao@demo.com"),
            new Exemplo("Calçamento danificado", "Pedras soltas no calçamento, já vi duas pessoas tropeçarem.",
                    0.0068, 0.0031, "Rua Nova, 300", "São Cristóvão", ABERTA, 1, 1, Urgencia.MEDIA, Alcance.RUA, false, "cidadao@demo.com"),
            new Exemplo("Praça ou equipamento danificado", "Bancos da praça quebrados e brinquedos enferrujados.",
                    -0.0005, 0.0008, "Praça Central", "Centro", CONCLUIDA, 5, 25, Urgencia.BAIXA, Alcance.BAIRRO, false, "cidadao@demo.com"),
            new Exemplo("Buraco na rua", "Buraco grande no meio da rua, os carros desviam pela contramão.",
                    0.0075, 0.0044, "Rua do Sol, 45", "São Cristóvão", ABERTA, 12, 12, Urgencia.ALTA, Alcance.RUA, false, "morador1@demo.com"),
            new Exemplo("Fiação exposta", "Fio solto pendurado perto do chão, ao lado do ponto de ônibus.",
                    0.0009, 0.0021, "Praça da Bandeira", "Centro", EM_ANDAMENTO, 4, 2, Urgencia.ALTA, Alcance.LOCAL, true, "morador2@demo.com"),
            new Exemplo("Bueiro entupido", "Bueiro entupido com lixo, a água fica parada na frente das casas.",
                    -0.0062, -0.0048, "Rua São José, 88", "Sucupira", ABERTA, 3, 9, Urgencia.MEDIA, Alcance.RUA, false, "morador3@demo.com"),
            new Exemplo("Alagamento", "Quando chove a avenida alaga e a água entra nas casas.",
                    -0.0093, 0.0011, "Avenida Brasil", "São Geraldo", ENCAMINHADA, 11, 15, Urgencia.ALTA, Alcance.BAIRRO, false, "morador4@demo.com"),
            new Exemplo("Mato alto", "Terreno baldio com mato alto e muitos mosquitos.",
                    -0.0035, 0.0071, "Rua das Acácias", "Boa Vista", ABERTA, 2, 20, Urgencia.BAIXA, Alcance.LOCAL, false, "morador5@demo.com"),
            new Exemplo("Poste apagado", "Dois postes seguidos apagados na rua da igreja.",
                    -0.0057, -0.0039, "Rua da Matriz, 12", "Sucupira", CONCLUIDA, 3, 18, Urgencia.MEDIA, Alcance.RUA, false, "morador6@demo.com"),
            new Exemplo("Entulho na via", "Entulho de obra jogado na calçada, pedestres andam pela rua.",
                    -0.0088, -0.0021, "Rua do Comércio", "São Geraldo", ABERTA, 0, 4, Urgencia.MEDIA, Alcance.RUA, false, "morador7@demo.com"),
            new Exemplo("Lixo acumulado", "A coleta não passou esta semana, os sacos estão rasgados na calçada.",
                    0.0021, -0.0004, "Rua do Mercado", "Centro", EM_ANDAMENTO, 7, 7, Urgencia.MEDIA, Alcance.RUA, false, "morador8@demo.com"),
            new Exemplo("Buraco na rua", "Buraco aumentando depois das chuvas, perto da escola.",
                    -0.0039, 0.0059, "Avenida Brasil, 900", "Boa Vista", ABERTA, 5, 28, Urgencia.MEDIA, Alcance.RUA, false, "morador9@demo.com"),
            new Exemplo("Outro problema", "Semáforo piscando no amarelo o dia todo no cruzamento.",
                    0.0004, -0.0026, "Cruzamento da Rua Sete de Setembro", "Centro", ABERTA, 4, 2, Urgencia.ALTA, Alcance.BAIRRO, false, "morador10@demo.com"),
            new Exemplo("Poste apagado", "Moradora ligou: rua toda escura desde o temporal de ontem.",
                    -0.0071, 0.0036, "Rua Projetada, perto da caixa d'água", "Boa Vista", ABERTA, 0, 0, Urgencia.ALTA, Alcance.RUA, false,
                    "servidor@demo.com", Canal.TELEFONE, "Dona Severina")
    );

    private static final String[] MORADORES = {"José Almeida", "Lúcia Ferreira", "Antônio Barros", "Francisca Lima",
            "Severino Rocha", "Joana Melo", "Raimundo Cavalcanti", "Cícera Gomes", "Manoel Pereira", "Teresa Araújo",
            "Edson Nunes", "Fátima Vieira"};

    private final UsuarioService usuarioService;
    private final PrioridadeService prioridadeService;
    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;
    private final DemandaRepository demandaRepository;
    private final HistoricoDemandaRepository historicoRepository;
    private final ApoioRepository apoioRepository;
    private final ConviteRepository conviteRepository;

    public DadosDemo(UsuarioService usuarioService, PrioridadeService prioridadeService,
                     UsuarioRepository usuarioRepository, SetorRepository setorRepository,
                     CategoriaRepository categoriaRepository, DemandaRepository demandaRepository,
                     HistoricoDemandaRepository historicoRepository, ApoioRepository apoioRepository,
                     ConviteRepository conviteRepository) {
        this.conviteRepository = conviteRepository;
        this.usuarioService = usuarioService;
        this.prioridadeService = prioridadeService;
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.categoriaRepository = categoriaRepository;
        this.demandaRepository = demandaRepository;
        this.historicoRepository = historicoRepository;
        this.apoioRepository = apoioRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        criarContasPrincipais();
        criarEquipesEMoradores();
        if (demandaRepository.count() == 0) {
            criarDemandas();
        }
        if (conviteRepository.count() == 0) {
            criarConvite();
        }
    }

    // Convite pendente: quem criar conta corporativa com convidado@demo.com entra direto como servidor da Limpeza.
    private void criarConvite() {
        Convite convite = new Convite();
        convite.setEmail("convidado@demo.com");
        convite.setPerfil(Perfil.SERVIDOR);
        convite.setSetor(setor("Limpeza urbana"));
        convite.setCriadoPor(usuario("admin@demo.com"));
        convite.setCriadoEm(LocalDateTime.now().minusDays(1));
        conviteRepository.save(convite);
    }

    private void criarContasPrincipais() {
        if (usuarioService.existeEmail("cidadao@demo.com")) {
            return;
        }
        Setor iluminacao = setor("Iluminação pública");
        usuarioService.criarContaAtiva("Maria Cidadã", "cidadao@demo.com", SENHA, "(87) 90000-0001", Perfil.CIDADAO, null);
        usuarioService.criarContaAtiva("João Servidor", "servidor@demo.com", SENHA, "(87) 90000-0002", Perfil.SERVIDOR, iluminacao);
        usuarioService.criarContaAtiva("Ana Gestora", "gestor@demo.com", SENHA, "(87) 90000-0003", Perfil.GESTOR, iluminacao);

        CadastroCorporativoForm pendente = new CadastroCorporativoForm();
        pendente.setNome("Pedro Pendente");
        pendente.setEmail("pendente@demo.com");
        pendente.setSenha(SENHA);
        pendente.setTelefone("(87) 90000-0004");
        pendente.setAtuacao(Atuacao.EQUIPE_CAMPO);
        pendente.setSetorId(setor("Drenagem").getId());
        usuarioService.cadastrarCorporativo(pendente);
    }

    // Um servidor por setor (para serem responsáveis) e moradores fictícios (para os apoios).
    private void criarEquipesEMoradores() {
        if (usuarioService.existeEmail("morador1@demo.com")) {
            return;
        }
        usuarioService.criarContaAtiva("Carlos Lima", "pavimentacao@demo.com", SENHA, "(87) 90000-0011", Perfil.SERVIDOR, setor("Pavimentação"));
        usuarioService.criarContaAtiva("Rita Souza", "limpeza@demo.com", SENHA, "(87) 90000-0012", Perfil.SERVIDOR, setor("Limpeza urbana"));
        usuarioService.criarContaAtiva("Paulo Santos", "drenagem@demo.com", SENHA, "(87) 90000-0013", Perfil.SERVIDOR, setor("Drenagem"));
        usuarioService.criarContaAtiva("Bruno Costa", "conservacao@demo.com", SENHA, "(87) 90000-0014", Perfil.SERVIDOR, setor("Conservação de espaços públicos"));
        for (int i = 0; i < MORADORES.length; i++) {
            usuarioService.criarContaAtiva(MORADORES[i], "morador" + (i + 1) + "@demo.com", SENHA,
                    "(87) 91000-00" + String.format("%02d", i + 1), Perfil.CIDADAO, null);
        }
    }

    private void criarDemandas() {
        Usuario admin = usuario("admin@demo.com");
        Usuario gestora = usuario("gestor@demo.com");
        List<Usuario> moradores = new ArrayList<>();
        for (int i = 1; i <= MORADORES.length; i++) {
            moradores.add(usuario("morador" + i + "@demo.com"));
        }

        for (Exemplo exemplo : EXEMPLOS) {
            Categoria categoria = categoriaRepository.findByNome(exemplo.categoria()).orElseThrow();
            Usuario autor = usuario(exemplo.autor());
            LocalDateTime criada = LocalDateTime.now().minusDays(exemplo.diasAtras()).withHour(8).withMinute(30);

            Demanda d = new Demanda();
            d.setCategoria(categoria);
            d.setSetor(categoria.getSetorPadrao());
            d.setAutor(autor);
            d.setDescricao(exemplo.descricao());
            d.setStatus(exemplo.status());
            d.setUrgencia(exemplo.urgencia());
            d.setAlcance(exemplo.alcance());
            d.setRiscoSeguranca(exemplo.risco());
            d.setCanal(exemplo.canal());
            d.setSolicitanteNome(exemplo.solicitante());
            d.setLatitude(LAT_CENTRO + exemplo.dLat());
            d.setLongitude(LNG_CENTRO + exemplo.dLng());
            d.setEndereco(exemplo.endereco());
            d.setBairro(exemplo.bairro());
            d.setCriadaEm(criada);
            d.setAtualizadaEm(criada);
            demandaRepository.save(d);
            d.setProtocolo(DemandaService.gerarProtocolo(d));

            String destino = d.getSetor() != null ? "Direcionada ao setor " + d.getSetor().getNome() + "." : "Aguardando triagem para definir o setor.";
            String origem = exemplo.canal() == Canal.APP ? "Demanda registrada pelo site."
                    : "Demanda recebida por " + exemplo.canal().getDescricao().toLowerCase() + " e registrada por " + autor.getNome() + ".";
            historico(d, autor, TipoHistorico.CRIACAO, origem + " " + destino, criada);

            if (exemplo.status() != ABERTA) {
                Usuario responsavel = servidorDoSetor(d.getSetor());
                Usuario quemEncaminhou = d.getSetor().getNome().equals("Iluminação pública") ? gestora : admin;
                d.setResponsavel(responsavel);
                d.setPrazo(criada.toLocalDate().plusDays(categoria.getPrazoPadraoDias()));
                historico(d, quemEncaminhou, TipoHistorico.ENCAMINHAMENTO, "Encaminhada para " + responsavel.getNome()
                        + ". Prazo: " + d.getPrazo().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".", criada.plusHours(5));
            }
            if (exemplo.status() == EM_ANDAMENTO || exemplo.status() == CONCLUIDA) {
                historico(d, d.getResponsavel(), TipoHistorico.STATUS, "Atendimento iniciado: equipe a caminho do local.", criada.plusDays(1));
            }
            if (exemplo.status() == CONCLUIDA) {
                LocalDateTime concluida = criada.plusDays(3);
                historico(d, d.getResponsavel(), TipoHistorico.PROVIDENCIA, "Serviço realizado no local pela equipe.", concluida.minusHours(2));
                historico(d, d.getResponsavel(), TipoHistorico.CONCLUSAO, "Demanda concluída.", concluida);
                d.setConcluidaEm(concluida);
            }

            // Apoios de moradores diferentes, espalhados entre a criação e hoje.
            for (int i = 0; i < exemplo.apoios() && i < moradores.size(); i++) {
                Usuario morador = moradores.get(i);
                if (morador.getId().equals(autor.getId())) {
                    continue;
                }
                LocalDateTime quando = criada.plusHours(6L + (long) i * Math.max(1, exemplo.diasAtras()) * 2);
                LocalDateTime limite = d.getConcluidaEm() != null ? d.getConcluidaEm().minusHours(1) : LocalDateTime.now();
                Apoio apoio = new Apoio();
                apoio.setDemanda(d);
                apoio.setUsuario(morador);
                apoio.setCriadoEm(quando.isAfter(limite) ? limite : quando);
                apoioRepository.save(apoio);
                d.setTotalApoios(d.getTotalApoios() + 1);
                d.setUltimoApoioEm(apoio.getCriadoEm());
            }

            prioridadeService.atualizar(d);
            demandaRepository.save(d);
        }
    }

    private void historico(Demanda demanda, Usuario usuario, TipoHistorico tipo, String descricao, LocalDateTime quando) {
        HistoricoDemanda passo = new HistoricoDemanda();
        passo.setDemanda(demanda);
        passo.setUsuario(usuario);
        passo.setTipo(tipo);
        passo.setDescricao(descricao);
        passo.setCriadoEm(quando);
        historicoRepository.save(passo);
    }

    private Usuario servidorDoSetor(Setor setor) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getPerfil() == Perfil.SERVIDOR && u.getSetor() != null && u.getSetor().getId().equals(setor.getId()))
                .findFirst()
                .orElseThrow();
    }

    private Setor setor(String nome) {
        return setorRepository.findByNome(nome).orElseThrow();
    }

    private Usuario usuario(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow();
    }
}
