package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.event.CobroRecibido;
import com.projects.kapikua_api.billetera.domain.event.OperacionRegistrada;
import com.projects.kapikua_api.shared.Dinero;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los eventos de una transacción se derivan del registro guardado. */
class TransaccionEventosTest {

    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    Transaccion tx(Long id, TipoTransaccion tipo, Transaccion.Movimiento... movimientos) {
        return new Transaccion(id, tipo, Dinero.de("50"), "ref-" + tipo, null, AHORA, List.of(movimientos));
    }

    Transaccion.Movimiento mov(long billetera, String monto) {
        return new Transaccion.Movimiento(billetera, new BigDecimal(monto), Dinero.de("100"));
    }

    @Test
    void unPagoANegocioEsUnaOperacionYUnCobroParaQuienRecibe() {
        var eventos = tx(9L, TipoTransaccion.PAGO_NEGOCIO, mov(1, "-50"), mov(2, "50")).eventos();
        assertThat(eventos).containsExactly(
                new OperacionRegistrada(9L, TipoTransaccion.PAGO_NEGOCIO, Dinero.de("50"), 1L, 2L, AHORA),
                new CobroRecibido(2L, 9L, Dinero.de("50")));
    }

    @Test
    void unaRecargaOUnPagoDeCuotaNoSonCobros() {
        assertThat(tx(10L, TipoTransaccion.RECARGA, mov(2, "50")).eventos()).singleElement()
                .isEqualTo(new OperacionRegistrada(10L, TipoTransaccion.RECARGA, Dinero.de("50"), null, 2L, AHORA));
        assertThat(tx(11L, TipoTransaccion.PAGO_CUOTA, mov(2, "-50")).eventos()).singleElement()
                .isEqualTo(new OperacionRegistrada(11L, TipoTransaccion.PAGO_CUOTA, Dinero.de("50"), 2L, null, AHORA));
    }

    @Test
    void sinGuardarNoHayEventos() {
        assertThatThrownBy(() -> tx(null, TipoTransaccion.RECARGA, mov(2, "50")).eventos())
                .isInstanceOf(IllegalStateException.class);
    }
}
