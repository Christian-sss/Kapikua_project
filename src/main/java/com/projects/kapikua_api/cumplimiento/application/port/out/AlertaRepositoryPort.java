package com.projects.kapikua_api.cumplimiento.application.port.out;

import com.projects.kapikua_api.cumplimiento.domain.model.Alerta;
import com.projects.kapikua_api.cumplimiento.domain.model.ReglaAlerta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AlertaRepositoryPort {
    boolean registrar(Alerta alerta);

    Map<ReglaAlerta, OffsetDateTime> ultimasPorRegla(Long billeteraId);

    Optional<Alerta> porId(Long id);

    List<Alerta> pendientes();

    List<Alerta> revisadas(int limite);

    boolean guardarRevision(Alerta alerta);
}
