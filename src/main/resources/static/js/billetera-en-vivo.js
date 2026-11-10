(() => {
    'use strict';

    const inicial = document.getElementById('contenido');
    if (!inicial || !window.EventSource || !window.fetch) return;

    const urlFragmento = inicial.dataset.fragmento;
    const urlEventos = inicial.dataset.eventos;
    let enCurso = false;
    let otraVez = false;

    async function refrescar() {
        if (enCurso) {
            otraVez = true;
            return;
        }
        enCurso = true;
        try {
            const respuesta = await fetch(urlFragmento, {
                credentials: 'same-origin',
                headers: {'Accept': 'text/html'}
            });
            if (respuesta.redirected || !respuesta.ok) {
                window.location.reload();
                return;
            }
            const plantilla = document.createElement('template');
            plantilla.innerHTML = (await respuesta.text()).trim();
            const nuevo = plantilla.content.firstElementChild;
            const actual = document.getElementById('contenido');
            if (nuevo && actual) actual.replaceWith(nuevo);
        } catch (e) {
        } finally {
            enCurso = false;
            if (otraVez) {
                otraVez = false;
                refrescar();
            }
        }
    }

    const fuente = new EventSource(urlEventos);
    fuente.addEventListener('billetera', refrescar);
    fuente.onerror = () => {
        if (fuente.readyState === EventSource.CLOSED) window.location.reload();
    };
    window.addEventListener('pagehide', () => fuente.close());
})();
