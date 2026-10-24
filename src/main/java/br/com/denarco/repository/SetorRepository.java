package br.com.denarco.repository;

import br.com.denarco.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    List<Setor> findByAtivoTrueOrderByNome();

    Optional<Setor> findByNome(String nome);
}
