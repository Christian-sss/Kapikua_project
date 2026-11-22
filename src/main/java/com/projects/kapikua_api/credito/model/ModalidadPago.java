package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public abstract sealed class ModalidadPago permits ModalidadRegular, ModalidadTotal, ModalidadAnticipadaParcial {
    static final ModalidadRegular REGULAR = new ModalidadRegular();
    public static final ModalidadPago TOTAL = new ModalidadTotal();
    public static final ModalidadPago REDUCIR_CUOTA = new ModalidadReducirCuota();
    public static final ModalidadPago REDUCIR_PLAZO = new ModalidadReducirPlazo();

    private static final List<ModalidadPago> ANTICIPADAS = List.of(TOTAL, REDUCIR_CUOTA, REDUCIR_PLAZO);

    private final String codigo;

    ModalidadPago(String codigo) {
        this.codigo = codigo;
    }

    public final String codigo() {
        return codigo;
    }

    public static Optional<ModalidadPago> desdeCodigoAnticipado(String codigo) {
        if (codigo == null) return Optional.empty();
        var normalizado = codigo.strip();
        return ANTICIPADAS.stream().filter(modalidad -> modalidad.codigo().equals(normalizado)).findFirst();
    }

    public boolean esLiquidacionTotal() {
        return false;
    }

    public boolean esAnticipada() {
        return true;
    }

    public Dinero montoACobrar(Prestamo prestamo, Dinero montoSolicitado) {
        return montoSolicitado;
    }

    public Dinero apartadoAUsar(Prestamo prestamo) {
        return Dinero.CERO;
    }

    Result<Void, ErrorCredito> verificarAnticipado(Prestamo prestamo, Dinero monto) {
        return Result.err(new ErrorCredito.ModalidadPagoInvalida());
    }

    Result<Dinero, ErrorCredito> aplicarAnticipado(Prestamo prestamo, Dinero monto, Dinero deApartado,
                                                  OffsetDateTime ahora) {
        return Result.err(new ErrorCredito.ModalidadPagoInvalida());
    }

    static Result<List<Cuota>, ErrorCredito> cuotasReprogramables(Prestamo prestamo) {
        var pendientes = prestamo.getCuotas().stream().filter(Cuota::esPendiente).toList();
        if (pendientes.isEmpty()) return Result.err(new ErrorCredito.NadaQuePagar());
        for (var cuota : pendientes) {
            if (cuota.verificarReprogramable() instanceof Result.Err<Void, ErrorCredito> err) {
                return Result.err(err.error());
            }
        }
        return Result.ok(pendientes);
    }

    static Result<Dinero, ErrorCredito> anularCuotas(List<Cuota> cuotas, OffsetDateTime ahora) {
        var liberado = Dinero.CERO;
        for (var cuota : cuotas) {
            var apartado = cuota.getMontoApartado();
            if (cuota.anular(ahora) instanceof Result.Err<CambioEstado, ErrorCredito> err) {
                return Result.err(err.error());
            }
            liberado = liberado.sumar(apartado);
        }
        return Result.ok(liberado);
    }
}
