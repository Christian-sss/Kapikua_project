package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.model.ContextoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;

final class RecomendacionAptoConAjuste implements EstrategiaRecomendacion {
    @Override
    public boolean aplica(ContextoEvaluacion contexto) {
        return true;
    }

    @Override
    public Evaluacion evaluar(ContextoEvaluacion contexto) {
        return Evaluacion.aptoConAjuste(contexto, contexto.oferta().condiciones(), motivo(contexto));
    }

    private static String motivo(ContextoEvaluacion contexto) {
        if (!contexto.requiereAjustePorRiesgo()) {
            return "El monto solicitado supera el límite de la política: se ofrece hasta "
                    + contexto.oferta().condiciones().monto() + ".";
        }
        var score = contexto.detalleScore().puntaje();
        var causa = score < contexto.reglas().scoreApto() ? "Score de " + score
                : "Clasificación CPP en el sistema financiero";
        var porcentaje = contexto.reglas().porcentajeAjuste().stripTrailingZeros().toPlainString();
        return causa + ": se ofrece hasta el " + porcentaje + " % de lo pedido.";
    }
}
