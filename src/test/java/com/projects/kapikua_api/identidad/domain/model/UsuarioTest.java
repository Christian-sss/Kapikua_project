package com.projects.kapikua_api.identidad.domain.model;

import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        return registrar(dni, correo, "945678912", nacimiento, reniec, enListas);
    }

    private Result<Usuario, RegistroError> registrar(String dni, String correo, String celular, LocalDate nacimiento,
                                                     Optional<IdentidadVerificada> reniec, boolean enListas) {
        return Usuario.registrar(new DatosRegistroUsuario(dni, correo, celular, nacimiento), "$2a$10$hash", reniec,
                enListas, "1.0", AHORA);
    }

    private Result<Usuario, RegistroError> registrarCon(String dni, String celular, String correo) {
        return registrar(dni, correo, celular, LocalDate.of(1990, 5, 20), RENIEC_OK, false);
    }

    private Usuario registradoOk() {
        var r = registrar("45678912", "  Ana@Gmail.COM ", LocalDate.of(1990, 5, 20), RENIEC_OK, false);
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
        assertThat(u.getCorreo()).isEqualTo("ana@gmail.com");
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
        assertThat(error(registrar("45678912", "a@gmail.com", LocalDate.of(1990, 1, 1), Optional.empty(), false)))
                .isInstanceOf(RegistroError.DniNoValidado.class);
    }

    @Test
    void rechazaMenorDeEdadAunqueCumplaDieciochoManana() {
        var cumpleManana = AHORA.toLocalDate().minusYears(18).plusDays(1);
        assertThat(error(registrar("45678912", "a@gmail.com", cumpleManana, RENIEC_OK, false)))
                .isInstanceOf(RegistroError.MenorDeEdad.class);
    }

    @Test
    void aceptaQuienCumpleDieciochoHoy() {
        var cumpleHoy = AHORA.toLocalDate().minusYears(18);
        assertThat(registrar("45678912", "a@gmail.com", cumpleHoy, RENIEC_OK, false)).isInstanceOf(Result.Ok.class);
    }

    @Test
    void rechazaSiFiguraEnListasRestrictivas() {
        assertThat(error(registrar("45678912", "a@gmail.com", LocalDate.of(1990, 1, 1), RENIEC_OK, true)))
                .isInstanceOf(RegistroError.EnListasRestrictivas.class);
    }

    @Test
    void rechazaDniYCorreoConFormatoInvalido() {
        assertThat(error(registrar("1234567", "sin-arroba", null, Optional.empty(), true)))
                .isEqualTo(new RegistroError.DatoInvalido("DNI"));
        assertThat(error(registrar("45678912", "sin-arroba", null, Optional.empty(), true)))
                .isEqualTo(new RegistroError.DatoInvalido("correo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000", "11111111", "99999999", "12345678", "01234567", "23456789",
            "87654321", "98765432", "76543210", "0123456789", "00000000000", "1234567", ""})
    void rechazaDnisFicticiosOConFormatoInvalido(String dni) {
        assertThat(error(registrarCon(dni, "945678912", "ana@gmail.com")))
                .isEqualTo(new RegistroError.DatoInvalido("DNI"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"45678912", "11111112", "12345679", "10203040", "73982461"})
    void aceptaDnisQueSoloSeParecenAFicticios(String dni) {
        assertThat(registrarCon(dni, "945678912", "ana@gmail.com")).isInstanceOf(Result.Ok.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999", "900000000", "911111111", "912345678", "987654321", "901234567",
            "111111111", "812345678", "0123456789", "9123", ""})
    void rechazaCelularesFicticiosOConFormatoInvalido(String celular) {
        assertThat(error(registrarCon("45678912", celular, "ana@gmail.com")))
                .isEqualTo(new RegistroError.DatoInvalido("celular"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"945678912", "911111112", "912345679", "999999998", "945 678 912"})
    void aceptaCelularesReales(String celular) {
        assertThat(registrarCon("45678912", celular, "ana@gmail.com")).isInstanceOf(Result.Ok.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"@sdfdsfsdf.", "000232@e2321312", "000232@e2321312.123", "x@-.-", "a@b.c",
            "ana..perez@gmail.com", ".ana@gmail.com", "ana.@gmail.com", "ana@correo..pe", "ana@-correo.pe",
            "ana correo@x.pe", "ana@correo", "anacorreo.pe", ""})
    void rechazaCorreosConFormatoInvalido(String correo) {
        assertThat(error(registrarCon("45678912", "945678912", correo)))
                .isEqualTo(new RegistroError.DatoInvalido("correo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@gmail.com", "ana.perez+kapikua@gmail.com", "user_1@virtual.upt.pe", "a@gmail.com",
            "  Ana@Gmail.COM "})
    void aceptaCorreosValidos(String correo) {
        assertThat(registrarCon("45678912", "945678912", correo)).isInstanceOf(Result.Ok.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@gmail.com", "ana@outlook.com", "ana@kapikua.com", "ana@upt.pe",
            "ana@virtual.upt.pe", "ANA@GMAIL.COM"})
    void aceptaSoloLosDominiosPermitidos(String correo) {
        assertThat(registrarCon("45678912", "945678912", correo)).isInstanceOf(Result.Ok.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@sdfdsfsdf.com", "ana@hotmail.com", "ana@yahoo.com", "ana@kapikua.pe",
            "ana@mail.gmail.com", "ana@gmail.com.pe", "ana@otro.upt.pe", "ana@xupt.pe", "ana@outlook.es"})
    void rechazaCorreosDeOtrosDominios(String correo) {
        assertThat(error(registrarCon("45678912", "945678912", correo)))
                .isInstanceOf(RegistroError.DominioCorreoNoPermitido.class)
                .extracting(RegistroError::mensaje)
                .isEqualTo("Solo se aceptan correos de gmail.com, outlook.com, kapikua.com, upt.pe, virtual.upt.pe.");
    }

    @Test
    void enmascaraElDni() {
        assertThat(registradoOk().dniEnmascarado()).isEqualTo("****8912");
    }

    @Test
    void elUsuarioIdSoloExisteDespuesDeGuardar() {
        assertThatThrownBy(() -> registradoOk().usuarioId()).isInstanceOf(IllegalStateException.class);
    }

}
