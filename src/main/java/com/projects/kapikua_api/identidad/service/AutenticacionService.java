package com.projects.kapikua_api.identidad.service;

import com.projects.kapikua_api.identidad.dto.SesionEmitida;
import com.projects.kapikua_api.identidad.entity.Usuario;
import com.projects.kapikua_api.identidad.error.AutenticacionError;
import com.projects.kapikua_api.identidad.repository.UsuarioRepository;
import com.projects.kapikua_api.identidad.security.JwtServicio;
import com.projects.kapikua_api.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AutenticacionService {
    private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtServicio jwt;
    private final String hashRelleno;

    public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtServicio jwt) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.hashRelleno = passwordEncoder.encode("kapikua-relleno-sin-uso");
    }

    public Result<SesionEmitida, AutenticacionError> autenticar(String correoODni, String contrasena) {
        var clave = contrasena == null ? "" : contrasena;
        Optional<Usuario> usuario = (correoODni == null || correoODni.isBlank())
                ? Optional.empty()
                : usuarios.porCorreoODni(correoODni);

        if (usuario.isEmpty()) {
            // Se compara igual contra un hash de relleno para que el tiempo de respuesta no revele si el usuario existe.
            passwordEncoder.matches(clave, hashRelleno);
            log.info("Inicio de sesión rechazado: credenciales inválidas");
            return Result.err(new AutenticacionError.CredencialesInvalidas());
        }
        var u = usuario.get();
        if (!passwordEncoder.matches(clave, u.getPasswordHash())) {
            log.info("Inicio de sesión rechazado: credenciales inválidas (usuarioId={})", u.getId());
            return Result.err(new AutenticacionError.CredencialesInvalidas());
        }
        if (!u.isActivo()) {
            log.info("Inicio de sesión rechazado: usuario inactivo (usuarioId={})", u.getId());
            return Result.err(new AutenticacionError.UsuarioInactivo());
        }

        log.info("Inicio de sesión exitoso (usuarioId={}, rol={})", u.getId(), u.getRol());
        return Result.ok(new SesionEmitida(u.usuarioId(), u.getRol(), jwt.emitir(u.usuarioId(), u.getRol())));
    }
}
