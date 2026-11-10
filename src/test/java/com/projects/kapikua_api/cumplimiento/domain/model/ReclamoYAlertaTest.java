package com.projects.kapikua_api.cumplimiento.domain.model;

import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ReclamoYAlertaTest {

    static final OffsetDateTime LUNES = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final UsuarioId ANA = new UsuarioId(10L);
    static final UsuarioId ADMIN = new UsuarioId(1L);

    static <T> T ok(Result<T, ErrorCumplimiento> r) {
        assertThat(r.esOk()).as("%s", r).isTrue();
        return ((Result.Ok<T, ErrorCumplimiento>) r).valor();
    }

    static ErrorCumplimiento error(Result<?, ErrorCumplimiento> r) {
        assertThat(r.esOk()).isFalse();
        return ((Result.Err<?, ErrorCumplimiento>) r).error();
    }

    Reclamo reclamo() {
        return ok(Reclamo.registrar(ANA, MotivoReclamo.COBRO, "Me cobraron la cuota dos veces", null, LUNES));
    }

    @Test
    void elReclamoTienePlazoDe15DiasHabiles() {
        var r = reclamo();
        assertThat(r.getFechaLimite()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(r.getEstado()).isEqualTo(EstadoReclamo.REGISTRADO);
        assertThat(r.diasHabilesRestantes(LUNES.toLocalDate())).isEqualTo(15);
        assertThat(r.cercaDelLimite(LocalDate.of(2026, 11, 25))).isTrue();     // quedan 3
        assertThat(r.cercaDelLimite(LocalDate.of(2026, 11, 24))).isFalse();    // quedan 4
        assertThat(r.fueraDePlazo(LocalDate.of(2026, 12, 1))).isTrue();
    }

    @Test
    void laOperacionVaAlInicioDelDetalleYElDetalleSeValida() {
        var r = ok(Reclamo.registrar(ANA, MotivoReclamo.OPERACION, "  No llegó el dinero  ", 123L, LUNES));
        assertThat(r.getDetalle()).isEqualTo("Operación N° 123: No llegó el dinero");
        assertThat(error(Reclamo.registrar(ANA, MotivoReclamo.OTRO, "corto", null, LUNES)))
                .isInstanceOf(ErrorCumplimiento.DetalleInvalido.class);
        assertThat(error(Reclamo.registrar(ANA, MotivoReclamo.OTRO, "x".repeat(501), null, LUNES)))
                .isInstanceOf(ErrorCumplimiento.DetalleInvalido.class);
        assertThat(error(Reclamo.registrar(ANA, null, "Un problema cualquiera", null, LUNES)))
                .isInstanceOf(ErrorCumplimiento.MotivoRequerido.class);
    }

    @Test
    void responderLoDejaAtendidoUnaSolaVez() {
        var r = reclamo();
        assertThat(error(r.responder("ok", ADMIN, LUNES))).isInstanceOf(ErrorCumplimiento.RespuestaInvalida.class);
        ok(r.responder("Revisamos y devolvimos el cobro duplicado.", ADMIN, LUNES.plusDays(2)));
        assertThat(r.getEstado()).isEqualTo(EstadoReclamo.ATENDIDO);
        assertThat(r.cercaDelLimite(LocalDate.of(2026, 11, 29))).isFalse();
        assertThat(error(r.responder("Otra respuesta distinta", ADMIN, LUNES))).isInstanceOf(ErrorCumplimiento.ReclamoYaAtendido.class);
        assertThat(error(r.prorrogar("Necesitamos más tiempo"))).isInstanceOf(ErrorCumplimiento.ReclamoYaAtendido.class);
    }

    @Test
    void laProrrogaAmplia15DiasHabilesUnaSolaVez() {
        var r = reclamo();
        ok(r.prorrogar("Esperamos la respuesta de la entidad aliada"));
        assertThat(r.getFechaLimite()).isEqualTo(LocalDate.of(2026, 12, 23));   // 30/11 + 15 hábiles (sin 8 y 9/12)
        assertThat(error(r.prorrogar("Otra vez más tiempo"))).isInstanceOf(ErrorCumplimiento.ProrrogaYaRegistrada.class);
    }

    @Test
    void laRevisionDeLaAlertaEsDefinitiva() {
        var a = Alerta.nueva(5L, 77L, ReglaAlerta.CERCA_DEL_TOPE, "detalle", LUNES);
        assertThat(error(a.revisar(EstadoAlerta.PENDIENTE, "comentario", ADMIN, LUNES))).isInstanceOf(ErrorCumplimiento.DecisionInvalida.class);
        assertThat(error(a.revisar(EstadoAlerta.ESCALADA, "x", ADMIN, LUNES))).isInstanceOf(ErrorCumplimiento.ComentarioInvalido.class);
        ok(a.revisar(EstadoAlerta.DESCARTADA, "Venta de fin de mes, habitual", ADMIN, LUNES));
        assertThat(a.getEstado()).isEqualTo(EstadoAlerta.DESCARTADA);
        assertThat(a.getRevisadoPor()).isEqualTo(ADMIN);
        assertThat(error(a.revisar(EstadoAlerta.ESCALADA, "Cambio de opinión", ADMIN, LUNES))).isInstanceOf(ErrorCumplimiento.AlertaYaRevisada.class);
    }
}
