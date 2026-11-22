package com.projects.kapikua_api.cumplimiento.dto;

import com.projects.kapikua_api.cumplimiento.model.EstadoReclamo;
import com.projects.kapikua_api.cumplimiento.model.MotivoReclamo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record VistaReclamo(Long id, String codigo, MotivoReclamo motivo, String detalle, EstadoReclamo estado,
                           OffsetDateTime fechaRegistro, LocalDate fechaLimite, int diasHabilesRestantes,
                           boolean cercaDelLimite, boolean fueraDePlazo, String prorroga, String respuesta,
                           OffsetDateTime fechaRespuesta, Cliente cliente) {}
