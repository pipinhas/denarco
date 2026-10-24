package br.com.denarco.service;

import br.com.denarco.dto.ResultadoConsulta;
import br.com.denarco.model.Categoria;
import br.com.denarco.model.Demanda;
import br.com.denarco.model.Setor;
import br.com.denarco.model.StatusDemanda;
import br.com.denarco.model.Usuario;
import br.com.denarco.repository.DemandaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta em linguagem simples: "iluminação atrasada no Centro" vira filtros.
 * Funciona por regras e um dicionário de palavras, sem inteligência artificial e sem custo.
 * A resposta sempre mostra os filtros entendidos, para a pessoa conferir (respostas verificáveis).
 * O texto nunca vira SQL: ele só escolhe valores para uma consulta fixa.
 */
@Service
public class ConsultaService {

    private static final int MAXIMO_RESULTADOS = 50;

    // Palavra (sem acento, minúscula) -> nome da categoria.
    private static final Map<String, String> PALAVRAS_CATEGORIA = Map.ofEntries(
            Map.entry("buraco", "Buraco na rua"),
            Map.entry("cratera", "Buraco na rua"),
            Map.entry("calcamento", "Calçamento danificado"),
            Map.entry("calcada", "Calçamento danificado"),
            Map.entry("poste", "Poste apagado"),
            Map.entry("lampada", "Poste apagado"),
            Map.entry("escuro", "Poste apagado"),
            Map.entry("fio", "Fiação exposta"),
            Map.entry("fiacao", "Fiação exposta"),
            Map.entry("lixo", "Lixo acumulado"),
            Map.entry("coleta", "Lixo acumulado"),
            Map.entry("entulho", "Entulho na via"),
            Map.entry("bueiro", "Bueiro entupido"),
            Map.entry("alagamento", "Alagamento"),
            Map.entry("alaga", "Alagamento"),
            Map.entry("enchente", "Alagamento"),
            Map.entry("mato", "Mato alto"),
            Map.entry("terreno", "Mato alto"),
            Map.entry("praca", "Praça ou equipamento danificado"),
            Map.entry("brinquedo", "Praça ou equipamento danificado"));

    // Palavra -> nome do setor.
    private static final Map<String, String> PALAVRAS_SETOR = Map.of(
            "iluminacao", "Iluminação pública",
            "luz", "Iluminação pública",
            "limpeza", "Limpeza urbana",
            "drenagem", "Drenagem",
            "pavimentacao", "Pavimentação",
            "asfalto", "Pavimentação",
            "conservacao", "Conservação de espaços públicos");

    private static final Pattern ULTIMOS_DIAS = Pattern.compile("ultimos? (\\d{1,3}) dias?");

    private final DemandaRepository demandaRepository;
    private final CategoriaService categoriaService;
    private final SetorService setorService;

    public ConsultaService(DemandaRepository demandaRepository, CategoriaService categoriaService,
                           SetorService setorService) {
        this.demandaRepository = demandaRepository;
        this.categoriaService = categoriaService;
        this.setorService = setorService;
    }

