package com.projects.kapikua_api.credito.dto;

/** Lo que responde la solicitud: la oferta creada (si fue apta) y la recomendación. */
public record ResultadoSolicitud(Long ofertaId, String recomendacion, String motivo, boolean requiereAval) {
    public boolean tieneOferta() {
        return ofertaId != null;
    }
}
