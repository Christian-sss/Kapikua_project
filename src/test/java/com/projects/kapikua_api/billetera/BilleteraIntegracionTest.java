package com.projects.kapikua_api.billetera;

import com.projects.kapikua_api.billetera.application.port.in.Comprobante;
import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase.VistaPrevia;
import com.projects.kapikua_api.billetera.application.port.in.RecargarSaldoUseCase;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KAPIKUA_DB_PRUEBAS", matches = "jdbc:postgresql:.+")
class BilleteraIntegracionTest {
    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("KAPIKUA_DB_PRUEBAS"));
    }

    @Autowired RegistrarUsuarioUseCase registrar;
    @Autowired RecargarSaldoUseCase recargar;
    @Autowired EnviarDineroUseCase enviar;
    @Autowired ConsultarBilleteraUseCase consultar;
    @Autowired JdbcTemplate jdbc;

    private record Cliente(UsuarioId id, String celular) {}

    private Cliente registrarCliente(String negocio) {
        var r = ThreadLocalRandom.current();
        String dni;
        do { dni = String.valueOf(10_000_000 + r.nextInt(89_999_999)); } while (dni.endsWith("666"));
        var celular = "9" + (10_000_000 + r.nextInt(89_999_999));
        var resultado = registrar.registrar(new RegistrarUsuarioCommand(dni, LocalDate.of(1990, 1, 1),
                "c" + dni + "@prueba.pe", celular, negocio, "Kapikua2026", true));
        assertThat(resultado).isInstanceOf(Result.Ok.class);
        return new Cliente(((Result.Ok<UsuarioId, RegistroError>) resultado).valor(), celular);
    }

    private Dinero saldo(Cliente c) {
        return Dinero.de(jdbc.queryForObject("select saldo from billetera.billetera where usuario_id = ?",
                java.math.BigDecimal.class, c.id().valor()));
    }

    private int movimientos(Cliente c) {
        return jdbc.queryForObject("""
                select count(*) from billetera.movimiento m join billetera.billetera b on b.id = m.billetera_id
                where b.usuario_id = ?""", Integer.class, c.id().valor());
    }

    private static <T> T ok(Result<T, ErrorBilletera> r) {
        assertThat(r).isInstanceOf(Result.Ok.class);
        return ((Result.Ok<T, ErrorBilletera>) r).valor();
    }

    private static <T> ErrorBilletera err(Result<T, ErrorBilletera> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<T, ErrorBilletera>) r).error();
    }

    @Test
    void alRegistrarseSeAbreLaBilleteraYElCelularQuedaOcupado() {
        var ana = registrarCliente("Bodega Ana");
        assertThat(saldo(ana)).isEqualTo(Dinero.CERO);
        assertThat(consultar.celularRegistrado(ana.celular())).isTrue();

        var otra = registrar.registrar(new RegistrarUsuarioCommand("12345678", LocalDate.of(1990, 1, 1),
                "otra@prueba.pe", ana.celular(), null, "Kapikua2026", true));
        assertThat(((Result.Err<UsuarioId, RegistroError>) otra).error()).isInstanceOf(RegistroError.CelularDuplicado.class);
    }

    @Test
    void laRecargaRepetidaConLaMismaReferenciaNoDuplicaElSaldo() {
        var luis = registrarCliente(null);
        var referencia = UUID.randomUUID().toString();
        var primera = ok(recargar.recargar(luis.id(), Dinero.de("500"), "tok_aprobado", referencia));
        var segunda = ok(recargar.recargar(luis.id(), Dinero.de("500"), "tok_aprobado", referencia));

        assertThat(segunda.transaccionId()).isEqualTo(primera.transaccionId());
        assertThat(saldo(luis)).isEqualTo(Dinero.de("500"));
        assertThat(movimientos(luis)).isEqualTo(1);
        assertThat(err(recargar.recargar(luis.id(), Dinero.de("3300.01"), "tok_aprobado", UUID.randomUUID().toString())))
                .isInstanceOf(ErrorBilletera.TopePorOperacion.class);
        assertThat(err(recargar.recargar(luis.id(), Dinero.de("10"), "tok_rechazado", UUID.randomUUID().toString())))
                .isInstanceOf(ErrorBilletera.PagoRechazado.class);
    }

    @Test
    void laTransferenciaExigeElCodigoYNoSeRepiteConLaMismaReferencia() {
        var luis = registrarCliente(null);
        var ana = registrarCliente(null);
        ok(recargar.recargar(luis.id(), Dinero.de("300"), "tok_aprobado", UUID.randomUUID().toString()));

        var referencia = UUID.randomUUID().toString();
        VistaPrevia vista = ok(enviar.prepararTransferencia(luis.id(), ana.celular(), Dinero.de("120"), referencia));
        assertThat(vista.destinatario()).isEqualTo("Cliente P.");

        assertThat(err(enviar.transferir(luis.id(), ana.celular(), Dinero.de("120"), referencia, "000000x")))
                .isInstanceOf(ErrorBilletera.CodigoNoValido.class);
        Comprobante c1 = ok(enviar.transferir(luis.id(), ana.celular(), Dinero.de("120"), referencia, vista.codigoMostrado()));
        Comprobante c2 = ok(enviar.transferir(luis.id(), ana.celular(), Dinero.de("120"), referencia, "cualquiera"));

        assertThat(c2.transaccionId()).isEqualTo(c1.transaccionId());
        assertThat(saldo(luis)).isEqualTo(Dinero.de("180"));
        assertThat(saldo(ana)).isEqualTo(Dinero.de("120"));
        assertThat(err(enviar.pagarNegocio(ana.id(), luis.celular(), Dinero.de("1"), referencia)))
                .isInstanceOf(ErrorBilletera.ReferenciaInvalida.class);
    }

    @Test
    void soloSePagaANegociosRegistrados() {
        var luis = registrarCliente(null);
        var ana = registrarCliente(null);
        ok(recargar.recargar(luis.id(), Dinero.de("100"), "tok_aprobado", UUID.randomUUID().toString()));
        assertThat(err(enviar.prepararPagoNegocio(luis.id(), ana.celular(), Dinero.de("10"), UUID.randomUUID().toString())))
                .isInstanceOf(ErrorBilletera.NoEsNegocio.class);
    }

    @Test
    void pagosSimultaneosNoDejanElSaldoNegativo() throws Exception {
        var luis = registrarCliente(null);
        var bodega = registrarCliente("Bodega concurrente");
        ok(recargar.recargar(luis.id(), Dinero.de("500"), "tok_aprobado", UUID.randomUUID().toString()));

        var tareas = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < 10; i++) {
            tareas.add(() -> {
                var r = enviar.pagarNegocio(luis.id(), bodega.celular(), Dinero.de("100"), UUID.randomUUID().toString());
                if (r instanceof Result.Err<Comprobante, ErrorBilletera> e) {
                    assertThat(e.error()).isInstanceOfAny(ErrorBilletera.SaldoInsuficiente.class,
                            ErrorBilletera.OperacionConcurrente.class);
                }
                return r.esOk();
            });
        }
        int exitosos;
        try (var hilos = Executors.newFixedThreadPool(10)) {
            var futuros = hilos.invokeAll(tareas);
            exitosos = 0;
            for (var f : futuros) if (f.get()) exitosos++;
        }

        assertThat(exitosos).isBetween(1, 5);
        assertThat(saldo(luis)).isEqualTo(Dinero.de("500").restar(Dinero.de(String.valueOf(100 * exitosos))));
        assertThat(saldo(bodega)).isEqualTo(Dinero.de(String.valueOf(100 * exitosos)));
        assertThat(movimientos(luis)).isEqualTo(1 + exitosos);
    }
}
