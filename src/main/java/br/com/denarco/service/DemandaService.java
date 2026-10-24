package br.com.denarco.service;

import br.com.denarco.dto.DemandaProxima;
import br.com.denarco.dto.NovaDemandaForm;
import br.com.denarco.dto.ResumoCidadao;
import br.com.denarco.exception.NaoEncontradoException;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Canal;
import br.com.denarco.model.Categoria;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.model.TipoHistorico;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.DemandaRepository;
import br.com.denarco.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Regras para registrar, consultar e mostrar demandas. */
@Service
public class DemandaService {

    private static final int RAIO_DUPLICADA_METROS = 100;
    private static final int RAIO_PERTO_METROS = 2000;
    private static final double METROS_POR_GRAU = 111_000.0;

    private final DemandaRepository demandaRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final HistoricoService historicoService;
    private final PrioridadeService prioridadeService;
    private final FotoService fotoService;

    public DemandaService(DemandaRepository demandaRepository, UsuarioService usuarioService,
                          CategoriaService categoriaService, HistoricoService historicoService,
                          PrioridadeService prioridadeService, FotoService fotoService) {
        this.demandaRepository = demandaRepository;
        this.usuarioService = usuarioService;
        this.categoriaService = categoriaService;
        this.historicoService = historicoService;
        this.prioridadeService = prioridadeService;
        this.fotoService = fotoService;
    }

    /** Demanda registrada pelo próprio cidadão no site. */
    @Transactional
    public Demanda registrar(NovaDemandaForm form, Long autorId) {
        Usuario autor = usuarioService.buscar(autorId);
        return criar(form, autor, Canal.APP, "Demanda registrada pelo site.");
    }

    /**
     * Demanda que chegou por telefone, presencialmente ou por WhatsApp e foi digitada por um atendente.
     * É isso que junta todos os canais numa base só.
     */
    @Transactional
    public Demanda registrarPorAtendente(NovaDemandaForm form, Long atendenteId) {
        if (form.getCanal() == null || form.getCanal() == Canal.APP) {
            throw new NegocioException("Informe por onde a demanda chegou.");
        }
        Usuario atendente = usuarioService.buscar(atendenteId);
        Demanda demanda = criar(form, atendente, form.getCanal(),
                "Demanda recebida por " + form.getCanal().getDescricao().toLowerCase()
                        + " e registrada por " + atendente.getNome() + ".");
        demanda.setSolicitanteNome(limpar(form.getSolicitanteNome()));
        demanda.setSolicitanteTelefone(limpar(form.getSolicitanteTelefone()));
        return demanda;
    }

    /** Recalcula a prioridade das demandas abertas (o tempo em aberto conta pontos a cada dia). */
    @Transactional
    public int recalcularPrioridades() {
        List<Demanda> abertas = demandaRepository.findByStatusIn(StatusDemanda.emAberto());
        abertas.forEach(prioridadeService::atualizar);
        demandaRepository.saveAll(abertas);
        return abertas.size();
    }

    private Demanda criar(NovaDemandaForm form, Usuario autor, Canal canal, String textoHistorico) {
        if (!AreaAtendimento.contem(form.getLatitude(), form.getLongitude())) {
            throw new NegocioException("Marque no mapa um local dentro da área de atendimento permitida.");
        }
        Categoria categoria = categoriaService.buscar(form.getCategoriaId());

        Demanda demanda = new Demanda();
        demanda.setCategoria(categoria);
        // A categoria já diz qual setor atende: é o encaminhamento automático.
        demanda.setSetor(categoria.getSetorPadrao());
        demanda.setAutor(autor);
        demanda.setDescricao(form.getDescricao().trim());
        demanda.setStatus(StatusDemanda.ABERTA);
        demanda.setUrgencia(form.getUrgencia());
        demanda.setAlcance(form.getAlcance());
        demanda.setRiscoSeguranca(form.isRiscoSeguranca());
        demanda.setCanal(canal);
        demanda.setLatitude(form.getLatitude());
        demanda.setLongitude(form.getLongitude());
        demanda.setEndereco(limpar(form.getEndereco()));
        demanda.setBairro(limpar(form.getBairro()));
        demanda.setPontoReferencia(limpar(form.getPontoReferencia()));
        demanda.setFoto(fotoService.salvar(form.getFoto()));
        demanda.setCriadaEm(LocalDateTime.now());
        demanda.setAtualizadaEm(demanda.getCriadaEm());
        prioridadeService.atualizar(demanda);

        demandaRepository.save(demanda);
        demanda.setProtocolo(gerarProtocolo(demanda));

        String destino = demanda.getSetor() != null
                ? "Direcionada ao setor " + demanda.getSetor().getNome() + "."
                : "Aguardando triagem para definir o setor.";
        historicoService.registrar(demanda, autor, TipoHistorico.CRIACAO, textoHistorico + " " + destino);
        return demanda;
    }

