package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.AlteracaoPerfil;
import br.com.denarco.model.Convite;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.StatusConta;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.AlteracaoPerfilRepository;
import br.com.denarco.repository.ConviteRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Gestão de pessoas: aprovar contas corporativas, convidar e mudar perfis.
 * Regra da seção 3 do plano: você só mexe em quem está abaixo de você e só dá
 * perfis abaixo do seu (o admin também pode nomear outro admin). Gestor de um setor
 * só cuida das pessoas do próprio setor. Tudo fica registrado em AlteracaoPerfil.
 */
@Service
public class EquipeService {

    private static final List<Perfil> EQUIPE = List.of(Perfil.SERVIDOR, Perfil.GESTOR, Perfil.ADMIN);

    private final UsuarioRepository usuarioRepository;
    private final ConviteRepository conviteRepository;
    private final AlteracaoPerfilRepository alteracaoRepository;
    private final UsuarioService usuarioService;
    private final SetorService setorService;

    public EquipeService(UsuarioRepository usuarioRepository, ConviteRepository conviteRepository,
                         AlteracaoPerfilRepository alteracaoRepository, UsuarioService usuarioService,
                         SetorService setorService) {
        this.usuarioRepository = usuarioRepository;
        this.conviteRepository = conviteRepository;
        this.alteracaoRepository = alteracaoRepository;
        this.usuarioService = usuarioService;
        this.setorService = setorService;
    }

    // ---------- Regras ----------

    public static boolean podeConceder(Usuario ator, Perfil perfil) {
        return ator.getPerfil() == Perfil.ADMIN || ator.getPerfil().acimaDe(perfil);
    }

    public List<Perfil> perfisQuePodeConceder(Usuario ator) {
        return Arrays.stream(Perfil.values()).filter(perfil -> podeConceder(ator, perfil)).toList();
    }

    public static boolean podeGerenciar(Usuario ator, Usuario alvo) {
        // Só gestor ou admin cuida de pessoas, e nunca de si mesmo ou de alguém do mesmo nível.
        if (!ator.getPerfil().peloMenos(Perfil.GESTOR) || ator.getId().equals(alvo.getId())
                || !ator.getPerfil().acimaDe(alvo.getPerfil())) {
            return false;
        }
        if (ator.getPerfil() == Perfil.ADMIN || ator.getSetor() == null) {
            return true;
        }
        // Gestor de setor: cidadãos (sem setor) ou pessoas do próprio setor.
        return alvo.getSetor() == null || alvo.getSetor().getId().equals(ator.getSetor().getId());
    }

    /** Define o setor do novo perfil. Gestor de setor só coloca pessoas no próprio setor. */
    private Setor setorPara(Usuario ator, Perfil perfil, Long setorId) {
        if (perfil == Perfil.CIDADAO || perfil == Perfil.ADMIN) {
            return null;
        }
        if (ator.getPerfil() != Perfil.ADMIN && ator.getSetor() != null) {
            return ator.getSetor();
        }
        if (setorId == null) {
            if (perfil == Perfil.GESTOR) {
                return null; // gestor sem setor = planejamento e controle (vê todos)
            }
            throw new NegocioException("Escolha o setor do servidor.");
        }
        return setorService.buscar(setorId);
    }

    // ---------- Ações ----------

    @Transactional
    public void alterarPerfil(Long usuarioId, Perfil novoPerfil, Long setorId, Usuario ator) {
        Usuario alvo = usuarioService.buscar(usuarioId);
        if (!podeGerenciar(ator, alvo)) {
            throw new NegocioException("Você não pode alterar o acesso de " + alvo.getNome() + ".");
        }
        if (novoPerfil == null || !podeConceder(ator, novoPerfil)) {
            throw new NegocioException("Você não pode dar esse perfil.");
        }
        aplicar(alvo, novoPerfil, setorPara(ator, novoPerfil, setorId), ator);
    }

    @Transactional
    public void aprovar(Long usuarioId, Perfil perfil, Long setorId, Usuario ator) {
        Usuario alvo = usuarioService.buscar(usuarioId);
        if (!alvo.isPendente()) {
            throw new NegocioException("Este pedido já foi respondido.");
        }
        // Se ninguém escolher outro setor, vale o que a pessoa pediu no cadastro.
        Long setor = setorId != null ? setorId : (alvo.getSetor() != null ? alvo.getSetor().getId() : null);
        alterarPerfil(usuarioId, perfil, setor, ator);
    }