    public ResultadoConsulta consultar(String pergunta, Usuario usuario) {
        String texto = normalizar(pergunta);
        List<String> entendido = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        // 1) Tipos de problema: pelo dicionário e pelo nome de qualquer categoria cadastrada.
        Set<Categoria> categorias = new LinkedHashSet<>();
        for (Categoria categoria : categoriaService.listarTodas()) {
            boolean citadaPeloNome = texto.contains(normalizar(categoria.getNome()));
            boolean citadaPorPalavra = PALAVRAS_CATEGORIA.entrySet().stream()
                    .anyMatch(e -> contemPalavra(texto, e.getKey()) && e.getValue().equals(categoria.getNome()));
            if (citadaPeloNome || citadaPorPalavra) {
                categorias.add(categoria);
            }
        }
        categorias.forEach(c -> entendido.add("Tipo: " + c.getNome()));

        // 2) Setor.
        Setor setorPerguntado = null;
        for (Setor setor : setorService.listarTodos()) {
            boolean citado = texto.contains(normalizar(setor.getNome()))
                    || PALAVRAS_SETOR.entrySet().stream()
                    .anyMatch(e -> contemPalavra(texto, e.getKey()) && e.getValue().equals(setor.getNome()));
            if (citado) {
                setorPerguntado = setor;
                break;
            }
        }

        // Quem é de um setor só consulta o próprio setor.
        Long setorId;
        if (AtendimentoService.enxergaTodosOsSetores(usuario)) {
            setorId = setorPerguntado != null ? setorPerguntado.getId() : null;
            if (setorPerguntado != null) {
                entendido.add("Setor: " + setorPerguntado.getNome());
            }
        } else {
            setorId = usuario.getSetor().getId();
            if (setorPerguntado != null && !setorPerguntado.getId().equals(setorId)) {
                avisos.add("Você só consulta o setor " + usuario.getSetor().getNome() + ".");
            }
        }

        // 3) Situação.
        List<StatusDemanda> status = StatusDemanda.emAberto();
        LocalDate vencidoAntesDe = null;
        boolean semResponsavel = false;
        if (texto.contains("atrasad") || texto.contains("vencid")) {
            vencidoAntesDe = LocalDate.now();
            entendido.add("Situação: atrasadas");
        } else if (texto.contains("sem responsavel")) {
            status = List.of(StatusDemanda.ABERTA);
            semResponsavel = true;
            entendido.add("Situação: sem responsável");
        } else if (texto.contains("conclu") || texto.contains("resolvid") || texto.contains("finalizad")) {
            status = List.of(StatusDemanda.CONCLUIDA);
            entendido.add("Situação: concluídas");
        } else if (texto.contains("andamento")) {
            status = List.of(StatusDemanda.EM_ANDAMENTO);
            entendido.add("Situação: em andamento");
        } else if (texto.contains("cancelad")) {
            status = List.of(StatusDemanda.CANCELADA);
            entendido.add("Situação: canceladas");
        } else {
            entendido.add("Situação: em aberto");
        }

        // 4) Período (pela data de registro).
        LocalDateTime desde = null;
        Matcher ultimos = ULTIMOS_DIAS.matcher(texto);
        if (ultimos.find()) {
            int dias = Integer.parseInt(ultimos.group(1));
            desde = LocalDate.now().minusDays(dias).atStartOfDay();
            entendido.add("Período: últimos " + dias + " dias");
        } else if (contemPalavra(texto, "hoje")) {
            desde = LocalDate.now().atStartOfDay();
            entendido.add("Período: hoje");
        } else if (contemPalavra(texto, "ontem")) {
            desde = LocalDate.now().minusDays(1).atStartOfDay();
            entendido.add("Período: desde ontem");
        } else if (contemPalavra(texto, "semana")) {
            desde = LocalDate.now().minusDays(7).atStartOfDay();
            entendido.add("Período: últimos 7 dias");
        } else if (contemPalavra(texto, "mes")) {
            desde = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            entendido.add("Período: este mês");
        } else if (contemPalavra(texto, "ano")) {
            desde = LocalDate.now().withDayOfYear(1).atStartOfDay();
            entendido.add("Período: este ano");
        }

        // 5) Bairro: qualquer bairro que já apareceu em alguma demanda.
        String bairro = null;
        for (String conhecido : demandaRepository.bairrosConhecidos()) {
            if (contemPalavra(texto, normalizar(conhecido))) {
                bairro = conhecido.toLowerCase();
                entendido.add("Bairro: " + conhecido);
                break;
            }
        }

        // 6) Ordem: por apoios, se a pergunta pedir; senão, pela prioridade.
        Sort ordem = Sort.by(Sort.Order.desc("pontuacao"), Sort.Order.asc("criadaEm"));
        if (texto.contains("apoiad") || texto.contains("apoios")) {
            ordem = Sort.by(Sort.Order.desc("totalApoios"));
            entendido.add("Ordem: mais apoiadas primeiro");
        }

        List<Long> categoriaIds = categorias.stream().map(Categoria::getId).toList();
        Page<Demanda> pagina = demandaRepository.buscarConsulta(status, setorId, categoriaIds.isEmpty(),
                categoriaIds.isEmpty() ? List.of(-1L) : categoriaIds, bairro, desde, vencidoAntesDe, semResponsavel,
                PageRequest.of(0, MAXIMO_RESULTADOS, ordem));

        return new ResultadoConsulta(pergunta, entendido, avisos, pagina.getTotalElements(), pagina.getContent());
    }

    /** Minúsculas e sem acentos: "Iluminação" e "iluminacao" passam a ser iguais. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    // Procura a palavra inteira ou o começo dela ("buracos" contém "buraco"; "semana" não contém "mes").
    private static boolean contemPalavra(String texto, String palavra) {
        return Pattern.compile("\\b" + Pattern.quote(palavra)).matcher(texto).find();
    }
}
