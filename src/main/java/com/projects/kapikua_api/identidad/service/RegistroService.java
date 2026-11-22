package com.projects.kapikua_api.identidad.service;

import com.projects.kapikua_api.identidad.client.CelularDisponible;
import com.projects.kapikua_api.identidad.client.ValidadorIdentidad;
import com.projects.kapikua_api.identidad.dto.RegistroForm;
import com.projects.kapikua_api.identidad.entity.DatosRegistroUsuario;
import com.projects.kapikua_api.identidad.entity.Usuario;
import com.projects.kapikua_api.identidad.error.RegistroError;
import com.projects.kapikua_api.identidad.repository.UsuarioRepository;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.UsuarioRegistrado;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

/** Registro de clientes (CU-01). */
@Service
public class RegistroService {

    private final UsuarioRepository usuarios;
    private final ValidadorIdentidad validadorIdentidad;
    private final PasswordEncoder passwordEncoder;
    private final CelularDisponible celularDisponible;
    private final PublicadorEventos publicador;
    private final Clock clock;
    private final String versionTerminos;

    public RegistroService(UsuarioRepository usuarios,
                           ValidadorIdentidad validadorIdentidad,
                           PasswordEncoder passwordEncoder,
                           CelularDisponible celularDisponible,
                           PublicadorEventos publicador,
                           Clock clock,
                           @Value("${kapikua.terminos.version:v1}") String versionTerminos) {
        this.usuarios = usuarios;
        this.validadorIdentidad = validadorIdentidad;
        this.passwordEncoder = passwordEncoder;
        this.celularDisponible = celularDisponible;
        this.publicador = publicador;
        this.clock = clock;
        this.versionTerminos = versionTerminos;
    }

    @Transactional
    public Result<UsuarioId, RegistroError> registrar(RegistroForm f) {

        if (!f.isAceptaTerminos()) {
            return Result.err(new RegistroError.TerminosNoAceptados());
        }
        var politica = PoliticaContrasena.validar(f.getContrasena());
        if (politica instanceof Result.Err<Void, RegistroError> err) {
            return Result.err(err.error());
        }
        var datos = new DatosRegistroUsuario(f.getDni(), f.getCorreo(), f.getCelular(), f.getFechaNacimiento());
        if (datos.dni() != null && usuarios.existsByDni(datos.dni())) {
            return Result.err(new RegistroError.DniDuplicado());
        }
        if (f.getCorreo() != null && usuarios.existsByCorreo(datos.correo())) {
            return Result.err(new RegistroError.CorreoDuplicado());
        }
        if (f.getCelular() == null) {
            return Result.err(new RegistroError.DatoInvalido("celular"));
        }
        if (!celularDisponible.estaLibre(f.getCelular())) {
            return Result.err(new RegistroError.CelularDuplicado());
        }

        // Sin nombres escritos se usan los que devuelva RENIEC; con nombres, deben coincidir (CU-01, flujo 5a).
        var identidad = f.getNombres() == null && f.getApellidos() == null
                ? validadorIdentidad.validar(datos.dni(), datos.fechaNacimiento())
                : validadorIdentidad.validar(datos.dni(), datos.fechaNacimiento(), f.getNombres(), f.getApellidos());
        var enListas = identidad.isPresent() && validadorIdentidad.figuraEnListas(datos.dni());

        var registro = Usuario.registrar(datos, passwordEncoder.encode(f.getContrasena()), identidad, enListas,
                versionTerminos, OffsetDateTime.now(clock));

        return switch (registro) {
            case Result.Err<Usuario, RegistroError> err -> Result.err(err.error());
            case Result.Ok<Usuario, RegistroError> ok -> {
                var usuarioId = usuarios.save(ok.valor()).usuarioId();
                publicador.publicar(new UsuarioRegistrado(usuarioId, f.getCelular(), nombreNegocio(f)));
                yield Result.ok(usuarioId);
            }
        };
    }

    private static String nombreNegocio(RegistroForm f) {
        var negocio = f.getNombreNegocio();
        return negocio == null || negocio.isBlank() ? null : negocio.trim();
    }
}
