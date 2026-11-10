package com.projects.kapikua_api.identidad.application.service;

import com.projects.kapikua_api.identidad.application.port.in.GestionarConsentimientosUseCase;
import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@Transactional
public class GestionarConsentimientosService implements GestionarConsentimientosUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final Clock clock;
    private final String version;

    public GestionarConsentimientosService(UsuarioRepositoryPort usuarios, Clock clock,
                                           @Value("${kapikua.terminos.version:v1}") String version) {
        this.usuarios = usuarios;
        this.clock = clock;
        this.version = version;
    }

    @Override
    public boolean otorgar(UsuarioId usuarioId, TipoConsentimiento tipo) {
        var usuario = usuarios.porId(usuarioId);
        usuario.ifPresent(u -> {
            u.otorgarConsentimiento(tipo, version, OffsetDateTime.now(clock));
            usuarios.guardar(u);
        });
        return usuario.isPresent();
    }

    @Override
    public boolean revocar(UsuarioId usuarioId, TipoConsentimiento tipo) {
        var usuario = usuarios.porId(usuarioId);
        usuario.ifPresent(u -> {
            u.revocarConsentimiento(tipo, OffsetDateTime.now(clock));
            usuarios.guardar(u);
        });
        return usuario.isPresent();
    }
}
