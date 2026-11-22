package com.projects.kapikua_api.identidad.service;

import com.projects.kapikua_api.identidad.dto.SesionEmitida;
import com.projects.kapikua_api.identidad.repository.UsuarioRepository;
import com.projects.kapikua_api.identidad.security.JwtServicio;
import com.projects.kapikua_api.identidad.error.AutenticacionError;
import com.projects.kapikua_api.identidad.entity.DatosRegistroUsuario;
import com.projects.kapikua_api.identidad.entity.IdentidadVerificada;
import com.projects.kapikua_api.identidad.entity.Rol;
import com.projects.kapikua_api.identidad.entity.Usuario;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticacionServiceTest {
    private final List<Usuario> registrados = new ArrayList<>();
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final JwtServicio jwt = mock(JwtServicio.class);
    private final CifradorFalso cifrador = new CifradorFalso();
    private AutenticacionService servicio;

    @BeforeEach
    void preparar() throws Exception {
        registrados.add(usuario(1L, "45678912", "ana@gmail.com", "Kapikua2026", true));
        registrados.add(usuario(2L, "11223344", "inactivo@gmail.com", "Kapikua2026", false));
        when(usuarios.porCorreoODni(anyString())).thenAnswer(i -> buscar(i.getArgument(0)));
        when(jwt.emitir(any(), any())).thenAnswer(i ->
                "token-" + i.<UsuarioId>getArgument(0).valor() + "-" + i.getArgument(1));
        servicio = new AutenticacionService(usuarios, cifrador, jwt);
        cifrador.comparaciones = 0;
    }

    private Optional<Usuario> buscar(String v) {
        return registrados.stream().filter(u -> u.getCorreo().equals(v.toLowerCase()) || u.getDni().equals(v)).findFirst();
    }

    @Test
    void ingresaConCorreoYRecibeTokenConSuRol() {
        var sesion = ok(servicio.autenticar("ana@gmail.com", "Kapikua2026"));

        assertThat(sesion.usuarioId()).isEqualTo(new UsuarioId(1L));
        assertThat(sesion.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(sesion.token()).isEqualTo("token-1-CLIENTE");
    }

    @Test
    void ingresaConDni() {
        assertThat(ok(servicio.autenticar("45678912", "Kapikua2026")).usuarioId()).isEqualTo(new UsuarioId(1L));
    }

    @Test
    void contrasenaIncorrectaYUsuarioInexistenteDanElMismoError() {
        assertThat(err(servicio.autenticar("ana@gmail.com", "otraClave1")))
                .isInstanceOf(AutenticacionError.CredencialesInvalidas.class);
        assertThat(err(servicio.autenticar("nadie@gmail.com", "Kapikua2026")))
                .isInstanceOf(AutenticacionError.CredencialesInvalidas.class);
    }

    @Test
    void conUsuarioInexistenteIgualComparaLaContrasena() {
        servicio.autenticar("nadie@gmail.com", "Kapikua2026");
        assertThat(cifrador.comparaciones).isEqualTo(1);
    }

    @Test
    void usuarioInactivoSoloSeInformaConLaContrasenaCorrecta() {
        assertThat(err(servicio.autenticar("inactivo@gmail.com", "Kapikua2026")))
                .isInstanceOf(AutenticacionError.UsuarioInactivo.class);
        assertThat(err(servicio.autenticar("inactivo@gmail.com", "malaClave1")))
                .isInstanceOf(AutenticacionError.CredencialesInvalidas.class);
    }

    @Test
    void usuarioVacioEsCredencialInvalida() {
        assertThat(err(servicio.autenticar("  ", "Kapikua2026")))
                .isInstanceOf(AutenticacionError.CredencialesInvalidas.class);
    }

    private static SesionEmitida ok(Result<SesionEmitida, AutenticacionError> r) {
        assertThat(r).isInstanceOf(Result.Ok.class);
        return ((Result.Ok<SesionEmitida, AutenticacionError>) r).valor();
    }

    private static AutenticacionError err(Result<SesionEmitida, AutenticacionError> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<SesionEmitida, AutenticacionError>) r).error();
    }

    private Usuario usuario(Long id, String dni, String correo, String clave, boolean activo) throws Exception {
        var datos = new DatosRegistroUsuario(dni, correo, "945678912", LocalDate.of(1990, 1, 1));
        var r = Usuario.registrar(datos, cifrador.encode(clave), Optional.of(new IdentidadVerificada("Ana", "Quispe")), false, "1.0",
                OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5)));
        var u = ((Result.Ok<Usuario, ?>) r).valor();
        asignar(u, "id", id);
        asignar(u, "activo", activo);
        return u;
    }

    private static void asignar(Object objeto, String campo, Object valor) throws Exception {
        Field f = Usuario.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(objeto, valor);
    }

    private static class CifradorFalso implements PasswordEncoder {
        int comparaciones;
        public String encode(CharSequence clave) { return "hash:" + clave; }
        public boolean matches(CharSequence clave, String hash) { comparaciones++; return hash.equals("hash:" + clave); }
    }
}
