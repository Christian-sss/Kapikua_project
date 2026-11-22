package com.projects.kapikua_api.billetera.model;

import java.util.List;

public final class TiposTransaccion {
    public static final TipoTransaccion CARGA_INICIAL = new TipoCargaInicial();
    public static final TipoTransaccion TRANSFERENCIA = new TipoTransferencia();
    public static final TipoTransaccion PAGO_NEGOCIO = new TipoPagoNegocio();
    public static final TipoTransaccion DESEMBOLSO = new TipoDesembolso();
    public static final TipoTransaccion PAGO_CUOTA = new TipoPagoCredito("PAGO_CUOTA");
    public static final TipoTransaccion PAGO_ANTICIPADO = new TipoPagoCredito("PAGO_ANTICIPADO");

    private static final List<TipoTransaccion> TIPOS = List.of(CARGA_INICIAL, TRANSFERENCIA, PAGO_NEGOCIO,
            DESEMBOLSO, PAGO_CUOTA, PAGO_ANTICIPADO);

    private TiposTransaccion() {
    }

    public static List<TipoTransaccion> todos() {
        return TIPOS;
    }

    public static TipoTransaccion desdeCodigo(String codigo) {
        return TIPOS.stream().filter(tipo -> tipo.codigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de transacción desconocido: " + codigo));
    }
}
