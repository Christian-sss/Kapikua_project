/* KAPIKUA · Modo claro / oscuro (compartido por todas las páginas).
   El modo claro es el predeterminado. El oscuro solo se activa cuando el usuario
   lo elige con el botón, y esa elección se guarda en su navegador. */
(function () {
  "use strict";

  var STORAGE_KEY = "kapikua-theme";
  var root = document.documentElement;

  function temaActual() {
    return root.getAttribute("data-theme") === "dark" ? "dark" : "light";
  }

  function guardar(tema) {
    try { localStorage.setItem(STORAGE_KEY, tema); } catch (e) { /* almacenamiento no disponible */ }
  }

  function actualizarBotones() {
    var oscuro = temaActual() === "dark";
    document.querySelectorAll("[data-theme-toggle]").forEach(function (btn) {
      btn.setAttribute("aria-label", oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
      btn.setAttribute("aria-pressed", String(oscuro));
    });
  }

  function aplicar(tema) {
    root.setAttribute("data-theme", tema);
    guardar(tema);
    actualizarBotones();
  }

  document.querySelectorAll("[data-theme-toggle]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      aplicar(temaActual() === "dark" ? "light" : "dark");
    });
  });

  actualizarBotones();
  window.KapikuaTheme = { actual: temaActual, aplicar: aplicar };
})();
