package com.projects.kapikua_api.identidad.domain.model;

import com.projects.kapikua_api.identidad.domain.error.AutenticacionError;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.ErrorDominio;
import com.projects.kapikua_api.shared.Result;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentimientoTest {
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final OffsetDateTime DESPUES = AHORA.plusDays(3);
    static final Optional<IdentidadVerificada> RENIEC = Optional.of(new IdentidadVerificada("Ana", "Quispe"));

    static Result<Usuario, RegistroError> registrar(String passwordHash, String correo, LocalDate nacimiento, OffsetDateTime ahora) {
        return Usuario.registrar("45678912", correo, passwordHash, nacimiento, RENIEC, false, "1.0", "987654321", null, ahora);
    }

    static Usuario usuario() {
        return ((Result.Ok<Usuario, RegistroError>) registrar("$2a$10$hash", "ana@correo.pe", LocalDate.of(1990, 5, 20), AHORA)).valor();
    }

    @Test
    void otorgarUnConsentimientoNuevoLoDejaVigente() {
        var u = usuario();
        u.otorgarConsentimiento(TipoConsentimiento.EVALUACION_CON_BILLETERA, "2.1", AHORA);
        assertThat(u.tieneConsentimientoVigente(TipoConsentimiento.EVALUACION_CON_BILLETERA)).isTrue();
        assertThat(u.consentimientos()).hasSize(2);
    }

    @Test
    void otorgarDosVecesElMismoTipoNoLoDuplica() {
        var u = usuario();
        u.otorgarConsentimiento(TipoConsentimiento.EVALUACION_CON_BILLETERA, "2.1", AHORA);
        u.otorgarConsentimiento(TipoConsentimiento.EVALUACION_CON_BILLETERA, "2.2", DESPUES);
        assertThat(u.consentimientos()).filteredOn(c -> c.getTipo() == TipoConsentimiento.EVALUACION_CON_BILLETERA)
                .singleElement().satisfies(c -> assertThat(c.getVersionTexto()).isEqualTo("2.1"));
    }

    @Test
    void revocarConservaElRegistroYPermiteOtorgarDeNuevo() {
        var u = usuario();
        u.otorgarConsentimiento(TipoConsentimiento.EVALUACION_CON_BILLETERA, "2.1", AHORA);
        u.revocarConsentimiento(TipoConsentimiento.EVALUACION_CON_BILLETERA, DESPUES);

        assertThat(u.tieneConsentimientoVigente(TipoConsentimiento.EVALUACION_CON_BILLETERA)).isFalse();
        assertThat(u.consentimientos()).filteredOn(c -> c.getTipo() == TipoConsentimiento.EVALUACION_CON_BILLETERA)
                .singleElement().satisfies(c -> assertThat(c.getRevocadoEn()).isEqualTo(DESPUES));
        assertThat(u.tieneConsentimientoVigente(TipoConsentimiento.TERMINOS_SERVICIO)).isTrue();

        u.otorgarConsentimiento(TipoConsentimiento.EVALUACION_CON_BILLETERA, "2.2", DESPUES.plusDays(1));
        assertThat(u.tieneConsentimientoVigente(TipoConsentimiento.EVALUACION_CON_BILLETERA)).isTrue();
        assertThat(u.consentimientos()).hasSize(3);
    }

    @Test
    void revocarUnTipoQueNoEstaVigenteNoHaceNada() {
        var u = usuario();
        u.revocarConsentimiento(TipoConsentimiento.EVALUACION_CON_BILLETERA, DESPUES);
        assertThat(u.consentimientos()).allSatisfy(c -> assertThat(c.estaVigente()).isTrue());
    }

    @Test
    void laListaDeConsentimientosEsDeSoloLectura() {
        assertThatThrownBy(() -> usuario().consentimientos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void unConsentimientoSeRevocaUnaSolaVezYNoSeOtorgaIncompleto() {
        var c = Consentimiento.otorgar(TipoConsentimiento.TERMINOS_SERVICIO, "1.0", AHORA);
        c.revocar(DESPUES);
        assertThatThrownBy(() -> c.revocar(DESPUES.plusDays(1))).isInstanceOf(IllegalStateException.class);
        assertThat(c.getRevocadoEn()).isEqualTo(DESPUES);

        assertThatThrownBy(() -> Consentimiento.otorgar(null, "1.0", AHORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Consentimiento.otorgar(TipoConsentimiento.TERMINOS_SERVICIO, " ", AHORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Consentimiento.otorgar(TipoConsentimiento.TERMINOS_SERVICIO, null, AHORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Consentimiento.otorgar(TipoConsentimiento.TERMINOS_SERVICIO, "1.0", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laFechaDeNacimientoNoPuedeFaltarNiSerFuturaYElCorreoEsObligatorio() {
        assertThat(((Result.Err<Usuario, RegistroError>) registrar("h", "ana@correo.pe", null, AHORA)).error())
                .isEqualTo(new RegistroError.DatoInvalido("fecha de nacimiento"));
        assertThat(((Result.Err<Usuario, RegistroError>) registrar("h", "ana@correo.pe", AHORA.toLocalDate().plusDays(1), AHORA)).error())
                .isEqualTo(new RegistroError.DatoInvalido("fecha de nacimiento"));
        assertThat(((Result.Err<Usuario, RegistroError>) registrar("h", null, LocalDate.of(1990, 1, 1), AHORA)).error())
                .isEqualTo(new RegistroError.DatoInvalido("correo"));
    }

    @Test
    void sinContrasenaCifradaOSinFechaEsUnErrorDeProgramacion() {
        assertThatThrownBy(() -> registrar(" ", "ana@correo.pe", LocalDate.of(1990, 1, 1), AHORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar(null, "ana@correo.pe", LocalDate.of(1990, 1, 1), AHORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar("h", "ana@correo.pe", LocalDate.of(1990, 1, 1), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unUsuarioYaExistenteNoVuelveARegistrarElEventoAlGuardarse() {
        var u = usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(u, "celularRegistro", null);
        org.springframework.test.util.ReflectionTestUtils.setField(u, "id", 5L);
        u.alGuardarseNuevo();
        assertThat(u.extraerEventos()).isEmpty();
    }

    @Test
    void cadaErrorDeIdentidadTieneUnCodigoUnicoYUnMensaje() {
        List<ErrorDominio> errores = List.of(
                new RegistroError.DatoInvalido("DNI"), new RegistroError.ContrasenaDebil(), new RegistroError.TerminosNoAceptados(),
                new RegistroError.DniDuplicado(), new RegistroError.CorreoDuplicado(), new RegistroError.CelularDuplicado(),
                new RegistroError.DniNoValidado(), new RegistroError.MenorDeEdad(), new RegistroError.EnListasRestrictivas(),
                new AutenticacionError.CredencialesInvalidas(), new AutenticacionError.UsuarioInactivo());
        assertThat(errores).hasSize(RegistroError.class.getPermittedSubclasses().length
                + AutenticacionError.class.getPermittedSubclasses().length);
        assertThat(errores).allSatisfy(e -> {
            assertThat(e.codigo()).matches("ID-\\d{2}");
            assertThat(e.mensaje()).isNotBlank();
        });
        assertThat(new HashSet<>(errores.stream().map(ErrorDominio::codigo).toList())).hasSameSizeAs(errores);
        assertThat(new RegistroError.DatoInvalido("correo").mensaje()).contains("correo");
    }
}
