package com.projects.kapikua_api.evaluacion.infrastructure.adapter.out.externo;

import com.projects.kapikua_api.evaluacion.application.port.out.CentralDeRiesgos;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.ClasificacionSbs;
import com.projects.kapikua_api.evaluacion.domain.model.InformeCentral;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.springframework.stereotype.Component;

@Component
class CentralDeRiesgosSimulada implements CentralDeRiesgos {
    @Override
    public Result<InformeCentral, ErrorEvaluacion> consultar(String dni) {
        if (dni == null || dni.isBlank()) return Result.err(new ErrorEvaluacion.CentralNoDisponible());
        return Result.ok(switch (dni.charAt(dni.length() - 1)) {
            case '4', '5' -> new InformeCentral(ClasificacionSbs.NORMAL, Dinero.de("1500"));
            case '6' -> new InformeCentral(ClasificacionSbs.NORMAL, Dinero.de("19500"));
            case '7' -> new InformeCentral(ClasificacionSbs.CPP, Dinero.de("800"));
            case '8' -> new InformeCentral(ClasificacionSbs.DEFICIENTE, Dinero.de("3000"));
            case '9' -> new InformeCentral(ClasificacionSbs.PERDIDA, Dinero.de("5000"));
            default -> new InformeCentral(null, Dinero.CERO);
        });
    }
}
