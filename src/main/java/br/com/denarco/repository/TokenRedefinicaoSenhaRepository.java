package br.com.denarco.repository;

import br.com.denarco.model.TokenRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByToken(String token);
}
