package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.model.CalculadoraFrancesa;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.MatematicaFinanciera;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

abstract sealed class ModalidadAnticipadaParcial extends ModalidadPago
        permits ModalidadReducirCuota, ModalidadReducirPlazo {

    ModalidadAnticipadaParcial(String codigo) {
        super(codigo);
    }

    @Override
    final Result<Void, ErrorCredito> verificarAnticipado(Prestamo prestamo, Dinero monto) {
        return prepararAnticipo(prestamo, monto).map(pendientes -> null);
    }

    @Override
    final Result<Dinero, ErrorCredito> aplicarAnticipado(Prestamo prestamo, Dinero monto, Dinero deApartado,
                                                        OffsetDateTime ahora) {
        return prepararAnticipo(prestamo, monto).flatMap(pendientes -> {
            var saldo = prestamo.capitalPendiente().restar(monto);
            var tasa = MatematicaFinanciera.tasaSemanal(prestamo.getTea());
            var tramos = calcularCronograma(saldo, tasa, pendientes);
            for (int k = 0; k < tramos.size(); k++) {
                var tramo = tramos.get(k);
                var resultado = pendientes.get(k).reprogramar(tramo.capital(), tramo.interes());
                if (resultado instanceof Result.Err<Void, ErrorCredito> err) return Result.err(err.error());
            }
            return anularCuotas(pendientes.subList(tramos.size(), pendientes.size()), ahora);
        });
    }

    abstract List<CalculadoraFrancesa.Tramo> calcularCronograma(Dinero saldo, BigDecimal tasa, List<Cuota> pendientes);

    private Result<List<Cuota>, ErrorCredito> prepararAnticipo(Prestamo prestamo, Dinero monto) {
        var capital = prestamo.capitalPendiente();
        if (monto.esCero() || !monto.menorQue(capital)) {
            return Result.err(new ErrorCredito.MontoAnticipadoInvalido(capital.toString()));
        }
        return cuotasReprogramables(prestamo);
    }
}
