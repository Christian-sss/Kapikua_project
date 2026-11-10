package com.projects.kapikua_api.identidad.application.service;

import com.projects.kapikua_api.identidad.application.port.in.AutenticarUseCase.SesionEmitida;
import com.projects.kapikua_api.identidad.application.port.out.CifradorContrasena;
import com.projects.kapikua_api.identidad.application.port.out.EmisorToken;
import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import com.projects.kapikua_api.identidad.domain.error.AutenticacionError;
import com.projects.kapikua_api.identidad.domain.model.IdentidadVerificada;
import com.projects.kapikua_api.identidad.domain.model.Rol;
import com.projects.kapikua_api.identidad.domain.model.Usuario;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AutenticarServiceTest {
    private final RepositorioEnMemoria usuarios = new RepositorioEnMemoria();
    private final CifradorFalso cifrador = new CifradorFalso();
    private final EmisorToken emisor = (id, rol) -> "token-" + id.valor() + "-" + rol;
    private AutenticarService servicio;

    @BeforeEach
    void preparar() throws Exception {
        usuarios.agregar(usuario(1L, "45678912", "ana@correo.pe", "Kapikua2026", true));
        usuarios.agregar(usuario(2L, "11223344", "inactivo@correo.pe", "Kapikua2026", false));
        servicio = new AutenticarService(usuarios, cifrador, emisor);
        cifrador.comparaciones = 0;
    }

    @Test
    void ingresaConCorreoYRecibeTokenConSuRol() {
        var sesion = ok(servicio.autenticar("ana@correo.pe", "Kapikua2026"));

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
        assertThat(err(servicio.autenticar("ana@correo.pe", "otraClave1")))
                .isInstanceOf(AutenticacionError.CredencialesInvalidas.class);
        assertThat(err(servicio.autenticar("nadie@correo.pe", "Kapikua2026")))
                .isInstanceOf(AutenticacionError.CredencialesInvalidas.class);
    }

    @Test
    void conUsuarioInexistenteIgualComparaLaContrasena() {
        servicio.autenticar("nadie@correo.pe", "Kapikua2026");
        assertThat(cifrador.comparaciones).isEqualTo(1);
    }

    @Test
    void usuarioInactivoSoloSeInformaConLaContrasenaCorrecta() {
        assertThat(err(servicio.autenticar("inactivo@correo.pe", "Kapikua2026")))
                .isInstanceOf(AutenticacionError.UsuarioInactivo.class);
        assertThat(err(servicio.autenticar("inactivo@correo.pe", "malaClave1")))
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
        var r = Usuario.registrar(dni, correo, cifrador.cifrar(clave), LocalDate.of(1990, 1, 1),
                Optional.of(new IdentidadVerificada("Ana", "Quispe")), false, "1.0", "987654321", null,
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

    private static class CifradorFalso implements CifradorContrasena {
        int comparaciones;
        public String cifrar(String clave) { return "hash:" + clave; }
        public boolean coincide(String clave, String hash) { comparaciones++; return hash.equals("hash:" + clave); }
    }

    private static class RepositorioEnMemoria implements UsuarioRepositoryPort {
        private final List<Usuario> datos = new ArrayList<>();
        void agregar(Usuario u) { datos.add(u); }
        public Optional<Usuario> porId(UsuarioId id) {
            return datos.stream().filter(u -> u.getId().equals(id.valor())).findFirst();
        }
        public boolean existeDni(String dni) { return datos.stream().anyMatch(u -> u.getDni().equals(dni)); }
        public boolean existeCorreo(String correo) { return datos.stream().anyMatch(u -> u.getCorreo().equals(correo)); }
        public Optional<Usuario> porCorreoODni(String v) {
            return datos.stream().filter(u -> u.getCorreo().equals(v.toLowerCase()) || u.getDni().equals(v)).findFirst();
        }
        public Usuario guardar(Usuario u) { datos.add(u); return u; }
    }
}
