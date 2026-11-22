package com.projects.kapikua_api.billetera.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Transaccion(Long id,
                          TipoTransaccion tipo,
                          Dinero monto,
                          String referencia,
                          String descripcion,
                          OffsetDateTime fecha,
                          List<Movimiento> movimientos) {
    public record Movimiento(Long billeteraId, BigDecimal monto, Dinero saldoPosterior) {}

    public Transaccion {
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(monto);
        Objects.requireNonNull(fecha);
        if (referencia == null || referencia.isBlank() || referencia.length() > 60) {
            throw new IllegalArgumentException("Referencia inválida");
        }
        if (monto.esCero()) throw new IllegalArgumentException("Una transacción no puede ser de monto cero");
        movimientos = List.copyOf(movimientos);
    }

    public static Transaccion desembolso(Billetera destino, Dinero monto, String referencia, String descripcion,
                                         OffsetDateTime ahora) {
        return new Transaccion(null, TiposTransaccion.DESEMBOLSO, monto, referencia, descripcion, ahora,
                List.of(new Movimiento(destino.getId(), monto.valor(), destino.getSaldo())));
    }

    public static Transaccion pagoACredito(TipoTransaccion tipo, Billetera origen, Dinero monto, String referencia,
                                           String descripcion, OffsetDateTime ahora) {
        if (!tipo.esPagoCredito()) {
            throw new IllegalArgumentException("Tipo no válido para un pago de crédito: " + tipo.codigo());
        }
        return new Transaccion(null, tipo, monto, referencia, descripcion, ahora,
                List.of(new Movimiento(origen.getId(), monto.valor().negate(), origen.getSaldo())));
    }

    public static Transaccion entreBilleteras(TipoTransaccion tipo, Billetera origen, Billetera destino,
                                              Dinero monto, String referencia, String descripcion,
                                              OffsetDateTime ahora) {
        if (!tipo.cuentaComoPagoOTransferencia()) {
            throw new IllegalArgumentException("Tipo no válido entre billeteras: " + tipo.codigo());
        }
        return new Transaccion(null, tipo, monto, referencia, descripcion, ahora, List.of(
                new Movimiento(origen.getId(), monto.valor().negate(), origen.getSaldo()),
                new Movimiento(destino.getId(), monto.valor(), destino.getSaldo())));
    }

    public Optional<Movimiento> movimientoDe(Long billeteraId) {
        return movimientos.stream().filter(m -> m.billeteraId().equals(billeteraId)).findFirst();
    }

    public Transaccion conId(Long id) {
        return new Transaccion(id, tipo, monto, referencia, descripcion, fecha, movimientos);
    }
}
