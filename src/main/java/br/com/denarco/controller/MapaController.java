package br.com.denarco.controller;

import br.com.denarco.dto.PontoMapa;
import br.com.denarco.service.CategoriaService;
import br.com.denarco.service.MapaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/** Mapa territorial: onde estão as demandas e onde elas se concentram. Aberto a todos os perfis. */
@Controller
public class MapaController {

    private final MapaService mapaService;
    private final CategoriaService categoriaService;

    public MapaController(MapaService mapaService, CategoriaService categoriaService) {
        this.mapaService = mapaService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/mapa")
    public String mapa(Model model) {
        model.addAttribute("categorias", categoriaService.listarAtivas());
        model.addAttribute("menuAtivo", "mapa");
        return "mapa/mapa";
    }

    @GetMapping("/api/mapa")
    @ResponseBody
    public List<PontoMapa> pontos(@RequestParam(required = false) String situacao,
                                  @RequestParam(required = false) Long categoriaId,
                                  @RequestParam(required = false) Integer dias) {
        return mapaService.pontos(situacao, categoriaId, dias);
    }
}
