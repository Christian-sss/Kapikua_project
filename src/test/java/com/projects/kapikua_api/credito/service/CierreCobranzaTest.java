package com.projects.kapikua_api.credito.service;

import com.projects.kapikua_api.credito.client.OperacionesBilletera;
import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.model.ModalidadPago;
import com.projects.kapikua_api.credito.model.Oferta;
import com.projects.kapikua_api.credito.model.Origen;
import com.projects.kapikua_api.credito.repository.OfertaRepository;
import com.projects.kapikua_api.credito.repository.PrestamoRepository;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.evento.EventosCredito;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.projects.kapikua_api.credito.service.PublicadorCreditoTest.AHORA;
import static com.projects.kapikua_api.credito.service.PublicadorCreditoTest.ANA;
import static com.projects.kapikua_api.credito.service.PublicadorCreditoTest.DESEMBOLSO;
import static com.projects.kapikua_api.credito.service.PublicadorCreditoTest.prestamo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CierreCobranzaTest {
    private final PrestamoRepository prestamos = mock(PrestamoRepository.class);
    private final OfertaRepository ofertas = mock(OfertaRepository.class);
    private final OperacionesBilletera billetera = mock(OperacionesBilletera.class);
    private final List<Object> publicados = new ArrayList<>();
    private final CierreCobranza cierre = new CierreCobranza(prestamos, ofertas, billetera,
            new PublicadorCredito(publicados::add), Clock.fixed(AHORA.toInstant(), ZoneId.of("America/Lima")));

    private static <T> T ok(Result<T, ErrorCredito> r) {
        assertThat(r).isInstanceOf(Result.Ok.class);
        return ((Result.Ok<T, ErrorCredito>) r).valor();
    }

    @Test
    void siElCobroSaldaElPrestamoLiberaAlAvalYAvisaQueSePago() {
        var p = prestamo(1L);
        ok(p.aplicarAnticipado(p.montoAnticipadoTotal(), ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA));
        var oferta = mock(Oferta.class);
        when(ofertas.porId(7L)).thenReturn(Optional.of(oferta));

        assertThat(ok(cierre.cerrar(p, Dinero.CERO))).isTrue();

        verify(prestamos).guardar(p);
        verify(oferta).liberarAval(AHORA);
        verify(ofertas).guardar(oferta);
        assertThat(publicados).containsExactly(
                new EventosCredito.PagoRegistrado(ANA, 1L, "EFA-C-1", Dinero.de("800")),
                new EventosCredito.PrestamoPagado(ANA, 1L, false));
    }

    @Test
    void siQuedaSaldoSoloAvisaDelPagoYNoTocaLaOferta() {
        var p = prestamo(1L);
        var c1 = p.getCuotas().getFirst();
        ok(p.aplicarPago(List.of(c1), c1.pendiente(), false, 900L, Origen.DEUDOR, false, DESEMBOLSO, AHORA));

        assertThat(ok(cierre.cerrar(p, Dinero.CERO))).isFalse();

        verify(prestamos).guardar(p);
        verifyNoInteractions(ofertas);
        assertThat(publicados).containsExactly(new EventosCredito.PagoRegistrado(ANA, 1L, "EFA-C-1", c1.total()));
    }

    @Test
    void siNoSePuedeDevolverElApartadoNoGuardaNiAvisa() {
        var p = prestamo(1L);
        var c1 = p.getCuotas().getFirst();
        ok(p.aplicarPago(List.of(c1), c1.pendiente(), false, 900L, Origen.DEUDOR, false, DESEMBOLSO, AHORA));
        when(billetera.liberarApartado(ANA, Dinero.de("5")))
                .thenReturn(Result.err(new ErrorCredito.OperacionBilleteraNoPermitida("bloqueada")));

        assertThat(cierre.cerrar(p, Dinero.de("5")).esOk()).isFalse();

        verifyNoInteractions(prestamos, ofertas);
        assertThat(publicados).isEmpty();
    }
}
