/* KAPIKUA · Tiempo real en las pantallas de crédito (RNF-17).
   Escucha los avisos del servidor (Server-Sent Events) y, cuando cambian el saldo apartado, las cuotas,
   el préstamo o la oferta, vuelve a pedir la misma página y reemplaza su contenido sin recargarla.
   Si el usuario está escribiendo en un formulario, espera a que termine para no borrarle lo escrito. */
(function () {
  "use strict";

  var main = document.querySelector("[data-en-vivo]");
  if (!main || !window.EventSource || !window.fetch || !window.DOMParser) return;

  var urlEventos = main.getAttribute("data-en-vivo");
  var pendiente = false, enCurso = false;

  function escribiendo() {
    var el = document.activeElement;
    return el && main.contains(el) && /^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName);
  }

  function refrescar() {
    if (escribiendo() || enCurso) { pendiente = true; return; }
    enCurso = true;
    fetch(window.location.href, { credentials: "same-origin", headers: { Accept: "text/html" } })
      .then(function (r) {
        if (r.redirected || !r.ok) { window.location.reload(); return null; }
        return r.text();
      })
      .then(function (html) {
        if (!html) return;
        var nuevo = new DOMParser().parseFromString(html, "text/html").querySelector("[data-en-vivo]");
        if (nuevo) {
          main.innerHTML = nuevo.innerHTML;
          main.classList.remove("is-updated"); void main.offsetWidth; main.classList.add("is-updated");
        }
      })
      .catch(function () {})
      .then(function () {
        enCurso = false;
        if (pendiente && !escribiendo()) { pendiente = false; refrescar(); }
      });
  }

  document.addEventListener("focusout", function () {
    setTimeout(function () { if (pendiente && !escribiendo()) { pendiente = false; refrescar(); } }, 0);
  });

  var fuente = new EventSource(urlEventos);
  fuente.addEventListener("credito", refrescar);
  fuente.addEventListener("billetera", refrescar);
  window.addEventListener("pagehide", function () { fuente.close(); });
})();