    /** Recusar mantém a conta, mas como cidadão comum. */
    @Transactional
    public void recusar(Long usuarioId, Usuario ator) {
        Usuario alvo = usuarioService.buscar(usuarioId);
        if (!alvo.isPendente() || !podeGerenciar(ator, alvo)) {
            throw new NegocioException("Você não pode responder este pedido.");
        }
        alvo.setAtuacao(null);
        aplicar(alvo, Perfil.CIDADAO, null, ator);
    }

    @Transactional
    public Convite convidar(String email, Perfil perfil, Long setorId, Usuario ator) {
        if (email == null || !email.contains("@")) {
            throw new NegocioException("Digite um e-mail válido.");
        }
        String emailNormalizado = UsuarioService.normalizarEmail(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new NegocioException("Essa pessoa já tem conta. Busque pelo e-mail abaixo e altere o perfil dela.");
        }
        if (perfil == null || perfil == Perfil.CIDADAO || !podeConceder(ator, perfil)) {
            throw new NegocioException("Você não pode convidar alguém com esse perfil.");
        }

        Convite convite = new Convite();
        convite.setEmail(emailNormalizado);
        convite.setPerfil(perfil);
        convite.setSetor(setorPara(ator, perfil, setorId));
        convite.setCriadoPor(ator);
        convite.setCriadoEm(LocalDateTime.now());
        return conviteRepository.save(convite);
    }

    @Transactional
    public void cancelarConvite(Long conviteId, Usuario ator) {
        Convite convite = conviteRepository.findById(conviteId)
                .orElseThrow(() -> new NegocioException("Convite não encontrado."));
        boolean doMesmoSetor = ator.getSetor() != null && convite.getSetor() != null
                && ator.getSetor().getId().equals(convite.getSetor().getId());
        if (ator.getPerfil() != Perfil.ADMIN && !doMesmoSetor && !convite.getCriadoPor().getId().equals(ator.getId())) {
            throw new NegocioException("Você não pode cancelar este convite.");
        }
        convite.setCancelado(true);
        conviteRepository.save(convite);
    }

    private void aplicar(Usuario alvo, Perfil novoPerfil, Setor setor, Usuario ator) {
        AlteracaoPerfil registro = new AlteracaoPerfil();
        registro.setUsuario(alvo);
        registro.setPerfilAnterior(alvo.getPerfil());
        registro.setPerfilNovo(novoPerfil);
        registro.setSetor(setor);
        registro.setAlteradoPor(ator);
        registro.setCriadoEm(LocalDateTime.now());
        alteracaoRepository.save(registro);

        alvo.setPerfil(novoPerfil);
        alvo.setSetor(setor);
        alvo.setStatusConta(StatusConta.ATIVA);
        if (novoPerfil == Perfil.CIDADAO) {
            alvo.setAtuacao(null);
        }
        usuarioRepository.save(alvo);
    }

    // ---------- Listas da tela "Equipe" ----------

    public List<Usuario> pedidosPendentes(Usuario ator) {
        return usuarioRepository.findByStatusContaOrderByCriadoEmAsc(StatusConta.PENDENTE).stream()
                .filter(alvo -> podeGerenciar(ator, alvo))
                .toList();
    }

    public List<Usuario> equipe(Usuario ator) {
        if (ator.getPerfil() == Perfil.ADMIN || ator.getSetor() == null) {
            return usuarioRepository.findByPerfilInOrderByNomeAsc(EQUIPE);
        }
        return usuarioRepository.findBySetorIdAndPerfilInOrderByNomeAsc(ator.getSetor().getId(), EQUIPE);
    }

    /** Busca exata por e-mail: assim dá para promover um cidadão sem expor a lista de todos os cidadãos. */
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(UsuarioService.normalizarEmail(email));
    }

    public List<Convite> convitesPendentes(Usuario ator) {
        if (ator.getPerfil() == Perfil.ADMIN || ator.getSetor() == null) {
            return conviteRepository.findByUsadoEmIsNullAndCanceladoFalseOrderByCriadoEmDesc();
        }
        return conviteRepository.findBySetorIdAndUsadoEmIsNullAndCanceladoFalseOrderByCriadoEmDesc(ator.getSetor().getId());
    }

    public List<AlteracaoPerfil> ultimasAlteracoes(Usuario ator) {
        if (ator.getPerfil() == Perfil.ADMIN || ator.getSetor() == null) {
            return alteracaoRepository.findTop10ByOrderByCriadoEmDesc();
        }
        return alteracaoRepository.findTop10BySetorIdOrderByCriadoEmDesc(ator.getSetor().getId());
    }
}
