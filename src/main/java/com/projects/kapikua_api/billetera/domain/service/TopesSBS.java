package com.projects.kapikua_api.billetera.domain.service;

import com.projects.kapikua_api.billetera.domain.model.*;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

public final class TopesSBS {
    private TopesSBS() {
    }

    public static Result<Void, ErrorBilletera> validarRecarga(Billetera destino, Dinero monto,
                                                              AcumuladoMensual acumulado, Topes topes) {
        if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
        if (!destino.estaActiva()) return Result.err(new ErrorBilletera.BilleteraBloqueada());
        if (monto.mayorQue(topes.porOperacion()))
            return Result.err(new ErrorBilletera.TopePorOperacion(topes.porOperacion()));
        if (acumulado.recargas().sumar(monto).mayorQue(topes.recargasMes())) {
            return Result.err(new ErrorBilletera.TopeRecargasMes(acumulado.recargas().faltaPara(topes.recargasMes())));
        }
        if (destino.getSaldo().sumar(monto).mayorQue(topes.saldoMaximo())) {
            return Result.err(new ErrorBilletera.TopeSaldoPropio(destino.puedeRecibir(topes.saldoMaximo())));
        }
        return Result.ok();
    }

    public static Result<Void, ErrorBilletera> validarEnvio(Billetera origen, Billetera destino, Dinero monto,
                                                            AcumuladoMensual acumuladoOrigen, Topes topes) {
        if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
        if (!origen.estaActiva()) return Result.err(new ErrorBilletera.BilleteraBloqueada());
        if (!destino.estaActiva()) return Result.err(new ErrorBilletera.DestinatarioNoEncontrado());
        if (origen.getId() != null && origen.getId().equals(destino.getId())) {
            return Result.err(new ErrorBilletera.OperacionConSiMismo());
        }
        if (monto.mayorQue(topes.porOperacion()))
            return Result.err(new ErrorBilletera.TopePorOperacion(topes.porOperacion()));
        if (acumuladoOrigen.pagosYTransferencias().sumar(monto).mayorQue(topes.pagosMes())) {
            return Result.err(new ErrorBilletera.TopePagosMes(
                    acumuladoOrigen.pagosYTransferencias().faltaPara(topes.pagosMes())));
        }
        if (origen.saldoDisponible().menorQue(monto)) return Result.err(new ErrorBilletera.SaldoInsuficiente());
        if (destino.getSaldo().sumar(monto).mayorQue(topes.saldoMaximo())) {
            return Result.err(new ErrorBilletera.DestinoNoPuedeRecibir());
        }
        return Result.ok();
    }
}
