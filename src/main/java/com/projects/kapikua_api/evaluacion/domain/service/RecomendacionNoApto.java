package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.model.ContextoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;

final class RecomendacionNoApto implements EstrategiaRecomendacion {
    @Override
    public boolean aplica(ContextoEvaluacion contexto) {
        return contexto.motivoEliminatorio().isPresent()
                || scoreInsuficiente(contexto)
                || contexto.oferta().cuotaInsuficiente();
    }

    @Override
    public Evaluacion evaluar(ContextoEvaluacion contexto) {
        var motivo = contexto.motivoEliminatorio().orElseGet(() -> motivoPorResultado(contexto));
        return Evaluacion.noApto(contexto, motivo);
    }

    private static boolean scoreInsuficiente(ContextoEvaluacion contexto) {
        return contexto.detalleScore().puntaje() < contexto.reglas().scoreAjuste();
    }

    private static String motivoPorResultado(ContextoEvaluacion contexto) {
        if (scoreInsuficiente(contexto)) {
            return "Score de " + contexto.detalleScore().puntaje() + ", menor que " + contexto.reglas().scoreAjuste() + ".";
        }
        var oferta = contexto.oferta();
        return "Tu capacidad de pago permite una cuota de " + oferta.condiciones().cuota()
                + ", menor que la cuota mínima de " + oferta.cuotaMinima() + ".";
    }
}
