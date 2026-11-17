package com.projects.kapikua_api.identidad.application.service;

import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.identidad.application.port.out.CelularDisponible;
import com.projects.kapikua_api.identidad.application.port.out.CifradorContrasena;
import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import com.projects.kapikua_api.identidad.application.port.out.ValidadorIdentidad;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.identidad.domain.model.DatosRegistroUsuario;
import com.projects.kapikua_api.identidad.domain.model.Usuario;
import com.projects.kapikua_api.identidad.domain.service.PoliticaContrasena;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.UsuarioRegistrado;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {
    private final UsuarioRepositoryPort usuarios;
    private final ValidadorIdentidad validadorIdentidad;
    private final CifradorContrasena cifrador;
    private final CelularDisponible celularDisponible;
    private final PublicadorEventos publicador;
    private final TransactionTemplate transaccion;
    private final Clock clock;
    private final String versionTerminos;

    public RegistrarUsuarioService(UsuarioRepositoryPort usuarios,
                                   ValidadorIdentidad validadorIdentidad,
                                   CifradorContrasena cifrador,
                                   CelularDisponible celularDisponible,
                                   PublicadorEventos publicador,
                                   TransactionTemplate transaccion,
                                   Clock clock,
                                   @Value("${kapikua.terminos.version:v1}") String versionTerminos) {
        this.usuarios = usuarios;
        this.validadorIdentidad = validadorIdentidad;
        this.cifrador = cifrador;
        this.celularDisponible = celularDisponible;
        this.publicador = publicador;
        this.transaccion = transaccion;
        this.clock = clock;
        this.versionTerminos = versionTerminos;
    }

    @Override
    public Result<UsuarioId, RegistroError> registrar(RegistrarUsuarioCommand c) {

        if (!c.aceptaTerminos()) {
            return Result.err(new RegistroError.TerminosNoAceptados());
        }
        var politica = PoliticaContrasena.validar(c.contrasena());
        if (politica instanceof Result.Err<Void, RegistroError> err) {
            return Result.err(err.error());
        }
        var datos = new DatosRegistroUsuario(c.dni(), c.correo(), c.fechaNacimiento());
        if (datos.dni() != null && usuarios.existeDni(datos.dni())) {
            return Result.err(new RegistroError.DniDuplicado());
        }
        if (c.correo() != null && usuarios.existeCorreo(datos.correo())) {
            return Result.err(new RegistroError.CorreoDuplicado());
        }
        if (c.celular() == null) {
            return Result.err(new RegistroError.DatoInvalido("celular"));
        }
        if (!celularDisponible.estaLibre(c.celular())) {
            return Result.err(new RegistroError.CelularDuplicado());
        }

        var identidad = validadorIdentidad.validar(datos.dni(), datos.fechaNacimiento());
        var enListas = identidad.isPresent() && validadorIdentidad.figuraEnListas(datos.dni());

        var registro = Usuario.registrar(datos, cifrador.cifrar(c.contrasena()), identidad, enListas, versionTerminos,
                OffsetDateTime.now(clock));

        return switch (registro) {
            case Result.Err<Usuario, RegistroError> err -> Result.err(err.error());
            case Result.Ok<Usuario, RegistroError> ok -> Result.ok(transaccion.execute(estado -> {
                var usuarioId = usuarios.guardar(ok.valor()).usuarioId();
                publicador.publicar(new UsuarioRegistrado(usuarioId, c.celular(), c.nombreNegocio()));
                return usuarioId;
            }));
        };
    }
}
