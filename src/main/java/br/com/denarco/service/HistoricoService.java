package br.com.denarco.service;

import br.com.denarco.exception.NaoEncontradoException;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.HistoricoDemanda;
import br.com.denarco.model.TipoHistorico;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.HistoricoDemandaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** Linha do tempo das demandas. Só existe "registrar": o histórico nunca é editado nem apagado. */
@Service
public class HistoricoService {

    private final HistoricoDemandaRepository historicoRepository;

    public HistoricoService(HistoricoDemandaRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    public void registrar(Demanda demanda, Usuario usuario, TipoHistorico tipo, String descricao) {
        registrar(demanda, usuario, tipo, descricao, null);
    }

    public void registrar(Demanda demanda, Usuario usuario, TipoHistorico tipo, String descricao, String foto) {
        HistoricoDemanda passo = new HistoricoDemanda();
        passo.setDemanda(demanda);
        passo.setUsuario(usuario);
        passo.setTipo(tipo);
        passo.setDescricao(descricao);
        passo.setFoto(foto);
        passo.setCriadoEm(LocalDateTime.now());
        historicoRepository.save(passo);
    }

    public HistoricoDemanda buscar(Long id) {
        return historicoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Registro não encontrado."));
    }

    public List<HistoricoDemanda> listar(Long demandaId) {
        return historicoRepository.findByDemandaIdOrderByCriadoEmAscIdAsc(demandaId);
    }
}
