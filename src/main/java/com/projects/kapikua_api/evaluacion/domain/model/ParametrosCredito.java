package com.projects.kapikua_api.evaluacion.domain.model;

public record ParametrosCredito(UsoMinimo usoMinimo, LimitesCredito limitesCredito, ReglasScore score,
                                ReglasRetencion retencion, ReglasNivel reglasNivel) {}
