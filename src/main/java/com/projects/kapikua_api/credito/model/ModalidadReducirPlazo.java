package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.model.CalculadoraFrancesa;
import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.util.List;

final class ModalidadReducirPlazo extends ModalidadAnticipadaParcial {
    ModalidadReducirPlazo() {
        super("REDUCIR_PLAZO");
    }

    @Override
    List<CalculadoraFrancesa.Tramo> calcularCronograma(Dinero saldo, BigDecimal tasa, List<Cuota> pendientes) {
        var cuota = pendientes.getFirst();
        var importeActual = cuota.getCapital().sumar(cuota.getInteres());
        int plazo = CalculadoraFrancesa.plazoPara(saldo, tasa, importeActual, pendientes.size());
        return CalculadoraFrancesa.amortizar(saldo, tasa, importeActual, plazo);
    }
}
