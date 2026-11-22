package com.projects.kapikua_api.credito.model;

import java.time.OffsetDateTime;

public record CicloGarantia(
        OffsetDateTime fechaAviso,
        OffsetDateTime fechaActivacion,
        OffsetDateTime fechaLiberacion
) {

    public static final CicloGarantia NINGUNO = new CicloGarantia(null, null, null);

    public CicloGarantia conAviso(OffsetDateTime f) {
        return new CicloGarantia(f, fechaActivacion, fechaLiberacion);
    }

    public CicloGarantia conActivacion(OffsetDateTime f) {
        return new CicloGarantia(fechaAviso, f, fechaLiberacion);
    }

    public CicloGarantia conLiberacion(OffsetDateTime f) {
        return new CicloGarantia(fechaAviso, fechaActivacion, f);
    }
}
