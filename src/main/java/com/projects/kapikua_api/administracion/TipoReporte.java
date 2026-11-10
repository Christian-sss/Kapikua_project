package com.projects.kapikua_api.administracion;

import java.util.List;

public enum TipoReporte {
    CLIENTES("Clientes", "Estado de la billetera", List.of("ACTIVA", "BLOQUEADA")),
    TRANSACCIONES("Transacciones", "Tipo", List.of("RECARGA", "TRANSFERENCIA", "PAGO_NEGOCIO", "DESEMBOLSO", "PAGO_CUOTA", "PAGO_ANTICIPADO")),
    CREDITOS("Créditos", "Estado del préstamo", List.of("VIGENTE", "EN_MORA", "PAGADO")),
    COBRANZA("Cobranza", "Estado de la cuota", List.of("PENDIENTE", "PAGADA", "VENCIDA"));

    private final String nombre;
    private final String filtro;
    private final List<String> estados;

    TipoReporte(String nombre, String filtro, List<String> estados) {
        this.nombre = nombre;
        this.filtro = filtro;
        this.estados = estados;
    }

    public String nombre() { return nombre; }
    public String filtro() { return filtro; }
    public List<String> estados() { return estados; }
    public String ruta() { return name().toLowerCase(); }

    public String estadoValido(String estado) {
        return estado != null && estados.contains(estado) ? estado : null;
    }
}
