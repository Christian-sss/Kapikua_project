package com.projects.kapikua_api.billetera.model;

import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

public interface TipoTransaccion {
    String codigo();

    default boolean requiereCodigoDeUnUso() {
        return false;
    }

    default Result<Void, ErrorBilletera> validarTopes(
            Billetera billetera,
            Dinero monto,
            AcumuladoMensual acumulado,
            Topes topes
    ) {
        if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());

        if (monto.mayorQue(topes.porOperacion())) {
            return Result.err(new ErrorBilletera.TopePorOperacion(topes.porOperacion()));
        }
        return Result.ok();
    }

    default boolean cuentaComoPagoOTransferencia() {
        return false;
    }

    default boolean esPagoCredito() {
        return false;
    }

    default boolean generaCobroRecibido() {
        return false;
    }

    default boolean debeObservarse(boolean esEntrada) {
        return false;
    }

    default ErrorBilletera destinatarioNoEncontrado() {
        return new ErrorBilletera.DestinatarioNoEncontrado();
    }

    default Result<Void, ErrorBilletera> validarDestinatario(Billetera destinatario) {
        return Result.ok();
    }

    default String descripcion(Billetera destinatario) {
        return null;
    }
}
