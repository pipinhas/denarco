package br.com.denarco.repository;

import br.com.denarco.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrueOrderById();

    Optional<Categoria> findByNome(String nome);
}
