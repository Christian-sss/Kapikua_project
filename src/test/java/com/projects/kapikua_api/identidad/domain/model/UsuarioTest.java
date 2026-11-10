package com.projects.kapikua_api.identidad.domain.model;

import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    private static final Optional<IdentidadVerificada> RENIEC_OK =
            Optional.of(new IdentidadVerificada("Ana", "Quispe Mamani"));

    private Result<Usuario, RegistroError> registrar(String dni, String correo, LocalDate nacimiento,
                                                     Optional<IdentidadVerificada> reniec, boolean enListas) {
        return Usuario.registrar(dni, correo, "$2a$10$hash", nacimiento, reniec, enListas, "1.0", "987654321", "Bodega", AHORA);
    }

    private Usuario registradoOk() {
        var r = registrar("45678912", "Ana@Correo.PE", LocalDate.of(1990, 5, 20), RENIEC_OK, false);
        assertThat(r).isInstanceOf(Result.Ok.class);
        return ((Result.Ok<Usuario, RegistroError>) r).valor();
    }

    private RegistroError error(Result<Usuario, RegistroError> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<Usuario, RegistroError>) r).error();
    }

    @Test
    void registraClienteValidadoConDatosDeReniecYCorreoNormalizado() {
        var u = registradoOk();

        assertThat(u.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(u.isIdentidadValidada()).isTrue();
        assertThat(u.getFechaValidacion()).isEqualTo(AHORA);
        assertThat(u.isActivo()).isTrue();
        assertThat(u.getNombres()).isEqualTo("Ana");
        assertThat(u.getApellidos()).isEqualTo("Quispe Mamani");
        assertThat(u.getCorreo()).isEqualTo("ana@correo.pe");
    }

    @Test
    void registraElConsentimientoDeTerminosVigente() {
        var u = registradoOk();

        assertThat(u.tieneConsentimientoVigente(TipoConsentimiento.TERMINOS_SERVICIO)).isTrue();
        assertThat(u.tieneConsentimientoVigente(TipoConsentimiento.EVALUACION_CON_BILLETERA)).isFalse();
        assertThat(u.consentimientos()).singleElement()
                .satisfies(c -> {
                    assertThat(c.getVersionTexto()).isEqualTo("1.0");
                    assertThat(c.getOtorgadoEn()).isEqualTo(AHORA);
                });
    }

    @Test
    void rechazaSiReniecNoConfirmaLaIdentidad() {
        assertThat(error(registrar("45678912", "a@b.pe", LocalDate.of(1990, 1, 1), Optional.empty(), false)))
                .isInstanceOf(RegistroError.DniNoValidado.class);
    }

    @Test
    void rechazaMenorDeEdadAunqueCumplaDieciochoManana() {
        var cumpleManana = AHORA.toLocalDate().minusYears(18).plusDays(1);
        assertThat(error(registrar("45678912", "a@b.pe", cumpleManana, RENIEC_OK, false)))
                .isInstanceOf(RegistroError.MenorDeEdad.class);
    }

    @Test
    void aceptaQuienCumpleDieciochoHoy() {
        var cumpleHoy = AHORA.toLocalDate().minusYears(18);
        assertThat(registrar("45678912", "a@b.pe", cumpleHoy, RENIEC_OK, false)).isInstanceOf(Result.Ok.class);
    }

    @Test
    void rechazaSiFiguraEnListasRestrictivas() {
        assertThat(error(registrar("45678912", "a@b.pe", LocalDate.of(1990, 1, 1), RENIEC_OK, true)))
                .isInstanceOf(RegistroError.EnListasRestrictivas.class);
    }

    @Test
    void rechazaDniYCorreoConFormatoInvalido() {
        assertThat(error(registrar("1234567", "a@b.pe", LocalDate.of(1990, 1, 1), RENIEC_OK, false)))
                .isEqualTo(new RegistroError.DatoInvalido("DNI"));
        assertThat(error(registrar("12345678", "sin-arroba", LocalDate.of(1990, 1, 1), RENIEC_OK, false)))
                .isEqualTo(new RegistroError.DatoInvalido("correo"));
    }

    @Test
    void enmascaraElDni() {
        assertThat(registradoOk().dniEnmascarado()).isEqualTo("****8912");
    }

    @Test
    void elUsuarioIdSoloExisteDespuesDeGuardar() {
        assertThatThrownBy(() -> registradoOk().usuarioId()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elEventoDeRegistroSeRegistraCuandoLaBaseAsignaElId() {
        var u = ((Result.Ok<Usuario, RegistroError>) registrar("45678912", "ana@correo.pe", LocalDate.of(1990, 5, 20),
                Optional.of(new IdentidadVerificada("Ana", "Quispe")), false)).valor();
        assertThat(u.extraerEventos()).isEmpty();
        org.springframework.test.util.ReflectionTestUtils.setField(u, "id", 5L);
        u.alGuardarseNuevo();
        assertThat(u.extraerEventos()).containsExactly(new com.projects.kapikua_api.identidad.domain.event.UsuarioRegistrado(
                new com.projects.kapikua_api.shared.UsuarioId(5L), "987654321", "Bodega"));
    }
}
