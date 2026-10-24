package br.com.denarco.repository;

import br.com.denarco.dto.Contagem;
import br.com.denarco.dto.Intervalo;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.StatusDemanda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DemandaRepository extends JpaRepository<Demanda, Long> {

    Optional<Demanda> findByProtocolo(String protocolo);

    List<Demanda> findByAutorIdOrderByCriadaEmDesc(Long autorId);

    List<Demanda> findByAutorIdAndStatusInOrderByCriadaEmDesc(Long autorId, Collection<StatusDemanda> status);

    long countByAutorIdAndStatusIn(Long autorId, Collection<StatusDemanda> status);

    @Query("select coalesce(sum(d.totalApoios), 0) from Demanda d where d.autor.id = :autorId")
    long somarApoiosRecebidos(@Param("autorId") Long autorId);

    // Demandas abertas de outras pessoas, as mais apoiadas primeiro.
    List<Demanda> findTop8ByStatusInAndAutorIdNotOrderByTotalApoiosDescCriadaEmDesc(
            Collection<StatusDemanda> status, Long autorId);

    List<Demanda> findByStatusIn(Collection<StatusDemanda> status);

    /**
     * Fila de atendimento. Cada filtro é opcional: parâmetro nulo (ou false) desliga o filtro.
     * setorId nulo = todos os setores (só para quem enxerga a cidade toda).
     */
    @Query("""
            select d from Demanda d
            where d.status in :status
              and (:setorId is null or d.setor.id = :setorId)
              and (:categoriaId is null or d.categoria.id = :categoriaId)
              and (:bairro is null or lower(d.bairro) like :bairro)
              and (:responsavelId is null or d.responsavel.id = :responsavelId)
              and (:somenteSemResponsavel = false or d.responsavel is null)
              and (:vencidoAntesDe is null or d.prazo < :vencidoAntesDe)
            """)
    Page<Demanda> buscarFila(@Param("status") Collection<StatusDemanda> status,
                             @Param("setorId") Long setorId,
                             @Param("categoriaId") Long categoriaId,
                             @Param("bairro") String bairro,
                             @Param("responsavelId") Long responsavelId,
                             @Param("somenteSemResponsavel") boolean somenteSemResponsavel,
                             @Param("vencidoAntesDe") LocalDate vencidoAntesDe,
                             Pageable pagina);

    @Query("select count(d) from Demanda d where (:setorId is null or d.setor.id = :setorId) and d.status in :status")
    long contar(@Param("setorId") Long setorId, @Param("status") Collection<StatusDemanda> status);

    @Query("""
            select count(d) from Demanda d
            where (:setorId is null or d.setor.id = :setorId) and d.status = :status and d.responsavel is null
            """)
    long contarSemResponsavel(@Param("setorId") Long setorId, @Param("status") StatusDemanda status);

    @Query("""
            select count(d) from Demanda d
            where (:setorId is null or d.setor.id = :setorId) and d.status in :status and d.prazo < :hoje
            """)
    long contarAtrasadas(@Param("setorId") Long setorId, @Param("status") Collection<StatusDemanda> status,
                         @Param("hoje") LocalDate hoje);

    @Query("""
            select count(d) from Demanda d
            where (:setorId is null or d.setor.id = :setorId) and d.status = :status and d.concluidaEm >= :desde
            """)
    long contarConcluidasDesde(@Param("setorId") Long setorId, @Param("status") StatusDemanda status,
                               @Param("desde") LocalDateTime desde);

    // ---------- Painel (as contas são feitas no banco, não em laços no Java) ----------

    @Query("""
            select count(d) from Demanda d
            where (:setorId is null or d.setor.id = :setorId) and d.criadaEm >= :desde
            """)
    long contarCriadasDesde(@Param("setorId") Long setorId, @Param("desde") LocalDateTime desde);

    @Query("""
            select new br.com.denarco.dto.Contagem(d.categoria.nome, count(d)) from Demanda d
            where (:setorId is null or d.setor.id = :setorId) and d.status in :status
            group by d.categoria.nome
            order by count(d) desc
            """)
    List<Contagem> contarPorCategoria(@Param("setorId") Long setorId, @Param("status") Collection<StatusDemanda> status);

    @Query("""
            select new br.com.denarco.dto.Contagem(d.bairro, count(d)) from Demanda d
            where (:setorId is null or d.setor.id = :setorId) and d.status in :status and d.bairro is not null
            group by d.bairro
            order by count(d) desc
            """)
    List<Contagem> contarPorBairro(@Param("setorId") Long setorId, @Param("status") Collection<StatusDemanda> status);

    @Query("select d.criadaEm from Demanda d where (:setorId is null or d.setor.id = :setorId) and d.criadaEm >= :desde")
    List<LocalDateTime> datasDeCriacao(@Param("setorId") Long setorId, @Param("desde") LocalDateTime desde);

    @Query("""
            select new br.com.denarco.dto.Intervalo(d.criadaEm, d.concluidaEm) from Demanda d
            where (:setorId is null or d.setor.id = :setorId) and d.status = :status and d.concluidaEm >= :desde
            """)
    List<Intervalo> intervalosConcluidos(@Param("setorId") Long setorId, @Param("status") StatusDemanda status,
                                         @Param("desde") LocalDateTime desde);

    // ---------- Mapa ----------

    @Query("""
            select d from Demanda d
            where d.latitude is not null and d.status in :status
              and (:categoriaId is null or d.categoria.id = :categoriaId)
              and (:desde is null or d.criadaEm >= :desde)
            """)
    List<Demanda> buscarParaMapa(@Param("status") Collection<StatusDemanda> status, @Param("categoriaId") Long categoriaId,
                                 @Param("desde") LocalDateTime desde);

    // ---------- Consulta em linguagem simples ----------

    @Query("select distinct d.bairro from Demanda d where d.bairro is not null")
    List<String> bairrosConhecidos();

    /** Como a fila, mas aceita várias categorias e período. todasCategorias = true desliga o filtro de categoria. */
    @Query("""
            select d from Demanda d
            where d.status in :status
              and (:setorId is null or d.setor.id = :setorId)
              and (:todasCategorias = true or d.categoria.id in :categoriaIds)
              and (:bairro is null or lower(d.bairro) = :bairro)
              and (:desde is null or d.criadaEm >= :desde)
              and (:vencidoAntesDe is null or d.prazo < :vencidoAntesDe)
              and (:somenteSemResponsavel = false or d.responsavel is null)
            """)
    Page<Demanda> buscarConsulta(@Param("status") Collection<StatusDemanda> status,
                                 @Param("setorId") Long setorId,
                                 @Param("todasCategorias") boolean todasCategorias,
                                 @Param("categoriaIds") Collection<Long> categoriaIds,
                                 @Param("bairro") String bairro,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("vencidoAntesDe") LocalDate vencidoAntesDe,
                                 @Param("somenteSemResponsavel") boolean somenteSemResponsavel,
                                 Pageable pagina);

    // Busca por um "quadrado" em volta do ponto; a distância exata é calculada depois, no service.
    @Query("""
            select d from Demanda d
            where d.status in :status
              and d.latitude between :latMin and :latMax
              and d.longitude between :lngMin and :lngMax
            """)
    List<Demanda> buscarNaRegiao(@Param("status") Collection<StatusDemanda> status,
                                 @Param("latMin") double latMin, @Param("latMax") double latMax,
                                 @Param("lngMin") double lngMin, @Param("lngMax") double lngMax);
}
