package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.model.TiposTransaccion;
import com.projects.kapikua_api.billetera.model.Transaccion;
import com.projects.kapikua_api.billetera.repository.BilleteraRepository;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.CobroRecibido;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicadorTransaccionesTest {
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final UsuarioId NEGOCIO = new UsuarioId(20L);

    private final List<Object> publicados = new ArrayList<>();
    private final BilleteraRepository billeteras = mock(BilleteraRepository.class);
    private final PublicadorTransacciones publicador = new PublicadorTransacciones(publicados::add, billeteras);

    @BeforeEach
    void preparar() {
        var destino = mock(Billetera.class);
        when(destino.getUsuarioId()).thenReturn(NEGOCIO);
        when(billeteras.porId(2L)).thenReturn(Optional.of(destino));
    }

    Transaccion tx(Long id, TipoTransaccion tipo, Transaccion.Movimiento... movimientos) {
        return new Transaccion(id, tipo, Dinero.de("50"), "ref-" + tipo.codigo(), null, AHORA, List.of(movimientos));
    }

    Transaccion.Movimiento mov(long billetera, String monto) {
        return new Transaccion.Movimiento(billetera, new BigDecimal(monto), Dinero.de("100"));
    }

    @Test
    void unPagoANegocioEsUnaOperacionYUnCobroParaQuienRecibe() {
        publicador.publicar(tx(9L, TiposTransaccion.PAGO_NEGOCIO, mov(1, "-50"), mov(2, "50")));
        assertThat(publicados).containsExactly(
                new TransaccionRegistrada(9L, "PAGO_NEGOCIO", Dinero.de("50"), 1L, 2L, AHORA),
                new CobroRecibido(NEGOCIO, 2L, 9L, Dinero.de("50")));
    }

    @Test
    void unDesembolsoOUnPagoDeCuotaNoSonCobros() {
        publicador.publicar(tx(10L, TiposTransaccion.DESEMBOLSO, mov(2, "50")));
        publicador.publicar(tx(11L, TiposTransaccion.PAGO_CUOTA, mov(2, "-50")));
        assertThat(publicados).containsExactly(
                new TransaccionRegistrada(10L, "DESEMBOLSO", Dinero.de("50"), null, 2L, AHORA),
                new TransaccionRegistrada(11L, "PAGO_CUOTA", Dinero.de("50"), 2L, null, AHORA));
    }

    @Test
    void sinGuardarNoSePublicaNada() {
        assertThatThrownBy(() -> publicador.publicar(tx(null, TiposTransaccion.DESEMBOLSO, mov(2, "50"))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(publicados).isEmpty();
    }
}
