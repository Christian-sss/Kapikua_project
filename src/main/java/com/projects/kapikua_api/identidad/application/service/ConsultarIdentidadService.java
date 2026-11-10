package com.projects.kapikua_api.identidad.application.service;

import com.projects.kapikua_api.identidad.application.port.in.ConsultarIdentidadUseCase;
import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ConsultarIdentidadService implements ConsultarIdentidadUseCase {

    private final UsuarioRepositoryPort usuarios;

    public ConsultarIdentidadService(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<DatosIdentidad> porId(UsuarioId usuarioId) {
        return usuarios.porId(usuarioId).map(u -> new DatosIdentidad(u.usuarioId(), u.getDni(), u.getNombres(),
                u.getApellidos(), u.getCorreo(), u.dniEnmascarado(), u.isIdentidadValidada(), u.isActivo()));
    }

    @Override
    public boolean consentimientoVigente(UsuarioId usuarioId, TipoConsentimiento tipo) {
        return usuarios.porId(usuarioId).map(u -> u.tieneConsentimientoVigente(tipo)).orElse(false);
    }
}
