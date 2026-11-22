package com.projects.kapikua_api.cumplimiento.dto;

import com.projects.kapikua_api.cumplimiento.model.EstadoAlerta;
import com.projects.kapikua_api.cumplimiento.model.ReglaAlerta;

import java.time.OffsetDateTime;

public record ResumenAlerta(Long id, ReglaAlerta regla, Cliente cliente, String detalle, OffsetDateTime fecha,
                            EstadoAlerta estado) {}
