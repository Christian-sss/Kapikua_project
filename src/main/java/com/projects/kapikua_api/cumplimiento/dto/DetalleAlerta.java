package com.projects.kapikua_api.cumplimiento.dto;

import com.projects.kapikua_api.cumplimiento.model.OperacionObservada;

import java.time.OffsetDateTime;
import java.util.List;

public record DetalleAlerta(ResumenAlerta alerta, Long transaccionId, List<OperacionObservada> operaciones,
                            String comentario, String revisadoPor, OffsetDateTime fechaRevision,
                            boolean billeteraBloqueada) {}
