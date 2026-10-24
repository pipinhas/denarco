package br.com.denarco.repository;

import br.com.denarco.model.Convite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConviteRepository extends JpaRepository<Convite, Long> {

    Optional<Convite> findFirstByEmailAndUsadoEmIsNullAndCanceladoFalseOrderByCriadoEmDesc(String email);

    List<Convite> findByUsadoEmIsNullAndCanceladoFalseOrderByCriadoEmDesc();

    List<Convite> findBySetorIdAndUsadoEmIsNullAndCanceladoFalseOrderByCriadoEmDesc(Long setorId);
}
