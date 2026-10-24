package br.com.denarco.service;

import br.com.denarco.dto.Contagem;
import br.com.denarco.dto.DadosGraficos;
import br.com.denarco.dto.Intervalo;
import br.com.denarco.dto.ResumoPainel;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.repository.DemandaRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Números e gráficos do painel de gestão.
 * setorId nulo = cidade toda. O período é sempre "os últimos N dias até agora".
 */
@Service
public class PainelService {

    public static final List<Integer> PERIODOS = List.of(7, 30, 90);
    private static final int MAXIMO_BAIRROS = 6;
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final DemandaRepository demandaRepository;

    public PainelService(DemandaRepository demandaRepository) {
        this.demandaRepository = demandaRepository;
    }

    public static int periodoValido(Integer dias) {
        return dias != null && PERIODOS.contains(dias) ? dias : 30;
    }

    public ResumoPainel resumo(Long setorId, int dias) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        List<Intervalo> concluidas = demandaRepository.intervalosConcluidos(setorId, StatusDemanda.CONCLUIDA, desde);
        return new ResumoPainel(
                demandaRepository.contar(setorId, StatusDemanda.emAberto()),
                demandaRepository.contarAtrasadas(setorId, StatusDemanda.emAberto(), LocalDate.now()),
                concluidas.size(),
                tempoMedio(concluidas));
    }

    public DadosGraficos graficos(Long setorId, int dias) {
        List<Contagem> porBairro = demandaRepository.contarPorBairro(setorId, StatusDemanda.emAberto());
        return montarSemanas(setorId, dias,
                demandaRepository.contarPorCategoria(setorId, StatusDemanda.emAberto()),
                porBairro.stream().limit(MAXIMO_BAIRROS).toList());
    }

    /** Tempo médio entre registrar e concluir, em texto amigável ("2,5 dias", "6 horas"). */
    static String tempoMedio(List<Intervalo> concluidas) {
        if (concluidas.isEmpty()) {
            return "—";
        }
        double horas = concluidas.stream()
                .mapToLong(i -> Duration.between(i.inicio(), i.fim()).toMinutes())
                .average()
                .orElse(0) / 60.0;
        if (horas < 24) {
            return Math.max(1, Math.round(horas)) + (Math.round(horas) <= 1 ? " hora" : " horas");
        }
        return "%.1f dias".formatted(horas / 24).replace('.', ',');
    }

    // Agrupa por semana (começando na segunda-feira) as demandas registradas e concluídas no período.
    private DadosGraficos montarSemanas(Long setorId, int dias, List<Contagem> porCategoria, List<Contagem> porBairro) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        LocalDate primeiraSemana = desde.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int quantidade = (int) ChronoUnit.WEEKS.between(primeiraSemana, LocalDate.now()) + 1;

        List<String> semanas = new ArrayList<>();
        long[] registradas = new long[quantidade];
        long[] concluidas = new long[quantidade];
        for (int i = 0; i < quantidade; i++) {
            semanas.add(primeiraSemana.plusWeeks(i).format(DIA_MES));
        }
        for (LocalDateTime data : demandaRepository.datasDeCriacao(setorId, desde)) {
            registradas[indiceDaSemana(primeiraSemana, data, quantidade)]++;
        }
        for (Intervalo intervalo : demandaRepository.intervalosConcluidos(setorId, StatusDemanda.CONCLUIDA, desde)) {
            concluidas[indiceDaSemana(primeiraSemana, intervalo.fim(), quantidade)]++;
        }
        return new DadosGraficos(porCategoria, semanas, paraLista(registradas), paraLista(concluidas), porBairro);
    }

    private int indiceDaSemana(LocalDate primeiraSemana, LocalDateTime data, int quantidade) {
        int indice = (int) ChronoUnit.WEEKS.between(primeiraSemana, data.toLocalDate());
        return Math.min(Math.max(indice, 0), quantidade - 1);
    }

    private List<Long> paraLista(long[] valores) {
        List<Long> lista = new ArrayList<>();
        for (long valor : valores) {
            lista.add(valor);
        }
        return lista;
    }
}
