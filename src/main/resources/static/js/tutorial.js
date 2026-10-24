// Tour guiado da primeira visita (Driver.js). Cada perfil tem os seus passos.
(function () {
    const corpo = document.body;
    if (corpo.dataset.mostrarTutorial !== 'true' || !window.driver) return;

    const PASSOS = {
        CIDADAO: [
            { popover: { title: 'Bem-vindo ao Denarco', description: 'Aqui você avisa a Prefeitura sobre problemas na cidade e acompanha a solução. Vamos dar uma olhada rápida? Leva 1 minuto.' } },
            { element: '.js-nova-demanda', popover: { title: 'Relatar um problema', description: 'Buraco, poste apagado, lixo... Toque aqui e registre em até 5 cliques.' } },
            { element: '#resumo', popover: { title: 'Seus números', description: 'Quantas demandas suas estão abertas, quantas foram resolvidas e quantos apoios elas receberam.' } },
            { element: '#minhas-abertas', popover: { title: 'Minhas demandas', description: 'A cor mostra em que etapa cada uma está. Toque para ver a linha do tempo.' } },
            { element: '#outras-demandas', popover: { title: 'Problemas de outras pessoas', description: 'Se algo também incomoda você, toque em "Ainda presente". Isso aumenta a prioridade.' } },
            { element: '.acessibilidade', popover: { title: 'Acessibilidade', description: 'Aumente a letra ou ative o alto contraste quando quiser.' } },
            { element: '#menu-usuario', popover: { title: 'Rever o tutorial', description: 'No seu menu você pode rever este tour e sair da conta. Pronto, é só começar!' } }
        ],
        SERVIDOR: [
            { popover: { title: 'Bem-vindo à equipe', description: 'Esta é a fila do seu setor: tudo o que a população relatou e que espera atendimento.' } },
            { element: '#resumo-fila', popover: { title: 'Números do setor', description: 'Em aberto, sem responsável, atrasadas e concluídas. Toque num número para filtrar a fila.' } },
            { element: '.abas', popover: { title: 'Minhas tarefas', description: 'Aqui ficam as demandas em que você é o responsável.' } },
            { element: '.item-fila', popover: { title: 'Prioridade primeiro', description: 'O número à esquerda é a prioridade. Abra a demanda para iniciar, registrar o que foi feito e concluir.' } },
            { element: '.js-nav-registrar', popover: { title: 'Atendimento por telefone', description: 'Recebeu um pedido por telefone, no balcão ou por WhatsApp? Registre aqui: ele entra na mesma fila.' } },
            { element: '.js-nav-mapa', popover: { title: 'Mapa', description: 'Veja onde estão as demandas da cidade.' } },
            { element: '#menu-usuario', popover: { title: 'Seu menu', description: 'Rever este tutorial, suas próprias demandas e sair. Bom trabalho!' } }
        ],
        GESTOR: [
            { popover: { title: 'Bem-vindo ao painel', description: 'Aqui você acompanha o setor e decide prioridades. Vamos ver o essencial em 1 minuto.' } },
            { element: '#kpis', popover: { title: 'Números do período', description: 'Em aberto, atrasadas, concluídas e o tempo médio até concluir. Mude o período no topo.' } },
            { element: '#consulta-rapida', popover: { title: 'Pergunte ao Denarco', description: 'Escreva como você falaria, por exemplo "iluminação atrasada no Centro". O sistema mostra o que entendeu.' } },
            { element: '#cartao-categorias', popover: { title: 'Onde está o volume', description: 'Quais tipos de problema mais esperam solução. Abra "Ver em tabela" para ler os números.' } },
            { element: '#ranking-bairros', popover: { title: 'Bairros', description: 'As concentrações no território. O mapa mostra cada ponto.' } },
            { element: '.js-nav-fila', popover: { title: 'Fila e triagem', description: 'Na fila você abre cada demanda para definir responsável, prazo e prioridade.' } },
            { element: '.js-nav-equipe', popover: { title: 'Equipe', description: 'Aprove pedidos de acesso, convide pessoas e mude perfis.' } },
            { element: '#menu-usuario', popover: { title: 'Seu menu', description: 'Rever este tutorial a qualquer momento. Bom trabalho!' } }
        ]
    };
    // O admin vê o tour do gestor e mais um passo sobre as configurações.
    PASSOS.ADMIN = PASSOS.GESTOR.slice(0, -1).concat([
        { element: '.js-nav-config', popover: { title: 'Configurações', description: 'Cadastre setores e tipos de problema, com setor padrão e prazo sugerido.' } },
        PASSOS.GESTOR[PASSOS.GESTOR.length - 1]
    ]);

    // Usa o primeiro elemento visível (ex.: no celular o botão "Nova demanda" fica na barra de baixo).
    // Se o elemento estiver escondido, o passo aparece no centro da tela.
    function prepararPasso(passo) {
        if (!passo.element) return passo;
        const visivel = Array.from(document.querySelectorAll(passo.element)).find(function (el) {
            return el.offsetParent !== null;
        });
        return visivel ? { element: visivel, popover: passo.popover } : { popover: passo.popover };
    }

    const passos = (PASSOS[corpo.dataset.perfil] || PASSOS.CIDADAO).map(prepararPasso);

    // Nos passos centralizados o Driver.js cria um elemento "fantasma" com atributos ARIA que não valem
    // para uma div vazia; tiramos para não confundir leitores de tela.
    function limparFantasma() {
        const fantasma = document.getElementById('driver-dummy-element');
        if (fantasma) {
            ['aria-haspopup', 'aria-expanded', 'aria-controls'].forEach(function (a) { fantasma.removeAttribute(a); });
        }
    }

    function salvarConclusao() {
        const token = document.querySelector('meta[name="csrf-token"]').content;
        const cabecalho = document.querySelector('meta[name="csrf-header"]').content;
        fetch('/tutorial/concluido', { method: 'POST', headers: { [cabecalho]: token } });
    }

    const tour = window.driver.js.driver({
        steps: passos,
        showProgress: true,
        popoverClass: 'tour-denarco',
        nextBtnText: 'Próximo',
        prevBtnText: 'Voltar',
        doneBtnText: 'Começar',
        onPopoverRender: function (popover, opcoes) {
            const atual = opcoes.state.activeIndex;
            // Bolinhas de progresso no lugar do texto "1 de 7"; o leitor de tela ouve "Passo 1 de 7".
            popover.progress.innerHTML = '<span class="visually-hidden">Passo ' + (atual + 1) + ' de ' + passos.length + '</span>'
                + passos.map(function (_, i) {
                    return '<span class="bolinha' + (i === atual ? ' ativa' : '') + '" aria-hidden="true"></span>';
                }).join('');

            const pular = document.createElement('button');
            pular.type = 'button';
            pular.className = 'tour-pular';
            pular.textContent = 'Pular tutorial';
            pular.addEventListener('click', function () { tour.destroy(); });
            popover.footer.prepend(pular);
            setTimeout(limparFantasma, 0);
        },
        onHighlighted: limparFantasma,
        // Terminar, pular ou fechar: em todos os casos o tour não aparece mais sozinho.
        onDestroyed: salvarConclusao
    });
    tour.drive();
})();
