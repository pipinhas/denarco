// Painel: busca os dados em /api/painel e desenha os gráficos (Chart.js), o ranking de bairros e as tabelas.
(function () {
    const api = document.getElementById('script-painel').dataset.api;

    // Escreve o valor na ponta de cada barra, com a cor do texto (nunca a cor da barra).
    const valorNaPonta = {
        id: 'valorNaPonta',
        afterDatasetsDraw: function (grafico) {
            const ctx = grafico.ctx;
            ctx.save();
            ctx.fillStyle = Chart.defaults.color;
            ctx.font = '600 13px system-ui, -apple-system, "Segoe UI", sans-serif';
            ctx.textBaseline = 'middle';
            grafico.getDatasetMeta(0).data.forEach(function (barra, i) {
                ctx.fillText(grafico.data.datasets[0].data[i], barra.x + 6, barra.y);
            });
            ctx.restore();
        }
    };

    // Escreve o último valor no fim de cada linha. Se os dois fins ficarem colados, só o primeiro ganha rótulo.
    const valorNoFim = {
        id: 'valorNoFim',
        afterDatasetsDraw: function (grafico) {
            const ctx = grafico.ctx;
            const usados = [];
            ctx.save();
            ctx.fillStyle = Chart.defaults.color;
            ctx.font = '600 13px system-ui, -apple-system, "Segoe UI", sans-serif';
            ctx.textBaseline = 'middle';
            grafico.data.datasets.forEach(function (serie, i) {
                const pontos = grafico.getDatasetMeta(i).data;
                const ultimo = pontos[pontos.length - 1];
                if (!ultimo || usados.some(function (y) { return Math.abs(y - ultimo.y) < 16; })) return;
                ctx.fillText(serie.label + ': ' + serie.data[serie.data.length - 1], ultimo.x + 10, ultimo.y);
                usados.push(ultimo.y);
            });
            ctx.restore();
        }
    };

    // Azul e laranja passaram no teste de daltonismo e de contraste; no alto contraste trocamos por amarelo e ciano.
    function paleta() {
        if (document.body.classList.contains('alto-contraste')) {
            return { serie1: '#ffd400', serie2: '#00e5ff', texto: '#ffffff', grade: '#555555', fundo: '#000000' };
        }
        return { serie1: '#2a78d6', serie2: '#eb6834', texto: '#52514e', grade: '#e1e0d9', fundo: '#ffffff' };
    }

    let dados = null;
    let graficos = [];

    fetch(api)
        .then(function (resposta) { return resposta.json(); })
        .then(function (resposta) {
            dados = resposta;
            desenhar();
        });

    // Quando a pessoa liga o alto contraste, os gráficos são redesenhados com as cores certas.
    document.addEventListener('denarco:acessibilidade', function () {
        if (dados) desenhar();
    });

    function desenhar() {
        graficos.forEach(function (g) { g.destroy(); });
        graficos = [];
        const cores = paleta();
        Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", sans-serif';
        Chart.defaults.font.size = 13;
        Chart.defaults.color = cores.texto;

        desenharCategorias(cores);
        desenharSemanas(cores);
        desenharBairros();
        montarTabela('tabela-categorias', ['Tipo de problema', 'Em aberto'],
            dados.porCategoria.map(function (c) { return [c.nome, c.total]; }));
        montarTabela('tabela-semanas', ['Semana de', 'Registradas', 'Concluídas'],
            dados.semanas.map(function (s, i) { return [s, dados.registradas[i], dados.concluidas[i]]; }));
    }

    function desenharCategorias(cores) {
        const area = document.getElementById('area-categorias');
        if (dados.porCategoria.length === 0) {
            area.style.height = 'auto';
            area.textContent = 'Nenhuma demanda em aberto.';
            return;
        }
        // Cada tipo ganha a mesma faixa de altura; a barra tem no máximo 24px de espessura.
        area.style.height = (dados.porCategoria.length * 36 + 40) + 'px';
        graficos.push(new Chart(document.getElementById('grafico-categorias'), {
            type: 'bar',
            data: {
                labels: dados.porCategoria.map(function (c) { return c.nome; }),
                datasets: [{
                    data: dados.porCategoria.map(function (c) { return c.total; }),
                    backgroundColor: cores.serie1,
                    borderRadius: { topRight: 4, bottomRight: 4 },
                    borderSkipped: 'start',
                    maxBarThickness: 24
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                layout: { padding: { right: 36 } },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (item) {
                                return ' ' + item.parsed.x + (item.parsed.x === 1 ? ' demanda' : ' demandas');
                            }
                        }
                    }
                },
                scales: {
                    x: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: cores.grade }, border: { display: false } },
                    y: { grid: { display: false }, border: { color: cores.grade } }
                }
            },
            plugins: [valorNaPonta]
        }));
    }

    function desenharSemanas(cores) {
        function serie(nome, valores, cor) {
            return {
                label: nome, data: valores, borderColor: cor, backgroundColor: cor,
                borderWidth: 2, pointRadius: 4, pointHoverRadius: 6,
                pointBorderColor: cores.fundo, pointBorderWidth: 2,
                // "monotone" suaviza sem inventar picos entre os pontos.
                cubicInterpolationMode: 'monotone'
            };
        }
        graficos.push(new Chart(document.getElementById('grafico-semanas'), {
            type: 'line',
            data: {
                labels: dados.semanas,
                datasets: [serie('Registradas', dados.registradas, cores.serie1),
                    serie('Concluídas', dados.concluidas, cores.serie2)]
            },
            plugins: [valorNoFim],
            options: {
                responsive: true,
                maintainAspectRatio: false,
                layout: { padding: { right: 110 } },
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { position: 'top', align: 'start', labels: { usePointStyle: true, boxWidth: 10, boxHeight: 10 } },
                    tooltip: { callbacks: { title: function (itens) { return 'Semana de ' + itens[0].label; } } }
                },
                scales: {
                    y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: cores.grade }, border: { display: false } },
                    x: { grid: { display: false }, border: { color: cores.grade } }
                }
            }
        }));
    }

    // Ranking em HTML (não é imagem): leitores de tela leem nome e número normalmente.
    function desenharBairros() {
        const lista = document.getElementById('lista-bairros');
        lista.replaceChildren();
        if (dados.porBairro.length === 0) {
            const vazio = document.createElement('li');
            vazio.textContent = 'Nenhuma demanda em aberto com bairro informado.';
            lista.appendChild(vazio);
            return;
        }
        const maior = Math.max.apply(null, dados.porBairro.map(function (b) { return b.total; }));
        dados.porBairro.forEach(function (b) {
            const item = document.createElement('li');
            const nome = document.createElement('span');
            nome.className = 'ranking-nome';
            nome.textContent = b.nome;
            const valor = document.createElement('strong');
            valor.className = 'ranking-valor';
            valor.textContent = b.total;
            const trilho = document.createElement('span');
            trilho.className = 'ranking-trilho';
            trilho.setAttribute('aria-hidden', 'true');
            const barra = document.createElement('span');
            barra.className = 'ranking-barra';
            barra.style.width = (b.total / maior * 100) + '%';
            trilho.appendChild(barra);
            item.append(nome, valor, trilho);
            lista.appendChild(item);
        });
    }

    // Tabela com os mesmos números do gráfico (para leitor de tela, impressão ou quem prefere ler).
    function montarTabela(id, cabecalhos, linhas) {
        const tabela = document.createElement('table');
        tabela.className = 'table table-sm mt-2 mb-0';
        const cabeca = tabela.createTHead().insertRow();
        cabecalhos.forEach(function (texto) {
            const th = document.createElement('th');
            th.scope = 'col';
            th.textContent = texto;
            cabeca.appendChild(th);
        });
        const corpo = tabela.createTBody();
        linhas.forEach(function (linha) {
            const tr = corpo.insertRow();
            linha.forEach(function (valor) { tr.insertCell().textContent = valor; });
        });
        document.getElementById(id).replaceChildren(tabela);
    }
})();
