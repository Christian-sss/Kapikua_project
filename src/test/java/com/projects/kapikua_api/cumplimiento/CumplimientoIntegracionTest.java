package com.projects.kapikua_api.cumplimiento;

import com.projects.kapikua_api.DatosDePrueba;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase;
import com.projects.kapikua_api.billetera.application.port.in.RecargarSaldoUseCase;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.cumplimiento.application.port.in.ReclamosUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.in.RevisarAlertasUseCase;
import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoAlerta;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoReclamo;
import com.projects.kapikua_api.cumplimiento.domain.model.MotivoReclamo;
import com.projects.kapikua_api.cumplimiento.domain.model.ReglaAlerta;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Alertas (CU-14, RN-28) y reclamos (CU-15, RN-29) contra PostgreSQL. Solo con KAPIKUA_DB_PRUEBAS. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KAPIKUA_DB_PRUEBAS", matches = "jdbc:postgresql:.+")
class CumplimientoIntegracionTest {

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("KAPIKUA_DB_PRUEBAS"));
    }

    @Autowired RegistrarUsuarioUseCase registrar;
    @Autowired RecargarSaldoUseCase recargar;
    @Autowired EnviarDineroUseCase enviar;
    @Autowired RevisarAlertasUseCase alertas;
    @Autowired ReclamosUseCase reclamos;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    DatosDePrueba datos;

    @BeforeEach
    void preparar() {
        datos = new DatosDePrueba(registrar, jdbc, clock);
    }

    private static <T> T ok(Result<T, ErrorCumplimiento> r) {
        if (r instanceof Result.Err<T, ErrorCumplimiento> e) throw new AssertionError("Se esperaba Ok y fue " + e.error());
        return ((Result.Ok<T, ErrorCumplimiento>) r).valor();
    }

    private static ErrorCumplimiento error(Result<?, ErrorCumplimiento> r) {
        assertThat(r.esOk()).isFalse();
        return ((Result.Err<?, ErrorCumplimiento>) r).error();
    }

    private Long recarga(UsuarioId u, String monto) {
        var r = recargar.recargar(u, Dinero.de(monto), "tok_aprobado", UUID.randomUUID().toString());
        assertThat(r.esOk()).as("recarga %s", r).isTrue();
        return jdbc.queryForObject("select max(id) from billetera.transaccion", Long.class);
    }

    private Long billetera(UsuarioId u) {
        return jdbc.queryForObject("select id from billetera.billetera where usuario_id = ?", Long.class, u.valor());
    }

    private List<String> reglasDe(UsuarioId u) {
        return jdbc.queryForList("select regla from cumplimiento.alerta where billetera_id = ? order by id", String.class, billetera(u));
    }

    private Long alertaDe(UsuarioId u, ReglaAlerta regla) {
        return jdbc.queryForObject("select id from cumplimiento.alerta where billetera_id = ? and regla = ?", Long.class,
                billetera(u), regla.name());
    }

    // ------------------------------------------------------------------ alertas

    @Test
    void unaRecargaCercaDelTopeGeneraUnaAlertaConElDniEnmascarado() {
        var ana = datos.cliente('0');
        var tx = recarga(ana.id(), "3000");
        assertThat(reglasDe(ana.id())).containsExactly("CERCA_DEL_TOPE");

        var pendiente = alertas.pendientes().stream().filter(a -> a.regla() == ReglaAlerta.CERCA_DEL_TOPE
                && a.cliente().dniEnmascarado().endsWith(ana.dni().substring(ana.dni().length() - 3))).findFirst();
        assertThat(pendiente).isPresent();
        assertThat(pendiente.get().cliente().dniEnmascarado()).isNotEqualTo(ana.dni()).contains("*");

        var detalle = ok(alertas.detalle(pendiente.get().id()));
        assertThat(detalle.transaccionId()).isEqualTo(tx);
        assertThat(detalle.operaciones()).extracting(o -> o.transaccionId()).contains(tx);
    }

    @Test
    void tresRecargasQueSuperanElTopeSonFraccionamientoUnaSolaVez() {
        var ana = datos.cliente('0');
        recarga(ana.id(), "1200");
        recarga(ana.id(), "1200");
        assertThat(reglasDe(ana.id())).isEmpty();
        recarga(ana.id(), "1200");
        recarga(ana.id(), "1200");
        assertThat(reglasDe(ana.id())).containsExactly("FRACCIONAMIENTO");
    }

    @Test
    void cincoPagosEnDiezMinutosSonTransferenciasSeguidas() {
        var ana = datos.cliente('0');
        var bodega = datos.cliente('1');
        recarga(ana.id(), "500");
        for (int i = 0; i < 6; i++) {
            assertThat(enviar.pagarNegocio(ana.id(), bodega.celular(), Dinero.de("10"), UUID.randomUUID().toString()).esOk()).isTrue();
        }
        assertThat(reglasDe(ana.id())).containsExactly("TRANSFERENCIAS_SEGUIDAS");
        assertThat(reglasDe(bodega.id())).isEmpty();   // quien recibe no hizo las operaciones
    }

    @Test
    void escalarConBloqueoDejaLaBilleteraBloqueadaYLaDecisionEsDefinitiva() {
        var ana = datos.cliente('0');
        var admin = datos.cliente('1');
        recarga(ana.id(), "3000");
        var alerta = alertaDe(ana.id(), ReglaAlerta.CERCA_DEL_TOPE);

        assertThat(error(alertas.revisar(alerta, admin.id(), EstadoAlerta.DESCARTADA, "Normal", true, "Motivo")))
                .isInstanceOf(ErrorCumplimiento.BloqueoSoloAlEscalar.class);
        assertThat(error(alertas.revisar(alerta, admin.id(), EstadoAlerta.ESCALADA, "Revisar origen", true, "")))
                .isInstanceOf(ErrorCumplimiento.MotivoBloqueoInvalido.class);

        ok(alertas.revisar(alerta, admin.id(), EstadoAlerta.ESCALADA, "Origen de fondos no claro", true, "Alerta escalada a cumplimiento"));
        var fila = jdbc.queryForMap("select estado, comentario, revisado_por, fecha_revision from cumplimiento.alerta where id = ?", alerta);
        assertThat(fila.get("estado")).isEqualTo("ESCALADA");
        assertThat(fila.get("revisado_por")).isEqualTo(admin.id().valor());
        assertThat(jdbc.queryForObject("select estado || ' · ' || motivo_bloqueo from billetera.billetera where usuario_id = ?",
                String.class, ana.id().valor())).isEqualTo("BLOQUEADA · Alerta escalada a cumplimiento");

        var otra = recargar.recargar(ana.id(), Dinero.de("10"), "tok_aprobado", UUID.randomUUID().toString());
        assertThat(otra).isInstanceOf(Result.Err.class);
        assertThat(((Result.Err<?, ErrorBilletera>) otra).error()).isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);

        assertThat(error(alertas.revisar(alerta, admin.id(), EstadoAlerta.DESCARTADA, "Cambio de opinión", false, "")))
                .isInstanceOf(ErrorCumplimiento.AlertaYaRevisada.class);
        assertThat(ok(alertas.detalle(alerta)).billeteraBloqueada()).isTrue();
        assertThat(alertas.revisadas(50)).extracting(RevisarAlertasUseCase.ResumenAlerta::id).contains(alerta);
    }

    @Test
    void descartarNoBloqueaLaBilletera() {
        var ana = datos.cliente('0');
        recarga(ana.id(), "3000");
        ok(alertas.revisar(alertaDe(ana.id(), ReglaAlerta.CERCA_DEL_TOPE), datos.cliente('1').id(),
                EstadoAlerta.DESCARTADA, "Venta de temporada, habitual", false, ""));
        assertThat(jdbc.queryForObject("select estado from billetera.billetera where usuario_id = ?", String.class, ana.id().valor()))
                .isEqualTo("ACTIVA");
    }

    // ------------------------------------------------------------------ reclamos

    @Test
    void elClienteRegistraUnReclamoConCodigoYPlazoYElAdministradorResponde() {
        var ana = datos.cliente('0');
        var luis = datos.cliente('1');
        var tx = recarga(ana.id(), "100");

        assertThat(error(reclamos.registrar(luis.id(), MotivoReclamo.OPERACION, "No reconozco esta operación", tx)))
                .isInstanceOf(ErrorCumplimiento.OperacionAjena.class);

        var r = ok(reclamos.registrar(ana.id(), MotivoReclamo.OPERACION, "La recarga no se reflejó a tiempo", tx));
        var hoy = LocalDate.now(clock);
        assertThat(r.codigo()).matches("R-" + hoy.getYear() + "-\\d{6}").endsWith(String.format("%06d", r.id()));
        assertThat(r.detalle()).startsWith("Operación N° " + tx + ": ");
        assertThat(r.fechaLimite()).isAfter(hoy.plusDays(14));
        assertThat(r.diasHabilesRestantes()).isEqualTo(15);

        assertThat(reclamos.deUsuario(ana.id())).extracting(ReclamosUseCase.VistaReclamo::codigo).containsExactly(r.codigo());
        assertThat(error(reclamos.delUsuario(luis.id(), r.id()))).isInstanceOf(ErrorCumplimiento.ReclamoNoEncontrado.class);
        assertThat(reclamos.pendientes()).extracting(ReclamosUseCase.VistaReclamo::id).contains(r.id());
        assertThat(ok(reclamos.detalle(r.id())).cliente().dniEnmascarado()).contains("*");

        var limiteAntes = r.fechaLimite();
        ok(reclamos.prorrogar(r.id(), "Esperamos el reporte de la pasarela de pagos"));
        assertThat(error(reclamos.prorrogar(r.id(), "Otra prórroga más"))).isInstanceOf(ErrorCumplimiento.ProrrogaYaRegistrada.class);
        assertThat(ok(reclamos.delUsuario(ana.id(), r.id())).fechaLimite()).isAfter(limiteAntes.plusDays(14));

        ok(reclamos.responder(r.id(), luis.id(), "La recarga se acreditó; el retraso fue de la pasarela."));
        assertThat(error(reclamos.responder(r.id(), luis.id(), "Respuesta duplicada del reclamo")))
                .isInstanceOf(ErrorCumplimiento.ReclamoYaAtendido.class);
        var atendido = ok(reclamos.delUsuario(ana.id(), r.id()));
        assertThat(atendido.estado()).isEqualTo(EstadoReclamo.ATENDIDO);
        assertThat(atendido.respuesta()).startsWith("La recarga se acreditó");
        assertThat(reclamos.pendientes()).extracting(ReclamosUseCase.VistaReclamo::id).doesNotContain(r.id());
        assertThat(reclamos.atendidos(50)).extracting(ReclamosUseCase.VistaReclamo::id).contains(r.id());
    }

    @Test
    void losReclamosPendientesSeOrdenanPorFechaLimite() {
        var ana = datos.cliente('0');
        var primero = ok(reclamos.registrar(ana.id(), MotivoReclamo.OTRO, "Primer reclamo de prueba", null));
        var segundo = ok(reclamos.registrar(ana.id(), MotivoReclamo.CREDITO, "Segundo reclamo de prueba", null));
        jdbc.update("update cumplimiento.reclamo set fecha_limite = current_date + 1 where id = ?", segundo.id());
        var ids = reclamos.pendientes().stream().map(ReclamosUseCase.VistaReclamo::id).toList();
        assertThat(ids.indexOf(segundo.id())).isLessThan(ids.indexOf(primero.id()));
        assertThat(ok(reclamos.detalle(segundo.id())).cercaDelLimite()).isTrue();
    }
}
