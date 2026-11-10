// Botón «Imprimir o guardar PDF» de los reportes (CU-16): abre el diálogo de impresión del navegador,
// donde se elige la impresora o «Guardar como PDF». Los estilos @media print ocultan menús y botones.
document.querySelectorAll('[data-imprimir]').forEach(function (boton) {
    boton.addEventListener('click', function () { window.print(); });
});
