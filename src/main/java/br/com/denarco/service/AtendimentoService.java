package br.com.denarco.service;

import br.com.denarco.dto.FiltroFila;
import br.com.denarco.dto.ResumoFila;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Alcance;
import br.com.denarco.model.Canal;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.MotivoCancelamento;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.StatusConta;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.model.TipoHistorico;
import br.com.denarco.model.Urgencia;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.DemandaRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static br.com.denarco.model.StatusDemanda.ABERTA;
import static br.com.denarco.model.StatusDemanda.CANCELADA;
import static br.com.denarco.model.StatusDemanda.CONCLUIDA;
import static br.com.denarco.model.StatusDemanda.EM_ANDAMENTO;
import static br.com.denarco.model.StatusDemanda.ENCAMINHADA;

/**
 * Trabalho da equipe da Prefeitura: fila, encaminhamento, execução e conclusão das demandas.
 * As permissões são conferidas com os dados atuais do banco (não só os da sessão),
 * e cada ação vira um passo na linha do tempo da demanda.
 */
@Service
public class AtendimentoService {

    private static final int ITENS_POR_PAGINA = 20;
    private static final int DIAS_PARA_REABRIR = 7;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DemandaRepository demandaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DemandaService demandaService;
    private final UsuarioService usuarioService;
    private final SetorService setorService;
    private final HistoricoService historicoService;
    private final PrioridadeService prioridadeService;
    private final ApoioService apoioService;
    private final FotoService fotoService;
    private final EmailService emailService;
    private final String urlBase;

    public AtendimentoService(DemandaRepository demandaRepository, UsuarioRepository usuarioRepository,
                              DemandaService demandaService, UsuarioService usuarioService, SetorService setorService,
                              HistoricoService historicoService, PrioridadeService prioridadeService,
                              ApoioService apoioService, FotoService fotoService, EmailService emailService,
                              @Value("${app.url-base}") String urlBase) {
        this.demandaRepository = demandaRepository;
        this.usuarioRepository = usuarioRepository;
        this.demandaService = demandaService;
        this.usuarioService = usuarioService;
        this.setorService = setorService;
        this.historicoService = historicoService;
        this.prioridadeService = prioridadeService;
        this.apoioService = apoioService;
        this.fotoService = fotoService;
        this.emailService = emailService;
        this.urlBase = urlBase;
    }

    // ---------- Permissões ----------

    /** Admin e gestor sem setor (planejamento e controle) acompanham a cidade toda. */
    public static boolean enxergaTodosOsSetores(Usuario usuario) {
        return usuario.getPerfil() == Perfil.ADMIN
                || (usuario.getPerfil() == Perfil.GESTOR && usuario.getSetor() == null);
    }

    /** Servidor ou gestor do setor da demanda (ou quem enxerga todos os setores). */
    public boolean podeAtender(Demanda demanda, Usuario usuario) {
        if (!usuario.getPerfil().peloMenos(Perfil.SERVIDOR)) {
            return false;
        }
        if (enxergaTodosOsSetores(usuario)) {
            return true;
        }
        return usuario.getSetor() != null && demanda.getSetor() != null
                && demanda.getSetor().getId().equals(usuario.getSetor().getId());
    }

    /** Encaminhar, transferir, cancelar e mexer na prioridade é só para gestor. */
    public boolean podeGerir(Demanda demanda, Usuario usuario) {
        return usuario.getPerfil().peloMenos(Perfil.GESTOR) && podeAtender(demanda, usuario);
    }

    // ---------- Fila ----------

    public Page<Demanda> fila(FiltroFila filtro, Usuario usuario) {
        Long setorId = escopo(usuario, filtro);
        String bairro = filtro.getBairro() == null || filtro.getBairro().isBlank()
                ? null : "%" + filtro.getBairro().trim().toLowerCase() + "%";

        List<StatusDemanda> status = StatusDemanda.emAberto();
        Long responsavelId = null;
        boolean semResponsavel = false;
        LocalDate vencidoAntesDe = null;
        // Mais prioritárias primeiro; empate: a mais antiga primeiro.
        Sort ordem = Sort.by(Sort.Order.desc("pontuacao"), Sort.Order.asc("criadaEm"));

        switch (filtro.getAba()) {
            case MINHAS -> responsavelId = usuario.getId();
            case SEM_RESPONSAVEL -> {
                status = List.of(ABERTA);
                semResponsavel = true;
            }
            case ATRASADAS -> vencidoAntesDe = LocalDate.now();
            case CONCLUIDAS -> {
                status = List.of(CONCLUIDA);
                ordem = Sort.by(Sort.Order.desc("concluidaEm"));
            }
            case EM_ABERTO -> { }
        }

        return demandaRepository.buscarFila(status, setorId, filtro.getCategoriaId(), bairro, responsavelId,
                semResponsavel, vencidoAntesDe, PageRequest.of(filtro.getPagina(), ITENS_POR_PAGINA, ordem));
    }

