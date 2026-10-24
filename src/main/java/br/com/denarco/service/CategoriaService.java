package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import br.com.denarco.model.Categoria;
import br.com.denarco.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tipos de problema. Como os setores, nunca são apagados: só desativados. */
@Service
public class CategoriaService {

    // Ícones que o admin pode escolher (Bootstrap Icons).
    public static final List<String> ICONES = List.of("bi-cone-striped", "bi-bricks", "bi-lightbulb-off",
            "bi-lightning-charge", "bi-trash3", "bi-truck", "bi-droplet-half", "bi-water", "bi-flower3", "bi-tree",
            "bi-signpost-2", "bi-tree-fill", "bi-bug", "bi-car-front", "bi-bus-front", "bi-house-exclamation",
            "bi-exclamation-triangle", "bi-three-dots");

    private final CategoriaRepository categoriaRepository;
    private final SetorService setorService;

    public CategoriaService(CategoriaRepository categoriaRepository, SetorService setorService) {
        this.categoriaRepository = categoriaRepository;
        this.setorService = setorService;
    }

    public List<Categoria> listarAtivas() {
        return categoriaRepository.findByAtivaTrueOrderById();
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll().stream()
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .toList();
    }

    public Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Tipo de problema não encontrado."));
    }

    @Transactional
    public Categoria criar(String nome, String icone, Long setorPadraoId, Integer prazoDias) {
        if (nome == null || nome.isBlank()) {
            throw new NegocioException("Digite o nome do tipo de problema.");
        }
        if (categoriaRepository.findByNome(nome.trim()).isPresent()) {
            throw new NegocioException("Já existe um tipo de problema com esse nome.");
        }
        Categoria categoria = new Categoria();
        categoria.setNome(nome.trim());
        preencher(categoria, icone, setorPadraoId, prazoDias);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria atualizar(Long id, String icone, Long setorPadraoId, Integer prazoDias) {
        Categoria categoria = buscar(id);
        preencher(categoria, icone, setorPadraoId, prazoDias);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria alternarAtiva(Long id) {
        Categoria categoria = buscar(id);
        categoria.setAtiva(!categoria.isAtiva());
        return categoriaRepository.save(categoria);
    }

    private void preencher(Categoria categoria, String icone, Long setorPadraoId, Integer prazoDias) {
        if (prazoDias == null || prazoDias < 1 || prazoDias > 365) {
            throw new NegocioException("O prazo padrão precisa ficar entre 1 e 365 dias.");
        }
        categoria.setIcone(ICONES.contains(icone) ? icone : "bi-three-dots");
        // Sem setor padrão: a demanda cai na triagem geral.
        categoria.setSetorPadrao(setorPadraoId == null ? null : setorService.buscar(setorPadraoId));
        categoria.setPrazoPadraoDias(prazoDias);
    }
}
