package com.projects.kapikua_api.identidad.application.service;

import com.projects.kapikua_api.identidad.application.port.in.AutenticarUseCase;
import com.projects.kapikua_api.identidad.application.port.out.CifradorContrasena;
import com.projects.kapikua_api.identidad.application.port.out.EmisorToken;
import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import com.projects.kapikua_api.identidad.domain.error.AutenticacionError;
import com.projects.kapikua_api.identidad.domain.model.Usuario;
import com.projects.kapikua_api.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * CU-02. Verifica la contraseña con BCrypt y emite el token con el rol.
 * Cuando el usuario no existe igual se compara contra un hash de relleno,
 * para que el tiempo de respuesta no revele qué correos o DNI están registrados.
 */
@Service
public class AutenticarService implements AutenticarUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutenticarService.class);

    private final UsuarioRepositoryPort usuarios;
    private final CifradorContrasena cifrador;
    private final EmisorToken emisorToken;
    private final String hashRelleno;

    public AutenticarService(UsuarioRepositoryPort usuarios, CifradorContrasena cifrador, EmisorToken emisorToken) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.emisorToken = emisorToken;
        this.hashRelleno = cifrador.cifrar("kapikua-relleno-sin-uso");
    }

    @Override
    public Result<SesionEmitida, AutenticacionError> autenticar(String correoODni, String contrasena) {
        var clave = contrasena == null ? "" : contrasena;
        Optional<Usuario> usuario = (correoODni == null || correoODni.isBlank())
                ? Optional.empty()
                : usuarios.porCorreoODni(correoODni);

        if (usuario.isEmpty()) {
            cifrador.coincide(clave, hashRelleno);
            log.info("Inicio de sesión rechazado: credenciales inválidas");
            return Result.err(new AutenticacionError.CredencialesInvalidas());
        }
        var u = usuario.get();
        if (!cifrador.coincide(clave, u.getPasswordHash())) {
            log.info("Inicio de sesión rechazado: credenciales inválidas (usuarioId={})", u.getId());
            return Result.err(new AutenticacionError.CredencialesInvalidas());
        }
        if (!u.isActivo()) {
            log.info("Inicio de sesión rechazado: usuario inactivo (usuarioId={})", u.getId());
            return Result.err(new AutenticacionError.UsuarioInactivo());
        }

        log.info("Inicio de sesión exitoso (usuarioId={}, rol={})", u.getId(), u.getRol());
        return Result.ok(new SesionEmitida(u.usuarioId(), u.getRol(), emisorToken.emitir(u.usuarioId(), u.getRol())));
    }
}
