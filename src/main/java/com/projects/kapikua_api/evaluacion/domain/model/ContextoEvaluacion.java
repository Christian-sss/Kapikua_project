package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion.DetalleScore;
import com.projects.kapikua_api.evaluacion.domain.model.ReglasScore.ReglasRecomendacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Optional;

public record ContextoEvaluacion(
        UsuarioId usuarioId,
        SolicitudCredito solicitud,
        InformeCentral central,
        DetalleScore detalleScore,
        ReglasRecomendacion reglas,
        OffsetDateTime fecha,
        Optional<OfertaViable> ofertaViable,
        Optional<String> motivoEliminatorio
) {
    public OfertaViable oferta() {
        return ofertaViable.orElseThrow();
    }

    public boolean requiereAval() {
        return ofertaViable.map(OfertaViable::requiereAval).orElse(false);
    }

    public boolean requiereAjustePorRiesgo() {
        return requiereAjustePorRiesgo(detalleScore.puntaje(), central.clasificacion(), reglas);
    }

    public static boolean requiereAjustePorRiesgo(int score, ClasificacionSbs clasificacion,
                                                  ReglasRecomendacion reglas) {
        return score < reglas.scoreApto() || clasificacion == ClasificacionSbs.CPP;
    }

    public record OfertaViable(CondicionesSugeridas condiciones, Dinero cuotaMinima, boolean requiereAval) {
        public boolean cuotaInsuficiente() {
            return condiciones.cuota().menorQue(cuotaMinima);
        }
    }
}
