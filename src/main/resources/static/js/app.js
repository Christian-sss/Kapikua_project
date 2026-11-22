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

  /* ---------- Menú con resaltado deslizante y cambio de página hacia el costado ----------
     El resaltado sigue al mouse y vuelve a la opción activa. Al elegir una opción que está
     a la derecha, la pantalla actual sale por la izquierda y la nueva entra por la derecha;
     hacia la izquierda, al revés. */
  function deslizante(nav, ul, marca) {
    var enlaces = $$("a", ul);
    if (!ul || !enlaces.length) return;
    var activo = enlaces.filter(function (a) { return a.getAttribute(marca[0]) === marca[1]; })[0] || null;
    var indicador = document.createElement("span");
    indicador.className = "nav-indicador";
    indicador.setAttribute("aria-hidden", "true");
    ul.insertBefore(indicador, ul.firstChild);
    nav.classList.add("con-indicador");

    function mover(a, sinAnimar) {
      enlaces.forEach(function (x) { x.classList.toggle("is-hover", x === a && x !== activo); });
      if (!a) { indicador.style.opacity = "0"; return; }
      var base = ul.getBoundingClientRect(), r = a.getBoundingClientRect();
      if (sinAnimar) indicador.style.transition = "none";
      indicador.style.opacity = "1";
      indicador.style.width = r.width + "px";
      indicador.style.height = r.height + "px";
      indicador.style.transform = "translate(" + (r.left - base.left - ul.clientLeft) + "px," + (r.top - base.top - ul.clientTop) + "px)";
      if (sinAnimar) { void indicador.offsetWidth; indicador.style.transition = ""; }
    }

    mover(activo, true);
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(function () { mover(activo, true); });
    window.addEventListener("resize", function () { mover(activo, true); });
    enlaces.forEach(function (a) {
      a.addEventListener("mouseenter", function () { mover(a); });
      a.addEventListener("focus", function () { mover(a); });
    });
    ul.addEventListener("mouseleave", function () { mover(activo); });

    var reducido = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    enlaces.forEach(function (a, i) {
      a.addEventListener("click", function (e) {
        if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey || a === activo) return;
        var desde = enlaces.indexOf(activo);
        var dir = desde === -1 || i > desde ? "derecha" : "izquierda";
        try { sessionStorage.setItem("kapikua-entrada", dir); } catch (err) {}
        mover(a);
        var main = $(".app-main");
        if (!main || reducido) return;
        e.preventDefault();
        main.classList.add(dir === "derecha" ? "sale-izquierda" : "sale-derecha");
        setTimeout(function () { window.location.href = a.href; }, 220);
      });
    });
  }
  $$(".app-nav").forEach(function (nav) { deslizante(nav, $("ul", nav), ["aria-current", "page"]); });
  /* Pestañas con enlaces (p. ej. los reportes de administración): mismo resaltado y transición. */
  $$(".segmented[role='tablist']").forEach(function (seg) {
    if ($$("a", seg).length) deslizante(seg, seg, ["aria-selected", "true"]);
  });

  /* ---------- Enlaces con el mismo cambio de página hacia el costado ----------
     data-transicion="izquierda": la pantalla sale por la derecha y la nueva entra por la izquierda (volver). */
  $$("a[data-transicion]").forEach(function (a) {
    a.addEventListener("click", function (e) {
      if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
      var dir = a.getAttribute("data-transicion") === "izquierda" ? "izquierda" : "derecha";
      try { sessionStorage.setItem("kapikua-entrada", dir); } catch (err) {}
      var main = $(".app-main");
      if (!main || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
      e.preventDefault();
      main.classList.add(dir === "derecha" ? "sale-izquierda" : "sale-derecha");
      setTimeout(function () { window.location.href = a.href; }, 220);
    });
  });

  /* ---------- Pago anticipado: el monto solo aparece en los pagos parciales ---------- */
  $$("[data-monto-parcial]").forEach(function (campo) {
    var form = campo.closest("form");
    var input = $("input", campo);
    function actualizar() {
      var elegido = form && $("input[name='modalidad']:checked", form);
      var parcial = !!elegido && elegido.value !== "TOTAL";
      campo.hidden = !parcial;
      if (input) { input.required = parcial; if (!parcial) input.value = ""; }
      var etiqueta = $("label", campo);
      var texto = parcial && etiqueta && etiqueta.getAttribute("data-etiqueta-" + elegido.value.toLowerCase());
      if (texto) etiqueta.textContent = texto;
    }
    if (form) $$("input[name='modalidad']", form).forEach(function (r) { r.addEventListener("change", actualizar); });
    actualizar();
  });

  /* ---------- Montos: máximo dos decimales (10.00 o 10.5, nunca 10.000000) ---------- */
  $$("input[type='number'][step='0.01']").forEach(function (campo) {
    function recortar() {
      var v = campo.value;
      var punto = v.indexOf(".");
      if (punto !== -1 && v.length - punto - 1 > 2) campo.value = v.slice(0, punto + 3);
    }
    campo.addEventListener("input", recortar);
    recortar();
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
