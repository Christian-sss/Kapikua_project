package com.projects.kapikua_api.evaluacion.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.cumplimiento.application.port.in.ConsultarParametrosUseCase;
import com.projects.kapikua_api.evaluacion.application.port.out.ParametrosPolitica;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.CondicionesNivel;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore.PonderacionesScore;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore.PuntajesClasificacionSbs;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore.ReferenciasScore;
import com.projects.kapikua_api.evaluacion.domain.model.LimitesCredito;
import com.projects.kapikua_api.evaluacion.domain.model.ParametrosCredito;
import com.projects.kapikua_api.evaluacion.domain.model.ReglasNivel;
import com.projects.kapikua_api.evaluacion.domain.model.ReglasRetencion;
import com.projects.kapikua_api.evaluacion.domain.model.ReglasScore;
import com.projects.kapikua_api.evaluacion.domain.model.ReglasScore.ReglasRecomendacion;
import com.projects.kapikua_api.evaluacion.domain.model.UsoMinimo;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
class ParametrosPoliticaAdapter implements ParametrosPolitica {
    private static final Logger log = LoggerFactory.getLogger(ParametrosPoliticaAdapter.class);

    private final ConsultarParametrosUseCase parametros;

    ParametrosPoliticaAdapter(ConsultarParametrosUseCase parametros) {
        this.parametros = parametros;
    }

    @Override
    public Result<ParametrosCredito, ErrorEvaluacion> vigentes(LocalDate fecha) {
        var v = new Lector(parametros.vigentes(fecha));
        try {
            var niveles = new HashMap<Integer, CondicionesNivel>();
            for (int n = 1; v.tiene("NIVEL_" + n + "_PORC_CAPACIDAD"); n++) {
                var prefijo = "NIVEL_" + n + "_";
                niveles.put(n, new CondicionesNivel(n, v.dec(prefijo + "PORC_CAPACIDAD"),
                        v.entero(prefijo + "MAX_CUOTAS"), v.dec(prefijo + "TEA"),
                        v.enteroOCero(prefijo + "CREDITOS_PARA_SUBIR")));
            }
            if (niveles.isEmpty()) throw new IllegalStateException("NIVEL_1_PORC_CAPACIDAD");

            var uso = new UsoMinimo(v.entero("USO_MIN_DIAS"), v.entero("USO_MIN_SEMANAS_COBRO"),
                    v.entero("USO_MIN_COBROS"), v.entero("PERIODO_COBROS_SEMANAS"));
            var limites = new LimitesCredito(v.dec("PORC_CAPACIDAD_PAGO"), Dinero.de(v.dec("DEUDA_MAX_MICROEMPRESA")),
                    Dinero.de(v.dec("UIT").multiply(v.dec("TOPE_OPERACION_UIT"))), v.dec("TASA_MAXIMA_TEA"),
                    Dinero.de(v.dec("CUOTA_MINIMA")), v.entero("CUOTAS_MIN"));
            var recomendacion = new ReglasRecomendacion(v.entero("SCORE_APTO"), v.entero("SCORE_AJUSTE"),
                    v.dec("PORC_AJUSTE"));
            var score = new ReglasScore(recomendacion, factores(v));
            var retencion = new ReglasRetencion(v.entero("RETENCION_MIN"), v.entero("RETENCION_MAX"),
                    v.entero("RETENCION_DEFECTO"));
            var reglasNivel = new ReglasNivel(niveles, v.entero("NIVEL_MAX_CON_AVAL"),
                    v.entero("DIAS_ATRASO_MAX_PARA_SUBIR"), v.entero("DIAS_ATRASO_BAJA_NIVEL"),
                    v.entero("DIAS_ATRASO_BLOQUEO"));
            return Result.ok(new ParametrosCredito(uso, limites, score, retencion, reglasNivel));
        } catch (IllegalStateException faltante) {
            log.error("Configuración incompleta: falta el parámetro vigente {} al {}", faltante.getMessage(), fecha);
            return Result.err(new ErrorEvaluacion.ParametrosNoDisponibles());
        } catch (IllegalArgumentException invalido) {
            log.error("Configuración de evaluación inválida al {}: {}", fecha, invalido.getMessage());
            return Result.err(new ErrorEvaluacion.ParametrosInvalidos());
        }
    }

    private static ConfiguracionFactoresScore factores(Lector v) {
        var ponderaciones = new PonderacionesScore(
                v.dec("SCORE_PESO_REGULARIDAD"), v.dec("SCORE_PESO_HOLGURA"), v.dec("SCORE_PESO_HISTORIAL"),
                v.dec("SCORE_PESO_PAGADORES"), v.dec("SCORE_PESO_ANTIGUEDAD"), v.dec("SCORE_PESO_CLASIFICACION"));
        var referencias = new ReferenciasScore(v.entero("SCORE_REF_PAGADORES"),
                v.entero("SCORE_REF_ANTIGUEDAD_DIAS"), v.dec("SCORE_HISTORIAL_NUEVO"), v.dec("SCORE_HISTORIAL_MENOS_POR_DIA"));
        var puntajesSbs = new PuntajesClasificacionSbs(
                v.dec("SCORE_SBS_NORMAL"), v.dec("SCORE_SBS_SIN_HISTORIAL"), v.dec("SCORE_SBS_CPP"));
        return new ConfiguracionFactoresScore(ponderaciones, referencias, puntajesSbs);
    }

    private record Lector(Map<String, BigDecimal> valores) {
        boolean tiene(String codigo) {
            return valores.containsKey(codigo);
        }

        BigDecimal dec(String codigo) {
            var valor = valores.get(codigo);
            if (valor == null) throw new IllegalStateException(codigo);
            return valor;
        }

        int entero(String codigo) {
            try {
                return dec(codigo).intValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("El parámetro " + codigo + " debe ser un entero válido", e);
            }
        }

        int enteroOCero(String codigo) {
            return tiene(codigo) ? entero(codigo) : 0;
        }
    }
}
