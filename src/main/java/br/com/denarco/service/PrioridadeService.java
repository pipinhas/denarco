package br.com.denarco.service;

import br.com.denarco.dto.ParcelaPrioridade;
import br.com.denarco.model.Demanda;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula a prioridade de uma demanda (0 a 100) com a fórmula da seção 4.3 do plano.
 * A conta aparece na tela em "Por que esta prioridade?", para que qualquer pessoa consiga conferir.
 * A fórmula só sugere: um gestor pode fixar outro valor, com justificativa.
 */
@Service
public class PrioridadeService {

    private static final int PONTOS_RISCO_SEGURANCA = 50;
    private static final int PONTOS_POR_APOIO = 2;

    // Apoios valem no máximo 20 pontos: uma demanda muito apoiada
    // não pode passar na frente de um problema com risco à segurança.
    private static final int MAXIMO_PONTOS_APOIO = 20;
    private static final int MAXIMO_PONTOS_TEMPO = 10;

    public List<ParcelaPrioridade> explicar(Demanda demanda) {
        List<ParcelaPrioridade> parcelas = new ArrayList<>();

        if (demanda.isRiscoSeguranca()) {
            parcelas.add(new ParcelaPrioridade("Risco à segurança", PONTOS_RISCO_SEGURANCA));
        } else {
            parcelas.add(new ParcelaPrioridade("Urgência " + demanda.getUrgencia().getDescricao().toLowerCase(),
                    demanda.getUrgencia().getPontos()));
        }

        parcelas.add(new ParcelaPrioridade("Alcance: " + demanda.getAlcance().getDescricao().toLowerCase(),
                demanda.getAlcance().getPontos()));

        int apoios = demanda.getTotalApoios();
        parcelas.add(new ParcelaPrioridade(apoios + (apoios == 1 ? " apoio" : " apoios"),
                Math.min(apoios * PONTOS_POR_APOIO, MAXIMO_PONTOS_APOIO)));

        long dias = diasEmAberto(demanda);
        parcelas.add(new ParcelaPrioridade(dias + (dias == 1 ? " dia em aberto" : " dias em aberto"),
                (int) Math.min(dias, MAXIMO_PONTOS_TEMPO)));

        return parcelas;
    }

    public int calcular(Demanda demanda) {
        return explicar(demanda).stream().mapToInt(ParcelaPrioridade::pontos).sum();
    }

    /** Recalcula e guarda na demanda, a não ser que um gestor tenha fixado a prioridade. */
    public void atualizar(Demanda demanda) {
        if (!demanda.isPontuacaoFixada()) {
            demanda.setPontuacao(calcular(demanda));
        }
    }

    private long diasEmAberto(Demanda demanda) {
        LocalDateTime fim = demanda.getConcluidaEm() != null ? demanda.getConcluidaEm() : LocalDateTime.now();
        LocalDate inicio = demanda.getCriadaEm() != null ? demanda.getCriadaEm().toLocalDate() : LocalDate.now();
        return Math.max(0, ChronoUnit.DAYS.between(inicio, fim.toLocalDate()));
    }
}
