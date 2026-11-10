// RNF-17: actualiza saldo, límites y movimientos sin recargar la página.
// El servidor avisa por Server-Sent Events (/eventos) y la página pide el fragmento actualizado.
(() => {
    'use strict';

    const inicial = document.getElementById('contenido');
    if (!inicial || !window.EventSource || !window.fetch) return;   // sin soporte: la página sigue funcionando igual

    const urlFragmento = inicial.dataset.fragmento;
    const urlEventos = inicial.dataset.eventos;
    let enCurso = false;
    let otraVez = false;

    async function refrescar() {
        // Si llegan varios avisos seguidos, se hace una sola petición y, al terminar, una más.
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
            // Redirigido = la sesión venció o hubo un error: se recarga la página completa.
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
            // Red caída: el próximo aviso o la reconexión lo intentarán de nuevo.
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
    // El navegador se reconecta solo. Si la conexión queda cerrada (por ejemplo, porque la sesión venció),
    // se recarga la página para que el servidor muestre el inicio de sesión.
    fuente.onerror = () => {
        if (fuente.readyState === EventSource.CLOSED) window.location.reload();
    };
    window.addEventListener('pagehide', () => fuente.close());
})();
