package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.MatematicaFinanciera;

import java.math.BigDecimal;
import java.util.List;

final class ModalidadReducirCuota extends ModalidadAnticipadaParcial {
    ModalidadReducirCuota() {
        super("REDUCIR_CUOTA");
    }

    @Override
    List<CalculadoraFrancesa.Tramo> calcularCronograma(Dinero saldo, BigDecimal tasa, List<Cuota> pendientes) {
        int plazo = pendientes.size();
        var cuota = MatematicaFinanciera.cuota(saldo, tasa, plazo);
        return CalculadoraFrancesa.amortizar(saldo, tasa, cuota, plazo);
    }
}
