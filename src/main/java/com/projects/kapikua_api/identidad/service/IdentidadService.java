package com.projects.kapikua_api.identidad.service;

import com.projects.kapikua_api.identidad.dto.DatosIdentidad;
import com.projects.kapikua_api.identidad.entity.TipoConsentimiento;
import com.projects.kapikua_api.identidad.repository.UsuarioRepository;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Consultas de identidad que usan los demás contextos (Billetera, Evaluación, Crédito, Cumplimiento). */
@Service
@Transactional(readOnly = true)
public class IdentidadService {

    private final UsuarioRepository usuarios;

    public IdentidadService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<DatosIdentidad> porId(UsuarioId usuarioId) {
        return usuarios
                .porId(usuarioId)
                .map(u -> new DatosIdentidad(
                        u.usuarioId(),
                        u.getDni(),
                        u.getNombres(),
                        u.getApellidos(),
                        u.getCorreo(),
                        u.dniEnmascarado(),
                        u.isIdentidadValidada(),
                        u.isActivo()));
    }

    public String nombreEnmascarado(UsuarioId usuarioId) {
        return usuarios.porId(usuarioId).map(usuario -> {
            var primerNombre = usuario.getNombres().trim().split("\\s+")[0];
            var apellidos = usuario.getApellidos();
            var inicial = apellidos.isBlank() ? "" : Character.toUpperCase(apellidos.trim().charAt(0)) + ".";
            return primerNombre + " " + inicial;
        }).orElse("Usuario Kapikua");
    }

    public boolean consentimientoVigente(UsuarioId usuarioId, TipoConsentimiento tipo) {
        return usuarios.porId(usuarioId).map(u -> u.tieneConsentimientoVigente(tipo)).orElse(false);
    }
}
