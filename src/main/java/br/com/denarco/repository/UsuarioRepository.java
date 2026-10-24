package br.com.denarco.repository;

import br.com.denarco.model.Perfil;
import br.com.denarco.model.StatusConta;
import br.com.denarco.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    List<Usuario> findByStatusContaOrderByCriadoEmAsc(StatusConta statusConta);

    List<Usuario> findByPerfilInOrderByNomeAsc(Collection<Perfil> perfis);

    List<Usuario> findBySetorIdAndPerfilInOrderByNomeAsc(Long setorId, Collection<Perfil> perfis);

    // Quem pode ser responsável por uma demanda do setor.
    List<Usuario> findBySetorIdAndPerfilInAndStatusContaOrderByNomeAsc(Long setorId, Collection<Perfil> perfis,
                                                                       StatusConta statusConta);
}