    public ResumoFila resumo(FiltroFila filtro, Usuario usuario) {
        Long setorId = escopo(usuario, filtro);
        return new ResumoFila(
                demandaRepository.contar(setorId, StatusDemanda.emAberto()),
                demandaRepository.contarSemResponsavel(setorId, ABERTA),
                demandaRepository.contarAtrasadas(setorId, StatusDemanda.emAberto(), LocalDate.now()),
                demandaRepository.contarConcluidasDesde(setorId, CONCLUIDA, LocalDateTime.now().minusDays(30)));
    }

    // Quem é de um setor só vê o seu; quem enxerga todos pode escolher um setor no filtro (ou nenhum = todos).
    private Long escopo(Usuario usuario, FiltroFila filtro) {
        if (enxergaTodosOsSetores(usuario)) {
            return filtro.getSetorId();
        }
        if (usuario.getSetor() == null) {
            throw new AccessDeniedException("Usuário sem setor");
        }
        return usuario.getSetor().getId();
    }

    /** Pessoas que podem ser responsáveis por uma demanda do setor. */
    public List<Usuario> possiveisResponsaveis(Setor setor) {
        if (setor == null) {
            return List.of();
        }
        return usuarioRepository.findBySetorIdAndPerfilInAndStatusContaOrderByNomeAsc(
                setor.getId(), List.of(Perfil.SERVIDOR, Perfil.GESTOR), StatusConta.ATIVA);
    }

    // ---------- Ações do gestor ----------

