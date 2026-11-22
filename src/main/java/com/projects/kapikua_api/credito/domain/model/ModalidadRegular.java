package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;

final class ModalidadRegular extends ModalidadPago {
    ModalidadRegular() {
        super("REGULAR");
    }

    @Override
    public boolean esAnticipada() {
        return false;
    }

    Result<Dinero, ErrorCredito> aplicarRegular(
            Prestamo prestamo,
            List<Cuota> destino,
            Dinero monto,
            boolean usaApartado,
            Long transaccionId, 
            Origen origen,
            boolean automatico, 
            LocalDate hoy, 
            OffsetDateTime ahora
    ) {
        if (monto.esCero()) return Result.err(new ErrorCredito.NadaQuePagar());
        var hayDuplicadas = new HashSet<>(destino).size() != destino.size();
        var cuotas = prestamo.getCuotas();
        if (hayDuplicadas || !cuotas.containsAll(destino)) {
            return Result.err(new ErrorCredito.PagoRegularInvalido("Las cuotas del pago no son válidas para este préstamo."));
        }
        var cobrables = destino.stream().filter(Cuota::porCobrar).toList();
        var total = cobrables.stream().map(Cuota::pendiente).reduce(Dinero.CERO, Dinero::sumar);
        if (monto.mayorQue(total)) {
            return Result.err(new ErrorCredito.PagoRegularInvalido("El pago supera lo pendiente en esas cuotas."));
        }
        if (cobrables.stream().anyMatch(cuota -> cuota.getId() == null)) {
            return Result.err(new ErrorCredito.PagoRegularInvalido("Las cuotas deben estar registradas para aplicar el pago."));
        }

        var resto = monto;
        var sobrante = Dinero.CERO;
        for (var cuota : cobrables) {
            if (resto.esCero()) break;
            var aEsta = minimo(resto, cuota.pendiente());
            if (aEsta.esCero()) continue;
            var deApartado = usaApartado ? minimo(cuota.getMontoApartado(), aEsta) : Dinero.CERO;
            var resultado = cuota.imputar(aEsta, deApartado, hoy);
            if (resultado instanceof Result.Err<Cuota.PagoImputado, ErrorCredito> err) return Result.err(err.error());
            var aplicado = ((Result.Ok<Cuota.PagoImputado, ErrorCredito>) resultado).valor();
            sobrante = sobrante.sumar(aplicado.sobrante());
            prestamo.registrarPagoCuota(new Pago(cuota.getId(), transaccionId, TipoPago.CUOTA, origen, automatico,
                    this, aEsta, aplicado.imputacion(), ahora));
            resto = resto.restar(aEsta);
        }
        return Result.ok(sobrante);
    }

    private static Dinero minimo(Dinero a, Dinero b) {
        return a.menorQue(b) ? a : b;
    }
}
