package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;

public record Topes(Dinero porOperacion, Dinero saldoMaximo, Dinero recargasMes, Dinero pagosMes) {
    public static Topes desdeUit(BigDecimal uit, BigDecimal factorOperacion, BigDecimal factorSaldo,
                                 BigDecimal factorRecargas, BigDecimal factorPagos) {
        var u = Dinero.de(uit);
        return new Topes(u.multiplicar(factorOperacion), u.multiplicar(factorSaldo),
                u.multiplicar(factorRecargas), u.multiplicar(factorPagos));
    }
}