    @Transactional
    public Demanda encaminhar(Long demandaId, Long responsavelId, LocalDate prazo, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirGestao(demanda, ator);
        exigirEmAberto(demanda);
        if (demanda.getSetor() == null) {
            throw new NegocioException("Escolha o setor antes de definir o responsável.");
        }
        if (responsavelId == null) {
            throw new NegocioException("Escolha quem vai cuidar da demanda.");
        }
        Usuario responsavel = usuarioService.buscar(responsavelId);
        if (possiveisResponsaveis(demanda.getSetor()).stream().noneMatch(u -> u.getId().equals(responsavelId))) {
            throw new NegocioException("O responsável precisa ser da equipe de " + demanda.getSetor().getNome() + ".");
        }
        if (prazo == null || prazo.isBefore(LocalDate.now())) {
            throw new NegocioException("Escolha um prazo de hoje em diante.");
        }

        demanda.setResponsavel(responsavel);
        demanda.setPrazo(prazo);
        if (demanda.getStatus() == ABERTA) {
            mudarStatus(demanda, ENCAMINHADA);
        }
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.ENCAMINHAMENTO,
                "Encaminhada para " + responsavel.getNome() + ". Prazo: " + prazo.format(DATA) + ".");
        return demanda;
    }

    @Transactional
    public Demanda transferirSetor(Long demandaId, Long setorId, String motivo, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirGestao(demanda, ator);
        if (demanda.getStatus() != ABERTA && demanda.getStatus() != ENCAMINHADA) {
            throw new NegocioException("Só dá para transferir demandas que ainda não começaram a ser atendidas.");
        }
        exigirTexto(motivo, "Explique por que a demanda vai para outro setor.");
        Setor novo = setorService.buscar(setorId);
        if (demanda.getSetor() != null && demanda.getSetor().getId().equals(novo.getId())) {
            throw new NegocioException("A demanda já está nesse setor.");
        }

        String de = demanda.getSetor() != null ? demanda.getSetor().getNome() : "triagem geral";
        demanda.setSetor(novo);
        demanda.setResponsavel(null);
        demanda.setPrazo(null);
        if (demanda.getStatus() == ENCAMINHADA) {
            mudarStatus(demanda, ABERTA);
        }
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.ENCAMINHAMENTO,
                "Transferida de " + de + " para " + novo.getNome() + ". Motivo: " + motivo.trim());
        return demanda;
    }

    @Transactional
    public Demanda corrigirClassificacao(Long demandaId, Urgencia urgencia, Alcance alcance, boolean risco, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirGestao(demanda, ator);
        exigirEmAberto(demanda);

        List<String> mudancas = new ArrayList<>();
        if (urgencia != null && urgencia != demanda.getUrgencia()) {
            mudancas.add("urgência " + demanda.getUrgencia().getDescricao() + " → " + urgencia.getDescricao());
            demanda.setUrgencia(urgencia);
        }
        if (alcance != null && alcance != demanda.getAlcance()) {
            mudancas.add("alcance " + demanda.getAlcance().getDescricao() + " → " + alcance.getDescricao());
            demanda.setAlcance(alcance);
        }
        if (risco != demanda.isRiscoSeguranca()) {
            mudancas.add(risco ? "marcada com risco à segurança" : "risco à segurança retirado");
            demanda.setRiscoSeguranca(risco);
        }
        if (mudancas.isEmpty()) {
            throw new NegocioException("Nada foi alterado.");
        }
        prioridadeService.atualizar(demanda);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.PRIORIDADE,
                "Classificação corrigida: " + String.join("; ", mudancas) + ".");
        return demanda;
    }

    /** O gestor pode passar por cima da fórmula, mas precisa dizer o motivo (fica na linha do tempo). */
    @Transactional
    public Demanda fixarPrioridade(Long demandaId, Integer valor, String justificativa, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirGestao(demanda, ator);
        exigirEmAberto(demanda);
        if (valor == null || valor < 0 || valor > 100) {
            throw new NegocioException("A prioridade vai de 0 a 100.");
        }
        exigirTexto(justificativa, "Escreva o motivo do ajuste: ele fica registrado na linha do tempo.");

        int anterior = demanda.getPontuacao();
        demanda.setPontuacao(valor);
        demanda.setPontuacaoFixada(true);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.PRIORIDADE,
                "Prioridade ajustada de " + anterior + " para " + valor + ". Motivo: " + justificativa.trim());
        return demanda;
    }

    @Transactional
    public Demanda voltarPrioridadeAutomatica(Long demandaId, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirGestao(demanda, ator);
        demanda.setPontuacaoFixada(false);
        prioridadeService.atualizar(demanda);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.PRIORIDADE,
                "Prioridade voltou ao cálculo automático (" + demanda.getPontuacao() + ").");
        return demanda;
    }

    @Transactional
    public Demanda cancelar(Long demandaId, MotivoCancelamento motivo, String protocoloOriginal, String observacao,
                            Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirGestao(demanda, ator);
        if (motivo == null) {
            throw new NegocioException("Escolha o motivo do cancelamento.");
        }

        String texto = "Cancelada: " + motivo.getDescricao().toLowerCase();
        if (motivo == MotivoCancelamento.DUPLICADA) {
            exigirTexto(protocoloOriginal, "Informe o protocolo da demanda original.");
            Demanda original = demandaService.buscarPorProtocolo(protocoloOriginal.trim());
            if (original.getId().equals(demanda.getId()) || !original.getStatus().isEmAberto()) {
                throw new NegocioException("A demanda original precisa ser outra e estar em aberto.");
            }
            texto += " (" + original.getProtocolo() + ")";
            if (apoioService.transferirApoio(original, demanda.getAutor())) {
                texto += ". Quem registrou passou a apoiar a original";
            }
        }
        if (observacao != null && !observacao.isBlank()) {
            texto += ". " + observacao.trim();
        }

        mudarStatus(demanda, CANCELADA);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.CANCELAMENTO, texto + ".");
        return demanda;
    }

    // ---------- Ações de quem executa ----------

    @Transactional
    public Demanda iniciar(Long demandaId, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirAtendimento(demanda, ator);
        mudarStatus(demanda, EM_ANDAMENTO);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.STATUS, "Atendimento iniciado.");
        return demanda;
    }

    @Transactional
    public Demanda registrarProvidencia(Long demandaId, String texto, MultipartFile foto, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirAtendimento(demanda, ator);
        exigirEmAberto(demanda);
        exigirTexto(texto, "Descreva o que foi feito.");

        String nomeFoto = fotoService.salvar(foto);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.PROVIDENCIA, texto.trim(), nomeFoto);
        return demanda;
    }

    @Transactional
    public Demanda concluir(Long demandaId, String texto, MultipartFile foto, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        exigirAtendimento(demanda, ator);
        if (demanda.getStatus() == ABERTA) {
            throw new NegocioException("Antes de concluir, a demanda precisa ter um responsável.");
        }
        exigirTexto(texto, "Conte o que foi feito para resolver.");

        String nomeFoto = fotoService.salvar(foto);
        mudarStatus(demanda, CONCLUIDA);
        demanda.setConcluidaEm(LocalDateTime.now());
        prioridadeService.atualizar(demanda);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.CONCLUSAO, "Concluída: " + texto.trim(), nomeFoto);
        avisarAutor(demanda, texto.trim());
        return demanda;
    }

    // ---------- Reabrir ----------

    /** O autor pode reabrir até 7 dias depois de concluída; o gestor do setor, a qualquer momento. */
    public boolean podeReabrir(Demanda demanda, Usuario usuario) {
        if (demanda.getStatus() != CONCLUIDA) {
            return false;
        }
        boolean autorNoPrazo = demanda.getAutor().getId().equals(usuario.getId()) && demanda.getConcluidaEm() != null
                && demanda.getConcluidaEm().isAfter(LocalDateTime.now().minusDays(DIAS_PARA_REABRIR));
        return autorNoPrazo || podeGerir(demanda, usuario);
    }

    @Transactional
    public Demanda reabrir(Long demandaId, String motivo, Usuario ator) {
        Demanda demanda = demandaService.buscar(demandaId);
        if (!podeReabrir(demanda, ator)) {
            throw new NegocioException("Esta demanda não pode mais ser reaberta por você.");
        }
        exigirTexto(motivo, "Conte o que ainda está errado.");
        mudarStatus(demanda, ABERTA);
        demanda.setResponsavel(null);
        demanda.setPrazo(null);
        demanda.setConcluidaEm(null);
        prioridadeService.atualizar(demanda);
        salvar(demanda);
        historicoService.registrar(demanda, ator, TipoHistorico.STATUS, "Reaberta: " + motivo.trim());
        return demanda;
    }

    // ---------- Apoio interno ----------

    private void mudarStatus(Demanda demanda, StatusDemanda novo) {
        if (!demanda.getStatus().podeMudarPara(novo)) {
            throw new NegocioException("Não é possível passar de \"" + demanda.getStatus().getDescricao()
                    + "\" para \"" + novo.getDescricao() + "\".");
        }
        demanda.setStatus(novo);
    }

    private void salvar(Demanda demanda) {
        demanda.setAtualizadaEm(LocalDateTime.now());
        demandaRepository.save(demanda);
    }

    private void exigirAtendimento(Demanda demanda, Usuario usuario) {
        if (!podeAtender(demanda, usuario)) {
            throw new AccessDeniedException("Sem permissão para atender esta demanda");
        }
    }

    private void exigirGestao(Demanda demanda, Usuario usuario) {
        if (!podeGerir(demanda, usuario)) {
            throw new AccessDeniedException("Sem permissão para gerir esta demanda");
        }
    }

    private void exigirEmAberto(Demanda demanda) {
        if (!demanda.getStatus().isEmAberto()) {
            throw new NegocioException("Esta demanda já foi encerrada.");
        }
    }

    private void exigirTexto(String texto, String mensagem) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException(mensagem);
        }
    }

    // Só avisa quem registrou pelo site; demandas por telefone têm o atendente como autor.
    private void avisarAutor(Demanda demanda, String texto) {
        if (demanda.getCanal() != Canal.APP) {
            return;
        }
        emailService.enviar(demanda.getAutor().getEmail(), "Denarco: demanda " + demanda.getProtocolo() + " concluída",
                "Olá, " + demanda.getAutor().getNome() + ".\n\n"
                        + "Sua demanda \"" + demanda.getCategoria().getNome() + "\" foi concluída: " + texto + "\n\n"
                        + "Se o problema continuar, você pode reabrir em até " + DIAS_PARA_REABRIR + " dias:\n"
                        + urlBase + "/demandas/" + demanda.getProtocolo());
    }
}
