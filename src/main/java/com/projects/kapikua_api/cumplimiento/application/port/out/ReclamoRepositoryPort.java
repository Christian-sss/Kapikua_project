package com.projects.kapikua_api.cumplimiento.application.port.out;

import com.projects.kapikua_api.cumplimiento.domain.model.Reclamo;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.List;
import java.util.Optional;

public interface ReclamoRepositoryPort {

    /** Inserta y asigna el código R-AAAA-NNNNNN (RN-29). */
    Reclamo registrar(Reclamo reclamo);

    Optional<Reclamo> porId(Long id);

    List<Reclamo> deUsuario(UsuarioId usuarioId);

    List<Reclamo> pendientes();

    List<Reclamo> atendidos(int limite);

    /** Guarda la respuesta solo si seguía REGISTRADO. */
    boolean guardarRespuesta(Reclamo reclamo);

    /** Guarda la prórroga solo si seguía REGISTRADO y sin prórroga. */
    boolean guardarProrroga(Reclamo reclamo);
}
