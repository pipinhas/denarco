// Mapa territorial: busca os pontos em /api/mapa e agrupa os que estão próximos (Leaflet.markercluster).
(function () {
    // Centro de Arcoverde, usado enquanto os pontos não chegam.
    const CENTRO = [-8.4189, -37.0539];
    const AREA = { sul: -8.49870, norte: -8.25309, oeste: -37.14904, leste: -36.89687 };
    const form = document.getElementById('filtros-mapa');
    const contador = document.getElementById('contador-mapa');

    const mapa = L.map('mapa-territorial')
        .setMaxBounds(L.latLngBounds([AREA.sul, AREA.oeste], [AREA.norte, AREA.leste]))
        .setView(CENTRO, 14);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; colaboradores do OpenStreetMap'
    }).addTo(mapa);

    // Pontos próximos viram um círculo com a quantidade: é assim que aparecem as concentrações.
    const grupo = L.markerClusterGroup({
        showCoverageOnHover: false,
        maxClusterRadius: 50,
        iconCreateFunction: function (cluster) {
            return L.divIcon({ html: '<span>' + cluster.getChildCount() + '</span>', className: 'grupo-pinos', iconSize: [42, 42] });
        }
    });
    mapa.addLayer(grupo);

    function classeDaFaixa(faixa) {
        if (faixa === 'Alta') return 'alta';
        if (faixa === 'Média') return 'media';
        return 'baixa';
    }

    // Monta o balão com textContent: o texto vem de outras pessoas e não pode virar código.
    function montarBalao(p) {
        const caixa = document.createElement('div');
        const titulo = document.createElement('strong');
        titulo.textContent = p.categoria;
        const local = document.createElement('div');
        local.textContent = p.local;
        const situacao = document.createElement('div');
        situacao.className = 'small';
        situacao.textContent = p.status + (p.atrasada ? ' · atrasada' : '') + ' · prioridade ' + p.pontuacao
            + ' · ' + p.apoios + (p.apoios === 1 ? ' apoio' : ' apoios');
        const link = document.createElement('a');
        link.href = '/demandas/' + p.protocolo;
        link.textContent = 'Ver demanda ' + p.protocolo;
        caixa.append(titulo, local, situacao, link);
        return caixa;
    }

    function carregar() {
        const parametros = new URLSearchParams(new FormData(form));
        fetch('/api/mapa?' + parametros)
            .then(function (resposta) { return resposta.json(); })
            .then(function (pontos) {
                grupo.clearLayers();
                const pontosNaArea = pontos.filter(function (p) {
                    return Number.isFinite(p.latitude) && Number.isFinite(p.longitude)
                        && p.latitude >= AREA.sul && p.latitude <= AREA.norte
                        && p.longitude >= AREA.oeste && p.longitude <= AREA.leste;
                });
                pontosNaArea.forEach(function (p) {
                    const marcador = L.marker([p.latitude, p.longitude], {
                        icon: L.divIcon({
                            html: '<span>' + p.pontuacao + '</span>',
                            className: 'pino pino-' + classeDaFaixa(p.faixa),
                            iconSize: [34, 34]
                        }),
                        title: p.categoria + ', prioridade ' + p.pontuacao,
                        keyboard: true
                    });
                    marcador.bindPopup(montarBalao(p));
                    grupo.addLayer(marcador);
                });
                contador.textContent = pontosNaArea.length === 1
                    ? '1 demanda no mapa' : pontosNaArea.length + ' demandas no mapa';
                if (pontosNaArea.length > 0) {
                    mapa.fitBounds(grupo.getBounds(), { padding: [30, 30], maxZoom: 16 });
                }
            });
    }

    form.addEventListener('change', carregar);
    form.addEventListener('submit', function (evento) {
        evento.preventDefault();
        carregar();
    });
    carregar();
})();
