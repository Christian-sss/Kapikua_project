package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;

final class CuotaVencida extends EstadoCuota {
    CuotaVencida() {
        super("VENCIDA");
    }

    @Override
    boolean estaVencida() { return true; }

    @Override
    Result<Cuota.PagoImputado, ErrorCredito> imputar(Cuota cuota, Dinero monto, Dinero deApartado, LocalDate hoy) {
        return cuota.aplicarImputacion(monto, deApartado, hoy);
    }
}
