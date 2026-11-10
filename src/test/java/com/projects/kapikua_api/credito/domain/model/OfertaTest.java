package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OfertaTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    private static final UsuarioId ANA = new UsuarioId(1L);
    private static final UsuarioId LUIS = new UsuarioId(2L);
    private static final UsuarioId ROSA = new UsuarioId(3L);
    private static final UsuarioId PEPE = new UsuarioId(4L);
    private static final Duration HORAS_48 = Duration.ofHours(48);
    private static final CondicionesCredito CONDICIONES = new CondicionesCredito(Dinero.de("800"), 8,
            new BigDecimal("95"), new BigDecimal("95"), Dinero.de("112.50"));

    private static Oferta nueva() {
        return Oferta.crear(10L, ANA, AHORA, LIMA, 7);
    }

    private static ErrorCredito err(Result<?, ErrorCredito> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<?, ErrorCredito>) r).error();
    }

    @Test
    void naceEnPendienteEfaYVenceALosSieteDias() {
        var o = nueva();
        assertThat(o.getEstado()).isEqualTo(EstadoOferta.PENDIENTE_EFA);
        assertThat(o.getVigenteHasta()).isEqualTo(LocalDate.of(2026, 11, 16));

        assertThat(o.actualizarVencimientos(AHORA.plusDays(7), LIMA, HORAS_48)).isFalse();
        assertThat(o.actualizarVencimientos(LocalDate.of(2026, 11, 17).atStartOfDay(LIMA).toOffsetDateTime(), LIMA, HORAS_48))
                .isTrue();
        assertThat(o.getEstado()).isEqualTo(EstadoOferta.VENCIDA);
    }

    @Test
    void elAvalSoloSePideSiLaEvaluacionLoExigeYNoPuedeSerElDeudor() {
        assertThat(err(nueva().designarAval(LUIS, false, 3, AHORA))).isInstanceOf(ErrorCredito.AvalNoRequerido.class);
        assertThat(err(nueva().designarAval(ANA, true, 3, AHORA))).isInstanceOf(ErrorCredito.AvalMismoDeudor.class);
    }

    @Test
    void unAvalEnCursoALaVezYNuncaDosVecesLaMismaPersona() {
        var o = nueva();
        assertThat(o.designarAval(LUIS, true, 3, AHORA)).isInstanceOf(Result.Ok.class);
        assertThat(err(o.designarAval(ROSA, true, 3, AHORA))).isInstanceOf(ErrorCredito.AvalEnCurso.class);

        o.responderAval(LUIS, false, 10, null, AHORA.plusHours(1));
        assertThat(err(o.designarAval(LUIS, true, 3, AHORA.plusHours(2)))).isInstanceOf(ErrorCredito.AvalYaInvitado.class);
        assertThat(o.designarAval(ROSA, true, 3, AHORA.plusHours(2))).isInstanceOf(Result.Ok.class);
    }

    @Test
    void comoMaximoTresAvalesPorSolicitud() {
        var o = nueva();
        for (var aval : new UsuarioId[]{LUIS, ROSA, PEPE}) {
            o.designarAval(aval, true, 3, AHORA);
            o.responderAval(aval, false, 10, null, AHORA.plusMinutes(5));
        }
        assertThat(err(o.designarAval(new UsuarioId(5L), true, 3, AHORA.plusHours(1))))
                .isEqualTo(new ErrorCredito.AvalesAgotados(3));
    }

    @Test
    void laInvitacionSinRespuestaVenceALas48Horas() {
        var o = nueva();
        o.designarAval(LUIS, true, 3, AHORA);
        assertThat(o.actualizarVencimientos(AHORA.plusHours(47), LIMA, HORAS_48)).isFalse();
        assertThat(o.actualizarVencimientos(AHORA.plusHours(48), LIMA, HORAS_48)).isTrue();
        assertThat(o.getAvales().getFirst().getEstado()).isEqualTo(EstadoAval.RECHAZADO);
        assertThat(err(o.responderAval(LUIS, true, 10, "x", AHORA.plusHours(49))))
                .isInstanceOf(ErrorCredito.InvitacionNoEncontrada.class);
    }

    @Test
    void laInvitacionNoDuraMasQueLaOferta() {
        var o = nueva();
        var seisDiasDespues = AHORA.plusDays(6).plusHours(13);
        o.designarAval(LUIS, true, 3, seisDiasDespues);
        var finVigencia = o.finVigencia(LIMA);
        assertThat(o.getAvales().getFirst().venceEn(HORAS_48, finVigencia)).isEqualTo(finVigencia);
    }

    @Test
    void conAvalAceptadoQuedaListaParaLaEntidadAliada() {
        var o = nueva();
        assertThat(o.listaParaEfa(true)).isFalse();
        o.designarAval(LUIS, true, 3, AHORA);
        o.responderAval(LUIS, true, 8, "Chrome/Android", AHORA.plusHours(1));
        assertThat(o.listaParaEfa(true)).isTrue();
        assertThat(o.avalAceptado().orElseThrow().getPorcentajeRetencionAutorizado()).isEqualTo(8);
        assertThat(nueva().listaParaEfa(false)).isTrue();
    }

    @Test
    void soloSeAceptaConfirmadaVigenteYConElAvalAceptado() {
        var o = nueva();
        assertThat(err(o.aceptar(false, AHORA, LIMA))).isInstanceOf(ErrorCredito.OfertaEstadoInvalido.class);

        o.designarAval(LUIS, true, 3, AHORA);
        o.responderAval(LUIS, true, 8, "x", AHORA);
        o.confirmar("EFA-S-1", CONDICIONES);
        assertThat(err(o.aceptar(true, AHORA.plusDays(8), LIMA))).isInstanceOf(ErrorCredito.OfertaNoVigente.class);
        assertThat(o.aceptar(true, AHORA.plusDays(1), LIMA)).isInstanceOf(Result.Ok.class);
        assertThat(o.getEstado()).isEqualTo(EstadoOferta.ACEPTADA);
    }

    @Test
    void sinAvalAceptadoNoSePuedeAceptarEnNivelesUnoYDos() {
        var o = nueva();
        o.confirmar("EFA-S-2", CONDICIONES);
        assertThat(err(o.aceptar(true, AHORA, LIMA))).isInstanceOf(ErrorCredito.AvalRequerido.class);
    }

    @Test
    void laEntidadAliadaPuedeDenegarConMotivo() {
        var o = nueva();
        o.denegar("EFA-S-3", "Política interna");
        assertThat(o.getEstado()).isEqualTo(EstadoOferta.DENEGADA);
        assertThat(o.getMotivoDenegacion()).isEqualTo("Política interna");
        assertThat(err(o.confirmar("EFA-S-3", CONDICIONES))).isInstanceOf(ErrorCredito.OfertaEstadoInvalido.class);
    }

    @Test
    void elPrestamoNaceVigenteConSuCronogramaYElCargoAutorizado() {
        var o = nueva();
        o.confirmar("EFA-S-4", CONDICIONES);
        o.aceptar(false, AHORA, LIMA);
        var oferta = Oferta.reconstituir(99L, o.getEvaluacionId(), o.getUsuarioId(), o.getCodigoSolicitudEfa(),
                o.getEstado(), o.getCondiciones(), null, o.getFecha(), o.getVigenteHasta(), 0L, o.getAvales());
        var p = Prestamo.desembolsar(oferta, "EFA-C-1", 8, new ConstanciaAceptacion(AHORA, "Chrome", "HR-1.0"), AHORA, LIMA);

        assertThat(p.getEstado()).isEqualTo(EstadoPrestamo.VIGENTE);
        assertThat(p.getCuotas()).hasSize(8);
        assertThat(p.capitalPendiente()).isEqualTo(Dinero.de("800"));
        assertThat(p.isCargoAutorizado()).isTrue();
        assertThat(p.getFechaAutorizacionCargo()).isEqualTo(AHORA);
        assertThat(p.getCodigoEntidad()).isEqualTo("AYNI_SUR");
        assertThat(p.extraerEventos()).singleElement().isInstanceOfSatisfying(
                com.projects.kapikua_api.credito.domain.event.PrestamoDesembolsado.class, e -> {
                    assertThat(e.codigoCredito()).isEqualTo("EFA-C-1");
                    assertThat(e.ofertaId()).isEqualTo(99L);
                    assertThat(e.monto()).isEqualTo(Dinero.de("800"));
                });
    }
}
