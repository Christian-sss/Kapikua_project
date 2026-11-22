package com.projects.kapikua_api.credito.service;

import com.projects.kapikua_api.credito.model.CalculadoraFrancesa;
import com.projects.kapikua_api.credito.model.ConstanciaAceptacion;
import com.projects.kapikua_api.credito.model.Cuota;
import com.projects.kapikua_api.credito.model.ModalidadPago;
import com.projects.kapikua_api.credito.model.Origen;
import com.projects.kapikua_api.credito.model.Prestamo;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.EventosCredito;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicadorCreditoTest {
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 9);
    static final UsuarioId ANA = new UsuarioId(1L);

    private final List<Object> publicados = new ArrayList<>();
    private final PublicadorCredito publicador = new PublicadorCredito(publicados::add);

    static Prestamo prestamo(Long id) {
        var cronograma = CalculadoraFrancesa.cronograma(Dinero.de("800"), new BigDecimal("95"), 8, DESEMBOLSO);
        var cuotas = new ArrayList<Cuota>();
        for (var c : cronograma.cuotas()) {
            cuotas.add(Cuota.reconstituir((long) c.numero() * 10, c.numero(), c.vencimiento(), c.capital(),
                    c.interes(), Dinero.CERO, Dinero.CERO, Dinero.CERO, null, "PENDIENTE"));
        }
        return Prestamo.reconstituir(id, 7L, ANA, "AYNI_SUR", "EFA-C-1", Dinero.de("800"), new BigDecimal("95"),
                new BigDecimal("95"), 8, new ConstanciaAceptacion(AHORA, "x", "HR-1.0"), true, AHORA, null,
                "VIGENTE", AHORA, 0L, cuotas, false);
    }

    @Test
    void elDesembolsoLlevaElCodigoDelCreditoDeLaEntidad() {
        publicador.desembolsado(prestamo(1L));
        assertThat(publicados).containsExactly(new EventosCredito.PrestamoDesembolsado(ANA, "EFA-C-1"));
    }

    @Test
    void cadaPagoNuevoEsUnAvisoYElPrestamoPagadoDiceSiPagoElAval() {
        var p = prestamo(1L);
        var c1 = p.getCuotas().getFirst();
        p.aplicarPago(List.of(c1), c1.pendiente(), false, 900L, Origen.DEUDOR, false, DESEMBOLSO, AHORA);
        p.aplicarAnticipado(p.montoAnticipadoTotal(), ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA);

        publicador.pagosRegistrados(p);
        publicador.prestamoPagado(p);

        assertThat(publicados).containsExactly(
                new EventosCredito.PagoRegistrado(ANA, 1L, "EFA-C-1", c1.total()),
                new EventosCredito.PagoRegistrado(ANA, 1L, "EFA-C-1", p.pagosNuevos().get(1).monto()),
                new EventosCredito.PrestamoPagado(ANA, 1L, false));
    }

    @Test
    void elAtrasoLlevaLosDias() {
        publicador.atrasoDetectado(prestamo(1L), 3);
        assertThat(publicados).containsExactly(new EventosCredito.AtrasoDetectado(ANA, 1L, 3));
    }

    @Test
    void sinGuardarNoSePublicaNada() {
        assertThatThrownBy(() -> publicador.desembolsado(prestamo(null))).isInstanceOf(IllegalStateException.class);
        assertThat(publicados).isEmpty();
    }
}
