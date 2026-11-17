package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

interface TipoEnvio extends TipoTransaccion {
    @Override
    default Result<Void, ErrorBilletera> validarTopes(Billetera billetera, Dinero monto,
                                                       AcumuladoMensual acumulado, Topes topes) {
        return TipoTransaccion.super.validarTopes(billetera, monto, acumulado, topes).flatMap(ignorado -> {
            var pagos = acumulado.pagosYTransferencias();
            if (pagos.sumar(monto).mayorQue(topes.pagosMes())) {
                return Result.err(new ErrorBilletera.TopePagosMes(pagos.faltaPara(topes.pagosMes())));
            }
            return Result.ok();
        });
    }

    @Override
    default boolean cuentaComoPagoOTransferencia() {
        return true;
    }

    @Override
    default boolean generaCobroRecibido() {
        return true;
    }

    @Override
    default boolean debeObservarse(boolean esEntrada) {
        return !esEntrada;
    }
}
