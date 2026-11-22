package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;
import java.util.List;

final class ModalidadTotal extends ModalidadPago {
    ModalidadTotal() {
        super("TOTAL");
    }

    @Override
    public boolean esLiquidacionTotal() {
        return true;
    }

    @Override
    public Dinero montoACobrar(Prestamo prestamo, Dinero montoSolicitado) {
        return prestamo.montoAnticipadoTotal();
    }

    @Override
    public Dinero apartadoAUsar(Prestamo prestamo) {
        return prestamo.totalApartado();
    }

    @Override
    Result<Void, ErrorCredito> verificarAnticipado(Prestamo prestamo, Dinero monto) {
        return prepararLiquidacion(prestamo, monto).map(pendientes -> null);
    }

    @Override
    Result<Dinero, ErrorCredito> aplicarAnticipado(Prestamo prestamo, Dinero monto, Dinero deApartado,
                                                  OffsetDateTime ahora) {
        return prepararLiquidacion(prestamo, monto)
                .flatMap(pendientes -> anularCuotas(pendientes, ahora))
                .map(liberado -> deApartado.faltaPara(liberado));
    }

    private Result<List<Cuota>, ErrorCredito> prepararLiquidacion(Prestamo prestamo, Dinero monto) {
        var capital = prestamo.montoAnticipadoTotal();
        if (capital.esCero()) return Result.err(new ErrorCredito.NadaQuePagar());
        if (!monto.equals(capital)) return Result.err(new ErrorCredito.MontoLiquidacionInvalido(capital.toString()));
        return cuotasReprogramables(prestamo);
    }
}
