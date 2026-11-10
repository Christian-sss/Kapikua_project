package com.projects.kapikua_api.cumplimiento.application.port.out;

import com.projects.kapikua_api.cumplimiento.domain.model.Reclamo;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.List;
import java.util.Optional;

public interface ReclamoRepositoryPort {
    Reclamo registrar(Reclamo reclamo);

    Optional<Reclamo> porId(Long id);

    List<Reclamo> deUsuario(UsuarioId usuarioId);

    List<Reclamo> pendientes();

    List<Reclamo> atendidos(int limite);

    boolean guardarRespuesta(Reclamo reclamo);

    boolean guardarProrroga(Reclamo reclamo);
}
