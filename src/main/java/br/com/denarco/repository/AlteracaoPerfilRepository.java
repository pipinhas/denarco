package br.com.denarco.repository;

import br.com.denarco.model.AlteracaoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlteracaoPerfilRepository extends JpaRepository<AlteracaoPerfil, Long> {

    List<AlteracaoPerfil> findTop10ByOrderByCriadoEmDesc();

    List<AlteracaoPerfil> findTop10BySetorIdOrderByCriadoEmDesc(Long setorId);
}
