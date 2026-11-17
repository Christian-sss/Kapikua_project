/* ==========================================================================
   KAPIKUA · Comportamiento común de la aplicación
   - Menú móvil
   - Código de un solo uso: casillas que llenan el campo oculto "codigo",
     aviso con el código generado por el servidor y vencimiento de 5 min
   - Avisos en pantalla (toast)
   - Diálogos, mostrar contraseña, valor de los controles deslizantes
   Va después de theme.js.
   ========================================================================== */
(function () {
  "use strict";

  var ICONOS = "/css/img/iconos.svg#";
  var $ = function (sel, ctx) { return (ctx || document).querySelector(sel); };
  var $$ = function (sel, ctx) { return Array.prototype.slice.call((ctx || document).querySelectorAll(sel)); };

  /* ---------- Menú móvil ---------- */
  $$("[data-menu-toggle]").forEach(function (btn) {
    var menu = document.getElementById(btn.getAttribute("aria-controls"));
    if (!menu) return;
    btn.addEventListener("click", function () {
      var abierto = btn.getAttribute("aria-expanded") === "true";
      btn.setAttribute("aria-expanded", String(!abierto));
      btn.setAttribute("aria-label", abierto ? "Abrir menú" : "Cerrar menú");
      menu.hidden = abierto;
    });
  });

  /* ---------- Avisos en pantalla ---------- */
  var stack;
  function toast(titulo, texto, opciones) {
    opciones = opciones || {};
    if (!stack) {
      stack = document.createElement("div");
      stack.className = "toast-stack";
      stack.setAttribute("role", "status");
      stack.setAttribute("aria-live", "polite");
      document.body.appendChild(stack);
    }
    var el = document.createElement("div");
    el.className = "toast";
    el.innerHTML =
      '<span class="ico-box"><svg class="icon" aria-hidden="true"><use href="' + ICONOS + (opciones.icono || "i-bolt") + '"/></svg></span>' +
      '<div><b></b><small></small></div>';
    el.querySelector("b").textContent = titulo;
    if (opciones.html) el.querySelector("small").innerHTML = texto; else el.querySelector("small").textContent = texto || "";
    stack.appendChild(el);
    setTimeout(function () { el.remove(); }, opciones.duracion || 6000);
    return el;
  }

  /* ---------- Código de un solo uso ---------- */
  $$("[data-otp-box]").forEach(function (box) {
    var codigo = box.getAttribute("data-codigo");
    if (codigo && /^\d+$/.test(codigo)) {
      toast("Tu código de un solo uso", '<span class="code">' + codigo + "</span> · vence en 5 minutos",
        { icono: "i-lock", html: true, duracion: 15000 });
    }
    var timerEl = $("[data-otp-timer]", box);
    if (!timerEl) return;
    var restante = 300;
    var intervalo = setInterval(function () {
      restante -= 1;
      if (restante <= 0) {
        clearInterval(intervalo);
        timerEl.textContent = "vencido";
        return;
      }
      var m = Math.floor(restante / 60), s = restante % 60;
      timerEl.textContent = m + ":" + (s < 10 ? "0" : "") + s;
    }, 1000);
  });

  $$(".otp").forEach(function (otp) {
    var inputs = $$("input", otp);
    var oculto = otp.parentElement.querySelector('input[name="codigo"]');
    function sincronizar() {
      if (oculto) oculto.value = inputs.map(function (x) { return x.value; }).join("");
    }
    inputs.forEach(function (inp, i) {
      inp.addEventListener("input", function () {
        inp.value = inp.value.replace(/\D/g, "").slice(-1);
        if (inp.value && inputs[i + 1]) inputs[i + 1].focus();
        sincronizar();
      });
      inp.addEventListener("keydown", function (e) {
        if (e.key === "Backspace" && !inp.value && inputs[i - 1]) inputs[i - 1].focus();
      });
      inp.addEventListener("paste", function (e) {
        var txt = (e.clipboardData || window.clipboardData).getData("text").replace(/\D/g, "").slice(0, inputs.length);
        if (!txt) return;
        e.preventDefault();
        txt.split("").forEach(function (d, k) { if (inputs[k]) inputs[k].value = d; });
        (inputs[txt.length] || inputs[inputs.length - 1]).focus();
        sincronizar();
      });
    });
    var form = otp.closest("form");
    if (form) {
      form.addEventListener("submit", function (e) {
        sincronizar();
        if (inputs.some(function (x) { return !x.value; })) {
          e.preventDefault();
          toast("Ingresa el código completo", "Son 6 dígitos y vencen a los 5 minutos.", { icono: "i-info" });
          var vacio = inputs.filter(function (x) { return !x.value; })[0];
          if (vacio) vacio.focus();
        }
      });
    }
  });

  /* ---------- Diálogos ---------- */
  document.addEventListener("click", function (e) {
    var abrir = e.target.closest("[data-open-dialog]");
    var cerrar = e.target.closest("[data-close-dialog]");
    if (abrir) {
      var d = document.getElementById(abrir.getAttribute("data-open-dialog"));
      if (d && d.showModal) d.showModal();
    }
    if (cerrar) {
      var dlg = cerrar.closest("dialog");
      if (dlg) dlg.close();
    }
  });

  /* ---------- Mostrar / ocultar contraseña ---------- */
  $$("[data-toggle-password]").forEach(function (btn) {
    var input = document.getElementById(btn.getAttribute("data-toggle-password"));
    if (!input) return;
    btn.addEventListener("click", function () {
      var ver = input.type === "password";
      input.type = ver ? "text" : "password";
      btn.setAttribute("aria-label", ver ? "Ocultar contraseña" : "Mostrar contraseña");
      btn.querySelector("use").setAttribute("href", ICONOS + (ver ? "i-eye-off" : "i-eye"));
    });
  });

  /* ---------- Valor visible de un control deslizante ---------- */
  $$("[data-range-out]").forEach(function (rango) {
    var salida = document.getElementById(rango.getAttribute("data-range-out"));
    var sufijo = rango.getAttribute("data-range-sufijo") || "";
    if (!salida) return;
    function pintar() { salida.textContent = rango.value + sufijo; }
    rango.addEventListener("input", pintar);
    pintar();
  });

  $$("[data-year]").forEach(function (el) { el.textContent = new Date().getFullYear(); });

  window.KapikuaApp = { toast: toast };
})();
