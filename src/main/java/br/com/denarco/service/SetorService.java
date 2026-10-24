package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Setor;
import br.com.denarco.repository.SetorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Setores da Prefeitura. Setor nunca é apagado, só desativado, para não quebrar o histórico. */
@Service
public class SetorService {

    private final SetorRepository setorRepository;

    public SetorService(SetorRepository setorRepository) {
        this.setorRepository = setorRepository;
    }

    public List<Setor> listarAtivos() {
        return setorRepository.findByAtivoTrueOrderByNome();
    }

    public List<Setor> listarTodos() {
        return setorRepository.findAll().stream()
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .toList();
    }

    public Setor buscar(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Setor não encontrado."));
    }

    @Transactional
    public Setor criar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new NegocioException("Digite o nome do setor.");
        }
        if (setorRepository.findByNome(nome.trim()).isPresent()) {
            throw new NegocioException("Já existe um setor com esse nome.");
        }
        Setor setor = new Setor();
        setor.setNome(nome.trim());
        return setorRepository.save(setor);
    }

    @Transactional
    public Setor alternarAtivo(Long id) {
        Setor setor = buscar(id);
        setor.setAtivo(!setor.isAtivo());
        return setorRepository.save(setor);
    }
}
