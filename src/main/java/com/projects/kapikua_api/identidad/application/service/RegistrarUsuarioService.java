package com.projects.kapikua_api.identidad.application.service;

import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.identidad.application.port.out.CelularDisponible;
import com.projects.kapikua_api.identidad.application.port.out.CifradorContrasena;
import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import com.projects.kapikua_api.identidad.application.port.out.ValidadorIdentidad;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.identidad.domain.model.PoliticaContrasena;
import com.projects.kapikua_api.identidad.domain.model.Usuario;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.UsuarioRegistrado;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * CU-01. Primero valida todo y consulta RENIEC fuera de la transacción (SAD 2.4);
 * solo al final abre la transacción para guardar el usuario y publicar
 * {@link UsuarioRegistrado}, que Billetera escuchará en esa misma transacción.
 */
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
        // 3a. Sin aceptar los términos no se continúa.
        if (!c.aceptaTerminos()) {
            return Result.err(new RegistroError.TerminosNoAceptados());
        }
        // 4. Política de contraseña (RNF-02) y unicidad de DNI, correo y celular.
        var politica = PoliticaContrasena.validar(c.contrasena());
        if (politica instanceof Result.Err<Void, RegistroError> err) {
            return Result.err(err.error());
        }
        if (c.dni() != null && usuarios.existeDni(c.dni())) {
            return Result.err(new RegistroError.DniDuplicado());
        }
        if (c.correo() != null && usuarios.existeCorreo(c.correo().trim().toLowerCase())) {
            return Result.err(new RegistroError.CorreoDuplicado());
        }
        if (!celularDisponible.estaLibre(c.celular())) {
            return Result.err(new RegistroError.CelularDuplicado());
        }

        // 5 y 6. RENIEC y listas restrictivas, fuera de la transacción.
        var identidad = validadorIdentidad.validar(c.dni(), c.fechaNacimiento());
        var enListas = identidad.isPresent() && validadorIdentidad.figuraEnListas(c.dni());
        // TODO(Cumplimiento): con coincidencia en listas, generar una alerta (CU-01, 6a).

        // 7 y 8. El dominio aplica RN-06 y registra el consentimiento de términos.
        var registro = Usuario.registrar(c.dni(), c.correo(), cifrador.cifrar(c.contrasena()),
                c.fechaNacimiento(), identidad, enListas, versionTerminos, OffsetDateTime.now(clock));

        return switch (registro) {
            case Result.Err<Usuario, RegistroError> err -> Result.err(err.error());
            case Result.Ok<Usuario, RegistroError> ok -> Result.ok(transaccion.execute(estado -> {
                var guardado = usuarios.guardar(ok.valor());
                publicador.publicar(new UsuarioRegistrado(
                        guardado.usuarioId(), c.celular(), c.nombreNegocio()));
                return guardado.usuarioId();
            }));
        };
    }
}
