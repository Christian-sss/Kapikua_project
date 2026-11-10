package com.projects.kapikua_api.cumplimiento.application.port.out;

import com.projects.kapikua_api.cumplimiento.domain.model.Alerta;
import com.projects.kapikua_api.cumplimiento.domain.model.ReglaAlerta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AlertaRepositoryPort {

    /** Idempotente: la misma regla no se registra dos veces para la misma operación. Devuelve si la insertó. */
    boolean registrar(Alerta alerta);

    /** La fecha de la última alerta de cada regla para la billetera. */
    Map<ReglaAlerta, OffsetDateTime> ultimasPorRegla(Long billeteraId);

    Optional<Alerta> porId(Long id);

    List<Alerta> pendientes();

    List<Alerta> revisadas(int limite);

    /** Guarda la revisión solo si la alerta seguía PENDIENTE; devuelve false si otro la revisó antes. */
    boolean guardarRevision(Alerta alerta);
}
