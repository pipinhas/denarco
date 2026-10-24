package br.com.denarco.service;

import br.com.denarco.dto.CadastroCorporativoForm;
import br.com.denarco.dto.CadastroForm;
import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.AlteracaoPerfil;
import br.com.denarco.model.Atuacao;
import br.com.denarco.model.Convite;
import br.com.denarco.model.Perfil;
import br.com.denarco.model.Setor;
import br.com.denarco.model.StatusConta;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.AlteracaoPerfilRepository;
import br.com.denarco.repository.ConviteRepository;
import br.com.denarco.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** Regras de cadastro e de contas de usuário. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConviteRepository conviteRepository;
    private final AlteracaoPerfilRepository alteracaoRepository;
    private final SetorService setorService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, ConviteRepository conviteRepository,
                          AlteracaoPerfilRepository alteracaoRepository, SetorService setorService,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.conviteRepository = conviteRepository;
        this.alteracaoRepository = alteracaoRepository;
        this.setorService = setorService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarCidadao(CadastroForm form) {
        Usuario usuario = novoUsuario(form);
        usuario.setPerfil(Perfil.CIDADAO);
        usuario.setStatusConta(StatusConta.ATIVA);
        return usuarioRepository.save(usuario);
    }

    /**
     * A conta corporativa nasce PENDENTE: a pessoa usa o sistema como cidadão
     * até um gestor do setor ou um admin aprovar. Sem isso, qualquer um poderia
     * se cadastrar como servidor e ver dados de cidadãos.
     * Exceção: se o e-mail foi convidado, a conta já nasce aprovada com o perfil e setor do convite.
     */
    @Transactional
    public Usuario cadastrarCorporativo(CadastroCorporativoForm form) {
        Optional<Convite> convite = conviteRepository
                .findFirstByEmailAndUsadoEmIsNullAndCanceladoFalseOrderByCriadoEmDesc(normalizarEmail(form.getEmail()));

        Setor setor = null;
        if (form.getSetorId() != null) {
            setor = setorService.buscar(form.getSetorId());
        } else if (form.getAtuacao() != Atuacao.PLANEJAMENTO && convite.isEmpty()) {
            throw new NegocioException("Escolha o setor em que você trabalha.");
        }

        Usuario usuario = novoUsuario(form);
        usuario.setPerfil(Perfil.CIDADAO);
        usuario.setStatusConta(StatusConta.PENDENTE);
        usuario.setAtuacao(form.getAtuacao());
        usuario.setSetor(setor);

        if (convite.isPresent()) {
            aceitarConvite(usuario, convite.get());
        }
        return usuarioRepository.save(usuario);
    }

    private void aceitarConvite(Usuario usuario, Convite convite) {
        usuarioRepository.save(usuario);

        AlteracaoPerfil registro = new AlteracaoPerfil();
        registro.setUsuario(usuario);
        registro.setPerfilAnterior(Perfil.CIDADAO);
        registro.setPerfilNovo(convite.getPerfil());
        registro.setSetor(convite.getSetor());
        registro.setAlteradoPor(convite.getCriadoPor());
        registro.setCriadoEm(LocalDateTime.now());
        alteracaoRepository.save(registro);

        usuario.setPerfil(convite.getPerfil());
        usuario.setSetor(convite.getSetor());
        usuario.setStatusConta(StatusConta.ATIVA);
        convite.setUsadoEm(LocalDateTime.now());
        conviteRepository.save(convite);
    }

    /** Usado para criar o admin inicial e os usuários de demonstração. */
    @Transactional
    public Usuario criarContaAtiva(String nome, String email, String senha, String telefone, Perfil perfil, Setor setor) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(normalizarEmail(email));
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setTelefone(telefone);
        usuario.setPerfil(perfil);
        usuario.setStatusConta(StatusConta.ATIVA);
        usuario.setSetor(setor);
        usuario.setCriadoEm(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Usuário não encontrado."));
    }

    @Transactional
    public void concluirTutorial(Long id) {
        Usuario usuario = buscar(id);
        usuario.setTutorialConcluido(true);
        usuarioRepository.save(usuario);
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(normalizarEmail(email));
    }

    public boolean existeAdmin() {
        return usuarioRepository.existsByPerfil(Perfil.ADMIN);
    }

    // E-mail sempre em minúsculas e sem espaços, para "Maria@X.com " e "maria@x.com" serem a mesma conta.
    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private Usuario novoUsuario(CadastroForm form) {
        String email = normalizarEmail(form.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("Este e-mail já tem uma conta. Se não lembra a senha, use \"Esqueci minha senha\".");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(form.getNome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(form.getSenha()));
        usuario.setTelefone(form.getTelefone().trim());
        usuario.setCriadoEm(LocalDateTime.now());
        return usuario;
    }
}
