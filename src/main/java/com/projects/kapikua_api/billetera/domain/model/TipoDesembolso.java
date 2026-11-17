package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

public final class TipoDesembolso implements TipoTransaccion {
    @Override
    public String codigo() {
        return "DESEMBOLSO";
    }

    @Override
    public Result<Void, ErrorBilletera> validarTopes(Billetera billetera, Dinero monto,
                                                     AcumuladoMensual acumulado, Topes topes) {
        return TipoTransaccion.super.validarTopes(billetera, monto, acumulado, topes)
                .flatMap(ignorado -> billetera.validarAcreditacion(monto, topes.saldoMaximo(), false));
    }
}
