package com.projects.kapikua_api.billetera.domain.service;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.AcumuladoMensual;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

public final class TopesSBS {
    private TopesSBS() {
    }

    public static Result<Void, ErrorBilletera> validar(TipoTransaccion tipo, Billetera billetera, Dinero monto,
                                                        AcumuladoMensual acumulado, Topes topes) {
        if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
        return tipo.validarTopes(billetera, monto, acumulado, topes);
    }

    public static Result<Void, ErrorBilletera> validarEnvio(TipoTransaccion tipo, Billetera origen, Billetera destino,
                                                            Dinero monto, AcumuladoMensual acumuladoOrigen, Topes topes) {
        if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
        return origen.validarSalida()
                .flatMap(ignorado -> destino.validarRecepcion(true))
                .flatMap(ignorado -> validarBilleterasDistintas(origen, destino))
                .flatMap(ignorado -> tipo.validarTopes(origen, monto, acumuladoOrigen, topes))
                .flatMap(ignorado -> validarSaldoDisponible(origen, monto))
                .flatMap(ignorado -> destino.validarAcreditacion(monto, topes.saldoMaximo(), true));
    }

    private static Result<Void, ErrorBilletera> validarBilleterasDistintas(Billetera origen, Billetera destino) {
        if (origen.getId() != null && origen.getId().equals(destino.getId())) {
            return Result.err(new ErrorBilletera.OperacionConSiMismo());
        }
        return Result.ok();
    }

    private static Result<Void, ErrorBilletera> validarSaldoDisponible(Billetera origen, Dinero monto) {
        if (origen.saldoDisponible().menorQue(monto)) return Result.err(new ErrorBilletera.SaldoInsuficiente());
        return Result.ok();
    }
}
