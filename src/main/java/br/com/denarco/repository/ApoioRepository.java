package br.com.denarco.repository;

import br.com.denarco.model.Apoio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ApoioRepository extends JpaRepository<Apoio, Long> {

    Optional<Apoio> findByDemandaIdAndUsuarioId(Long demandaId, Long usuarioId);

    boolean existsByDemandaIdAndUsuarioId(Long demandaId, Long usuarioId);

    // Quais demandas de uma lista o usuário já apoiou (para pintar o botão "Ainda presente").
    @Query("select a.demanda.id from Apoio a where a.usuario.id = :usuarioId and a.demanda.id in :demandaIds")
    Set<Long> idsApoiados(@Param("usuarioId") Long usuarioId, @Param("demandaIds") Collection<Long> demandaIds);
}
