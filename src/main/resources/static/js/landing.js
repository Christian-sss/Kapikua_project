/* KAPIKUA · Comportamiento de la landing page */
(function () {
  "use strict";

  var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  /* ---------- Encabezado: borde al hacer scroll ---------- */
  var header = document.querySelector(".site-header");
  function onScroll() { header.classList.toggle("is-scrolled", window.scrollY > 8); }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ---------- Menú móvil ---------- */
  var menuBtn = document.querySelector("[data-menu-toggle]");
  var menu = document.getElementById("menu-movil");

  function setMenu(abierto) {
    menuBtn.setAttribute("aria-expanded", String(abierto));
    menuBtn.setAttribute("aria-label", abierto ? "Cerrar menú" : "Abrir menú");
    menu.hidden = !abierto;
  }
  menuBtn.addEventListener("click", function () {
    setMenu(menuBtn.getAttribute("aria-expanded") !== "true");
  });
  menu.addEventListener("click", function (e) {
    if (e.target.closest("a")) setMenu(false);
  });
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && !menu.hidden) { setMenu(false); menuBtn.focus(); }
  });
  window.matchMedia("(min-width: 1280px)").addEventListener("change", function (mq) {
    if (mq.matches) setMenu(false);
  });

  /* ---------- Menú en píldora: indicador activo (scroll) + indicador de hover ---------- */
  var pillNav = document.querySelector("[data-pill-nav]");
  var pillActive = pillNav.querySelector(".nav-pill-active");
  var pillHover = pillNav.querySelector(".nav-pill-hover");
  var navLinks = Array.prototype.slice.call(pillNav.querySelectorAll("a"));
  var allLinks = Array.prototype.slice.call(document.querySelectorAll('.main-nav a, .mobile-menu a[href^="#"]:not(.btn)'));
  var secciones = navLinks
    .map(function (a) { return document.querySelector(a.getAttribute("href")); })
    .filter(Boolean);
  var idActivo = null;
  var bloqueoSpy = false;

  function moverPill(pill, link) {
    if (!link) { pill.style.opacity = "0"; return; }
    pill.style.width = link.offsetWidth + "px";
    pill.style.transform = "translateX(" + link.offsetLeft + "px)";
    pill.style.opacity = "1";
  }

  function linkActivo() {
    return navLinks.filter(function (a) { return a.getAttribute("href") === "#" + idActivo; })[0];
  }

  function activar(id) {
    if (id === idActivo) return;
    idActivo = id;
    allLinks.forEach(function (a) {
      var activo = a.getAttribute("href") === "#" + id;
      a.classList.toggle("is-active", activo);
      if (activo) a.setAttribute("aria-current", "true"); else a.removeAttribute("aria-current");
    });
    moverPill(pillActive, linkActivo());
  }

  // La sección activa es la última cuyo inicio ya pasó el 35% superior de la pantalla.
  function seccionVisible() {
    var limite = window.innerHeight * 0.35;
    var actual = secciones[0];
    secciones.forEach(function (s) { if (s.getBoundingClientRect().top <= limite) actual = s; });
    // Al llegar al final de la página se marca la última sección aunque sea corta.
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2) {
      actual = secciones[secciones.length - 1];
    }
    return actual.id;
  }

  var spyPendiente = false;
  window.addEventListener("scroll", function () {
    if (bloqueoSpy || spyPendiente) return;
    spyPendiente = true;
    requestAnimationFrame(function () { spyPendiente = false; activar(seccionVisible()); });
  }, { passive: true });

  // Al hacer clic, la píldora va directo al destino sin recorrer las secciones intermedias.
  var finScroll;
  function liberarSpy() {
    clearTimeout(finScroll);
    finScroll = setTimeout(function () { bloqueoSpy = false; activar(seccionVisible()); }, 150);
  }
  allLinks.forEach(function (a) {
    a.addEventListener("click", function () {
      bloqueoSpy = true;
      activar(a.getAttribute("href").slice(1));
      window.addEventListener("scroll", liberarSpy, { passive: true });
      liberarSpy();
    });
  });

  navLinks.forEach(function (a) {
    a.addEventListener("mouseenter", function () { moverPill(pillHover, a); });
    a.addEventListener("focus", function () { moverPill(pillHover, a); });
  });
  pillNav.addEventListener("mouseleave", function () { pillHover.style.opacity = "0"; });
  pillNav.addEventListener("focusout", function (e) {
    if (!pillNav.contains(e.relatedTarget)) pillHover.style.opacity = "0";
  });

  function remedir() { moverPill(pillActive, linkActivo()); }
  window.addEventListener("resize", remedir);
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(remedir);

  activar(seccionVisible());
  requestAnimationFrame(function () { pillNav.classList.add("is-ready"); });

  /* ---------- QR decorativo (no codifica datos reales) ---------- */
  function dibujarQR(svg) {
    var N = 21, ns = "http://www.w3.org/2000/svg", seed = 7;
    function rnd() { seed = (seed * 16807) % 2147483647; return seed / 2147483647; }
    function enOjo(x, y) {
      return (x < 8 && y < 8) || (x > N - 9 && y < 8) || (x < 8 && y > N - 9);
    }
    function rect(x, y, w, h, cls) {
      var r = document.createElementNS(ns, "rect");
      r.setAttribute("x", x); r.setAttribute("y", y);
      r.setAttribute("width", w); r.setAttribute("height", h);
      if (cls) r.setAttribute("class", cls);
      svg.appendChild(r);
    }
    [[0, 0], [N - 7, 0], [0, N - 7]].forEach(function (p) {
      var x = p[0], y = p[1];
      rect(x, y, 7, 1, "qr-eye"); rect(x, y + 6, 7, 1, "qr-eye");
      rect(x, y, 1, 7, "qr-eye"); rect(x + 6, y, 1, 7, "qr-eye");
      rect(x + 2, y + 2, 3, 3, "qr-eye");
    });
    for (var y = 0; y < N; y++) {
      for (var x = 0; x < N; x++) {
        if (!enOjo(x, y) && rnd() > 0.52) rect(x, y, 1, 1);
      }
    }
  }
  document.querySelectorAll("[data-qr]").forEach(dibujarQR);

  /* ---------- Demostración de transferencia en tiempo real ---------- */
  var notif = document.querySelector("[data-notif]");
  var notifAmt = document.querySelector("[data-notif-amt]");
  var notifName = document.querySelector("[data-notif-name]");
  var movNew = document.querySelector("[data-mov-new]");
  var movName = document.querySelector("[data-mov-name]");
  var movAmt = document.querySelector("[data-mov-amt]");
  var balanceEl = document.querySelector("[data-balance]");

  var saldo = 1250;
  var entrantes = [
    { nombre: "Carlos M***", monto: 50 },
    { nombre: "Rosa T***", monto: 120 },
    { nombre: "Diego P***", monto: 25.5 }
  ];
  var i = 0;
  var formato = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  function soles(n) { return "S/ " + formato.format(n); }

  function reiniciarAnimacion(el, clase) {
    el.classList.remove(clase);
    void el.offsetWidth; // fuerza el reflujo para repetir la animación
    el.classList.add(clase);
  }

  function recibir() {
    var t = entrantes[i % entrantes.length];
    i++;
    saldo += t.monto;

    notifAmt.textContent = soles(t.monto);
    notifName.textContent = t.nombre;
    movName.textContent = t.nombre;
    movAmt.textContent = "+ " + soles(t.monto);
    balanceEl.textContent = soles(saldo);

    movNew.hidden = false;
    reiniciarAnimacion(notif, "is-visible");
    reiniciarAnimacion(movNew, "is-new");
    reiniciarAnimacion(balanceEl, "is-updated");
    setTimeout(function () { balanceEl.classList.remove("is-updated"); }, 1400);
  }

  if (reduceMotion) {
    recibir();
  } else {
    setTimeout(recibir, 1200);
    setInterval(function () { if (!document.hidden) recibir(); }, 5200);
  }

  /* ---------- Aparición progresiva al hacer scroll ---------- */
  var revelables = document.querySelectorAll(
    ".feature, .step, .trust, .faq details, .split-copy, .eval-card, .section-head"
  );
  if (!reduceMotion && "IntersectionObserver" in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) {
          en.target.classList.add("is-visible");
          io.unobserve(en.target);
        }
      });
    }, { threshold: 0.15, rootMargin: "0px 0px -40px 0px" });
    revelables.forEach(function (el, n) {
      el.classList.add("reveal");
      el.style.transitionDelay = (n % 4) * 70 + "ms";
      io.observe(el);
    });
  } else {
    document.querySelectorAll(".eval-card").forEach(function (el) { el.classList.add("is-visible"); });
  }

  /* ---------- Año del pie ---------- */
  var year = document.querySelector("[data-year]");
  if (year) year.textContent = new Date().getFullYear();
})();
