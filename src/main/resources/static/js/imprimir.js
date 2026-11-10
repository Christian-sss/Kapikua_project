document.querySelectorAll('[data-imprimir]').forEach(function (boton) {
    boton.addEventListener('click', function () { window.print(); });
});
