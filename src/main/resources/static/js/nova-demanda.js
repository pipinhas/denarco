// Tela "Nova demanda": revela os passos, cuida do mapa, do aviso de duplicada e da foto.
(function () {
    const form = document.getElementById('form-demanda');
    const passoLocal = document.getElementById('passo-local');
    const passoDescricao = document.getElementById('passo-descricao');
    const campoLat = document.getElementById('latitude');
    const campoLng = document.getElementById('longitude');
    const avisoMapa = document.getElementById('aviso-mapa');
    const caixaDuplicada = document.getElementById('aviso-duplicada');

    // Centro de Arcoverde, usado antes de a pessoa marcar o local.
    const CENTRO = [-8.4189, -37.0539];
    let mapa = null;
    let marcador = null;

    function categoriaEscolhida() {
        return form.querySelector('input[name="categoriaId"]:checked');
    }

    // Mostra uma seção escondida. Devolve true se ela estava escondida.
    function revelar(secao) {
        if (!secao.hidden) return false;
        secao.hidden = false;
        return true;
    }

    // ---------- Passo 1: categoria ----------
    form.querySelectorAll('input[name="categoriaId"]').forEach(function (opcao) {
        opcao.addEventListener('change', function () {
            const primeiraVez = revelar(passoLocal);
            iniciarMapa();
            if (primeiraVez) passoLocal.scrollIntoView({ behavior: 'smooth', block: 'start' });
            verificarDuplicadas();
        });
    });

    // ---------- Passo 2: local ----------
    function iniciarMapa() {
        if (mapa) return;
        mapa = L.map('mapa').setView(CENTRO, 14);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; colaboradores do OpenStreetMap'
        }).addTo(mapa);
        mapa.on('click', function (evento) {
            marcar(evento.latlng.lat, evento.latlng.lng, false);
        });
        // Volta com erro de validação: o ponto já escolhido continua no mapa.
        if (campoLat.value && campoLng.value) {
            marcar(Number(campoLat.value), Number(campoLng.value), true, true);
        }
    }

    function marcar(lat, lng, centralizar, jaTemEndereco) {
        if (!marcador) {
            marcador = L.marker([lat, lng], { draggable: true }).addTo(mapa);
            marcador.on('dragend', function () {
                const ponto = marcador.getLatLng();
                marcar(ponto.lat, ponto.lng, false);
            });
        } else {
            marcador.setLatLng([lat, lng]);
        }
        if (centralizar) mapa.setView([lat, lng], 17);

        campoLat.value = lat.toFixed(6);
        campoLng.value = lng.toFixed(6);
        avisoMapa.textContent = 'Local marcado. Se precisar, arraste o marcador.';
        if (!jaTemEndereco) buscarEndereco(lat, lng);

        const primeiraVez = revelar(passoDescricao);
        verificarDuplicadas().then(function (achou) {
            // Se não há duplicada, o cursor já vai para a descrição: é só digitar.
            if (!achou && primeiraVez) document.getElementById('descricao').focus();
        });
    }

    // No modo atendente não há botão de GPS (o atendente não está no local do problema).
    const botaoLocalizacao = document.getElementById('btn-localizacao');
    if (botaoLocalizacao) botaoLocalizacao.addEventListener('click', function () {
        const botao = this;
        if (!navigator.geolocation) {
            avisoMapa.textContent = 'Seu navegador não informa a localização. Toque no mapa para marcar o local.';
            return;
        }
        const textoOriginal = botao.innerHTML;
        botao.disabled = true;
        botao.textContent = 'Procurando sua localização...';
        navigator.geolocation.getCurrentPosition(function (posicao) {
            botao.disabled = false;
            botao.innerHTML = textoOriginal;
            marcar(posicao.coords.latitude, posicao.coords.longitude, true);
        }, function () {
            botao.disabled = false;
            botao.innerHTML = textoOriginal;
            avisoMapa.textContent = 'Não conseguimos sua localização. Toque no mapa ou escreva o endereço.';
        }, { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 });
    });

    // Se o mapa não ajudar, escrever o endereço também libera o passo 3.
    ['endereco', 'pontoReferencia'].forEach(function (id) {
        document.getElementById(id).addEventListener('input', function () {
            revelar(passoDescricao);
        });
    });

    // Preenche endereço e bairro a partir do ponto (OpenStreetMap). Sem internet, a pessoa digita.
    function buscarEndereco(lat, lng) {
        const url = 'https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&addressdetails=1'
            + '&accept-language=pt-BR&lat=' + lat + '&lon=' + lng;
        fetch(url)
            .then(function (resposta) { return resposta.json(); })
            .then(function (dados) {
                const a = dados.address || {};
                const rua = [a.road, a.house_number].filter(Boolean).join(', ');
                if (rua) document.getElementById('endereco').value = rua;
                const bairro = a.suburb || a.neighbourhood || a.quarter || a.city_district || a.village || a.hamlet;
                if (bairro) document.getElementById('bairro').value = bairro;
            })
            .catch(function () { /* segue sem endereço automático */ });
    }

    // Procura demanda igual a até 100 m. Devolve uma Promise com true se encontrou.
    function verificarDuplicadas() {
        const categoria = categoriaEscolhida();
        if (!categoria || !campoLat.value) {
            caixaDuplicada.hidden = true;
            return Promise.resolve(false);
        }
        const url = '/api/demandas/proximas?categoriaId=' + categoria.value
            + '&latitude=' + campoLat.value + '&longitude=' + campoLng.value;

        return fetch(url)
            .then(function (resposta) { return resposta.ok ? resposta.json() : []; })
            .then(function (lista) {
                caixaDuplicada.innerHTML = '';
                if (lista.length === 0) {
                    caixaDuplicada.hidden = true;
                    return false;
                }
                caixaDuplicada.appendChild(montarAviso(lista[0]));
                caixaDuplicada.hidden = false;
                caixaDuplicada.scrollIntoView({ behavior: 'smooth', block: 'center' });
                return true;
            })
            .catch(function () { return false; });
    }

    // Monta o aviso com textContent (nunca innerHTML) para o texto de outra pessoa não virar código.
    function montarAviso(d) {
        const aviso = document.getElementById('modelo-duplicada').content.cloneNode(true);
        aviso.querySelector('.dup-titulo').textContent = 'Já existe uma demanda igual a ' + d.distanciaMetros + ' m daqui';
        aviso.querySelector('.dup-descricao').textContent = '"' + d.descricao + '"';
        aviso.querySelector('.dup-detalhe').textContent =
            d.local + ' · ' + d.totalApoios + (d.totalApoios === 1 ? ' apoio' : ' apoios');
        aviso.querySelector('.dup-ver').href = '/demandas/' + d.protocolo;

        const botaoApoiar = aviso.querySelector('.dup-apoiar');
        if (form.dataset.atendente === 'true') {
            // O atendente não "apoia": ele informa o protocolo existente para quem ligou.
            botaoApoiar.remove();
            aviso.querySelector('.dup-titulo').textContent = 'Já existe uma demanda igual a ' + d.distanciaMetros
                + ' m: informe o protocolo ' + d.protocolo + ' para quem entrou em contato';
        } else if (d.minha) {
            botaoApoiar.remove();
            aviso.querySelector('.dup-titulo').textContent = 'Você já registrou uma demanda igual a ' + d.distanciaMetros + ' m daqui';
        } else if (d.jaApoiei) {
            botaoApoiar.disabled = true;
            botaoApoiar.textContent = 'Você já apoia esta';
        } else {
            const formApoio = document.getElementById('form-apoio-duplicada');
            formApoio.action = '/demandas/' + d.id + '/apoio';
            formApoio.querySelector('[name="voltar"]').value = '/demandas/' + d.protocolo;
        }
        return aviso;
    }

    // ---------- Passo 3: foto e detalhes ----------
    const campoFoto = document.getElementById('foto');
    const previa = document.getElementById('previa-foto');

    // Reduz a foto no próprio celular antes de enviar (mais rápido em internet fraca).
    // O navegador já desenha a foto "em pé", aplicando a rotação que o celular gravou.
    campoFoto.addEventListener('change', function () {
        const arquivo = campoFoto.files[0];
        if (!arquivo) return;
        previa.querySelector('img').src = URL.createObjectURL(arquivo);
        previa.hidden = false;

        if (!window.createImageBitmap || !window.DataTransfer) return;
        createImageBitmap(arquivo).then(function (imagem) {
            const escala = Math.min(1, 1600 / Math.max(imagem.width, imagem.height));
            const tela = document.createElement('canvas');
            tela.width = Math.round(imagem.width * escala);
            tela.height = Math.round(imagem.height * escala);
            tela.getContext('2d').drawImage(imagem, 0, 0, tela.width, tela.height);
            tela.toBlob(function (blob) {
                if (!blob) return;
                const lista = new DataTransfer();
                lista.items.add(new File([blob], 'foto.jpg', { type: 'image/jpeg' }));
                campoFoto.files = lista.files;
            }, 'image/jpeg', 0.85);
        }).catch(function () { /* envia o arquivo original */ });
    });

    document.getElementById('remover-foto').addEventListener('click', function () {
        campoFoto.value = '';
        previa.hidden = true;
    });

    const campoRisco = document.querySelector('input[name="riscoSeguranca"][type="checkbox"]');
    campoRisco.addEventListener('change', function () {
        document.getElementById('aviso-risco').hidden = !campoRisco.checked;
    });

    // Volta com erro de validação: reabre o mapa com o que já tinha sido escolhido.
    if (categoriaEscolhida()) iniciarMapa();
})();
