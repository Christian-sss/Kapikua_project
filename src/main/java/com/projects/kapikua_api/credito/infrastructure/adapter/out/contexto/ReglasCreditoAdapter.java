package com.projects.kapikua_api.credito.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.credito.application.port.out.ReglasCredito;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.cumplimiento.application.port.in.ConsultarParametrosUseCase;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Component
class ReglasCreditoAdapter implements ReglasCredito {
    private static final Logger log = LoggerFactory.getLogger(ReglasCreditoAdapter.class);
    private static final List<String> CODIGOS = List.of("VIGENCIA_OFERTA_DIAS", "HORAS_RESPUESTA_AVAL",
            "MAX_AVALES_SOLICITUD", "CUOTA_MINIMA", "UIT", "TOPE_OPERACION_UIT", "TASA_MORATORIA_MAX",
            "DIAS_AVISO_AVAL", "DIAS_ACTIVACION_AVAL");

    private final ConsultarParametrosUseCase parametros;

    ReglasCreditoAdapter(ConsultarParametrosUseCase parametros) {
        this.parametros = parametros;
    }

    @Override
    public Result<Reglas, ErrorCredito> vigentes(LocalDate fecha) {
        var v = parametros.vigentes(fecha);
        var faltan = CODIGOS.stream().filter(c -> !v.containsKey(c)).toList();
        if (!faltan.isEmpty()) {
            log.error("Configuración incompleta al {}: faltan los parámetros vigentes {}", fecha, faltan);
            return Result.err(new ErrorCredito.ParametrosNoDisponibles());
        }
        return Result.ok(new Reglas(
                v.get("VIGENCIA_OFERTA_DIAS").intValueExact(),
                Duration.ofHours(v.get("HORAS_RESPUESTA_AVAL").longValueExact()),
                v.get("MAX_AVALES_SOLICITUD").intValueExact(),
                Dinero.de(v.get("CUOTA_MINIMA")),
                Dinero.de(v.get("UIT").multiply(v.get("TOPE_OPERACION_UIT"))),
                v.get("TASA_MORATORIA_MAX").setScale(2, RoundingMode.HALF_EVEN),
                v.get("DIAS_AVISO_AVAL").intValueExact(),
                v.get("DIAS_ACTIVACION_AVAL").intValueExact()));
    }
}