    public Demanda buscar(Long id) {
        return demandaRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Demanda não encontrada."));
    }

    public Demanda buscarPorProtocolo(String protocolo) {
        return demandaRepository.findByProtocolo(protocolo.toUpperCase())
                .orElseThrow(() -> new NaoEncontradoException("Demanda não encontrada."));
    }

    public List<Demanda> minhas(Long autorId) {
        return demandaRepository.findByAutorIdOrderByCriadaEmDesc(autorId);
    }

    public List<Demanda> minhasEmAberto(Long autorId) {
        return demandaRepository.findByAutorIdAndStatusInOrderByCriadaEmDesc(autorId, StatusDemanda.emAberto());
    }

    public ResumoCidadao resumoDoCidadao(Long autorId) {
        return new ResumoCidadao(
                demandaRepository.countByAutorIdAndStatusIn(autorId, StatusDemanda.emAberto()),
                demandaRepository.countByAutorIdAndStatusIn(autorId, List.of(StatusDemanda.CONCLUIDA)),
                demandaRepository.somarApoiosRecebidos(autorId));
    }

    public List<Demanda> maisApoiadasDeOutros(Long usuarioId) {
        return demandaRepository.findTop8ByStatusInAndAutorIdNotOrderByTotalApoiosDescCriadaEmDesc(
                StatusDemanda.emAberto(), usuarioId);
    }

    public List<DemandaProxima> pertoDe(double latitude, double longitude, Long usuarioId) {
        return proximas(latitude, longitude, RAIO_PERTO_METROS, null).stream()
                .filter(item -> !item.demanda().getAutor().getId().equals(usuarioId))
                .limit(8)
                .toList();
    }

    /** Demandas abertas da mesma categoria a até 100 m: provavelmente é o mesmo problema. */
    public List<DemandaProxima> possiveisDuplicadas(Long categoriaId, double latitude, double longitude) {
        return proximas(latitude, longitude, RAIO_DUPLICADA_METROS, categoriaId);
    }

    /**
     * Nome, contato e foto do autor só aparecem para o próprio autor, para o admin
     * e para a equipe do setor responsável (LGPD). Quem não tem setor (planejamento) vê todos.
     */
    public boolean podeVerDadosPrivados(Demanda demanda, UsuarioLogado usuario) {
        if (demanda.getAutor().getId().equals(usuario.getId()) || usuario.isAdmin()) {
            return true;
        }
        if (!usuario.isServidorOuAcima()) {
            return false;
        }
        return usuario.getSetorId() == null
                || (demanda.getSetor() != null && usuario.getSetorId().equals(demanda.getSetor().getId()));
    }

    private List<DemandaProxima> proximas(double latitude, double longitude, int raioMetros, Long categoriaId) {
        double grausLat = raioMetros / METROS_POR_GRAU;
        double grausLng = grausLat / Math.cos(Math.toRadians(latitude));

        return demandaRepository.buscarNaRegiao(StatusDemanda.emAberto(),
                        latitude - grausLat, latitude + grausLat, longitude - grausLng, longitude + grausLng)
                .stream()
                .filter(d -> categoriaId == null || d.getCategoria().getId().equals(categoriaId))
                .map(d -> new DemandaProxima(d, (int) distanciaEmMetros(latitude, longitude, d.getLatitude(), d.getLongitude())))
                .filter(item -> item.distanciaMetros() <= raioMetros)
                .sorted(Comparator.comparingInt(DemandaProxima::distanciaMetros))
                .toList();
    }

    // Fórmula de Haversine: distância entre dois pontos considerando a curvatura da Terra.
    static double distanciaEmMetros(double lat1, double lng1, double lat2, double lng2) {
        double raioTerra = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return raioTerra * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static String gerarProtocolo(Demanda demanda) {
        return "ARC-%d-%06d".formatted(demanda.getCriadaEm().getYear(), demanda.getId());
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
