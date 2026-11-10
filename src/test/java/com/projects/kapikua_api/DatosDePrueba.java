package com.projects.kapikua_api;

import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/** Prepara clientes y su historia de cobros en una base de prueba (solo para pruebas de integración). */
public final class DatosDePrueba {

    public record Cliente(UsuarioId id, String dni, String celular) {}

    private final RegistrarUsuarioUseCase registrar;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public DatosDePrueba(RegistrarUsuarioUseCase registrar, JdbcTemplate jdbc, Clock clock) {
        this.registrar = registrar;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Un cliente cuyo DNI termina en {@code digito}: la Central y la entidad aliada simuladas responden según ese dígito. */
    public Cliente cliente(char digito) {
        var r = ThreadLocalRandom.current();
        String dni;
        do { dni = String.valueOf(1_000_000 + r.nextInt(8_999_999)) + digito; } while (dni.endsWith("666"));
        var celular = "9" + (10_000_000 + r.nextInt(89_999_999));
        var resultado = registrar.registrar(new RegistrarUsuarioCommand(dni, LocalDate.of(1990, 1, 1),
                "p" + dni + "@prueba.pe", celular, "Negocio " + dni, "Kapikua2026", true));
        assertThat(resultado).isInstanceOf(Result.Ok.class);
        return new Cliente(((Result.Ok<UsuarioId, RegistroError>) resultado).valor(), dni, celular);
    }

    /**
     * Billetera abierta hace 200 días con 5 cobros de S/ 300 en cada una de las últimas 12 semanas completas,
     * de 15 pagadores distintos (S/ 1 500 por semana, 60 cobros).
     */
    public void conHistoriaDeCobros(UsuarioId cliente) {
        var billetera = jdbc.queryForObject("select id from billetera.billetera where usuario_id = ?", Long.class, cliente.valor());
        jdbc.update("update billetera.billetera set fecha_apertura = now() - interval '200 days' where id = ?", billetera);
        var pagadores = new ArrayList<Long>();
        for (int i = 0; i < 15; i++) {
            var r = ThreadLocalRandom.current();
            pagadores.add(jdbc.queryForObject("""
                    insert into billetera.billetera (usuario_id, numero_celular, saldo, fecha_apertura)
                    values (?, ?, 5000, now() - interval '300 days') returning id""", Long.class,
                    5_000_000L + r.nextInt(4_000_000), "9" + (10_000_000 + r.nextInt(89_999_999))));
        }
        var lunesActual = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int semana = 1; semana <= 12; semana++) {
            for (int k = 0; k < 5; k++) {
                var fecha = Timestamp.from(lunesActual.minusWeeks(semana).plusDays(k)
                        .atTime(10, 0).atZone(clock.getZone()).toInstant());
                var tx = jdbc.queryForObject("""
                        insert into billetera.transaccion (tipo, monto, referencia, fecha)
                        values ('PAGO_NEGOCIO', 300, ?, ?) returning id""", Long.class, UUID.randomUUID().toString(), fecha);
                jdbc.update("insert into billetera.movimiento (transaccion_id, billetera_id, monto, saldo_posterior, fecha) values (?, ?, -300, 1000, ?)",
                        tx, pagadores.get(((semana - 1) * 5 + k) % 15), fecha);
                jdbc.update("insert into billetera.movimiento (transaccion_id, billetera_id, monto, saldo_posterior, fecha) values (?, ?, 300, 1000, ?)",
                        tx, billetera, fecha);
            }
        }
    }

    public java.math.BigDecimal saldo(UsuarioId u) {
        return jdbc.queryForObject("select saldo from billetera.billetera where usuario_id = ?", java.math.BigDecimal.class, u.valor());
    }
}
