// Recursos usados em todas as telas: tamanho da letra, alto contraste e "mostrar senha".
(function () {
    const TAMANHOS = [100, 112.5, 125, 137.5];

    // O navegador pode bloquear o localStorage (modo anônimo); nesse caso só não lembramos a escolha.
    function ler(chave, padrao) {
        try { return localStorage.getItem(chave) ?? padrao; } catch (e) { return padrao; }
    }
    function salvar(chave, valor) {
        try { localStorage.setItem(chave, valor); } catch (e) { /* sem memória, sem problema */ }
    }

    let indiceFonte = Number(ler('denarco-fonte', '0')) || 0;
    let contraste = ler('denarco-contraste', '0') === '1';

    function aplicar() {
        document.documentElement.style.fontSize = TAMANHOS[indiceFonte] + '%';
        document.body.classList.toggle('alto-contraste', contraste);
        document.querySelectorAll('[data-acao="contraste"]').forEach(function (botao) {
            botao.setAttribute('aria-pressed', String(contraste));
        });
    }

    document.addEventListener('click', function (evento) {
        const botao = evento.target.closest('[data-acao]');
        if (botao) {
            const acao = botao.dataset.acao;
            if (acao === 'aumentar' && indiceFonte < TAMANHOS.length - 1) indiceFonte++;
            if (acao === 'diminuir' && indiceFonte > 0) indiceFonte--;
            if (acao === 'contraste') contraste = !contraste;
            salvar('denarco-fonte', String(indiceFonte));
            salvar('denarco-contraste', contraste ? '1' : '0');
            aplicar();
            // Avisa quem desenha gráficos e mapas para trocar as cores.
            document.dispatchEvent(new CustomEvent('denarco:acessibilidade'));
        }

        const olho = evento.target.closest('[data-mostrar-senha]');
        if (olho) {
            const campo = document.getElementById(olho.dataset.mostrarSenha);
            const mostrando = campo.type === 'text';
            campo.type = mostrando ? 'password' : 'text';
            olho.setAttribute('aria-label', mostrando ? 'Mostrar senha' : 'Esconder senha');
            olho.querySelector('i').className = mostrando ? 'bi bi-eye' : 'bi bi-eye-slash';
        }
    });

    aplicar();
})();
