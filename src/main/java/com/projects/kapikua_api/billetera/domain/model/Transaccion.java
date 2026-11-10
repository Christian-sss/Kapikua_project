package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.event.CobroRecibido;
import com.projects.kapikua_api.billetera.domain.event.OperacionRegistrada;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.domain.EventoDominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Una operación de dinero, inmutable (RNF-10). Una transferencia o un pago a negocio genera 2 movimientos
 * (débito y crédito); una recarga, 1. Se crea después de aplicar el cambio a las billeteras,
 * para guardar el saldo posterior de cada una.
 */
public record Transaccion(Long id,
                          TipoTransaccion tipo,
                          Dinero monto,
                          String referencia,
                          String descripcion,
                          OffsetDateTime fecha,
                          List<Movimiento> movimientos) {

    /** Una línea del libro mayor: positivo entra, negativo sale. */
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

    public static Transaccion recarga(Billetera destino, Dinero monto, String referencia, OffsetDateTime ahora) {
        return new Transaccion(null, TipoTransaccion.RECARGA, monto, referencia, null, ahora,
                List.of(new Movimiento(destino.getId(), monto.valor(), destino.getSaldo())));
    }

    /**
     * RN-31: el desembolso entra desde la cuenta de la entidad aliada, que no es una billetera:
     * genera un solo movimiento, el de entrada.
     */
    public static Transaccion desembolso(Billetera destino, Dinero monto, String referencia, String descripcion,
                                         OffsetDateTime ahora) {
        return new Transaccion(null, TipoTransaccion.DESEMBOLSO, monto, referencia, descripcion, ahora,
                List.of(new Movimiento(destino.getId(), monto.valor(), destino.getSaldo())));
    }

    /** Pago de cuota o anticipado: sale hacia la entidad aliada, que no es una billetera (un solo movimiento). */
    public static Transaccion pagoACredito(TipoTransaccion tipo, Billetera origen, Dinero monto, String referencia,
                                           String descripcion, OffsetDateTime ahora) {
        if (tipo != TipoTransaccion.PAGO_CUOTA && tipo != TipoTransaccion.PAGO_ANTICIPADO) {
            throw new IllegalArgumentException("Tipo no válido para un pago de crédito: " + tipo);
        }
        return new Transaccion(null, tipo, monto, referencia, descripcion, ahora,
                List.of(new Movimiento(origen.getId(), monto.valor().negate(), origen.getSaldo())));
    }

    /** Transferencia (CU-05) o pago a negocio (CU-06), ya aplicados a ambas billeteras. */
    public static Transaccion entreBilleteras(TipoTransaccion tipo, Billetera origen, Billetera destino,
                                              Dinero monto, String referencia, String descripcion,
                                              OffsetDateTime ahora) {
        if (!tipo.cuentaComoPagoOTransferencia()) {
            throw new IllegalArgumentException("Tipo no válido entre billeteras: " + tipo);
        }
        return new Transaccion(null, tipo, monto, referencia, descripcion, ahora, List.of(
                new Movimiento(origen.getId(), monto.valor().negate(), origen.getSaldo()),
                new Movimiento(destino.getId(), monto.valor(), destino.getSaldo())));
    }

    /** Vacío si la billetera no participa en la transacción. */
    public Optional<Movimiento> movimientoDe(Long billeteraId) {
        return movimientos.stream().filter(m -> m.billeteraId().equals(billeteraId)).findFirst();
    }

    public Transaccion conId(Long id) {
        return new Transaccion(id, tipo, monto, referencia, descripcion, fecha, movimientos);
    }

    /**
     * Eventos de dominio de la operación. La transacción es inmutable y su identidad la asigna la base, así que
     * sus eventos se derivan del registro ya guardado: no hay un estado previo que pueda cambiar.
     */
    public List<EventoDominio> eventos() {
        if (id == null) throw new IllegalStateException("La transacción aún no se ha guardado");
        var origen = movimientos.stream().filter(m -> m.monto().signum() < 0).map(Movimiento::billeteraId).findFirst().orElse(null);
        var destino = movimientos.stream().filter(m -> m.monto().signum() > 0).map(Movimiento::billeteraId).findFirst().orElse(null);
        var eventos = new ArrayList<EventoDominio>();
        eventos.add(new OperacionRegistrada(id, tipo, monto, origen, destino, fecha));
        if ((tipo == TipoTransaccion.TRANSFERENCIA || tipo == TipoTransaccion.PAGO_NEGOCIO) && destino != null) {
            eventos.add(new CobroRecibido(destino, id, monto));
        }
        return eventos;
    }
}
