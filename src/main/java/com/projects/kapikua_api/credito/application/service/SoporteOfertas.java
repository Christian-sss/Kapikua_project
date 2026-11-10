package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.out.EvaluacionCrediticia;
import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.ReglasCredito;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
class SoporteOfertas {
    private final OfertaRepositoryPort ofertas;
    private final EvaluacionCrediticia evaluacion;
    private final ReglasCredito reglas;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    SoporteOfertas(OfertaRepositoryPort ofertas, EvaluacionCrediticia evaluacion, ReglasCredito reglas,
                   EjecutorTransaccional ejecutor, Clock clock) {
        this.ofertas = ofertas;
        this.evaluacion = evaluacion;
        this.reglas = reglas;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    OffsetDateTime ahora() { return OffsetDateTime.now(clock); }

    Result<ReglasCredito.Reglas, ErrorCredito> reglas() {
        return reglas.vigentes(LocalDate.now(clock));
    }

    Result<Oferta, ErrorCredito> actualizada(Long ofertaId, UsuarioId usuario) {
        var leida = ofertas.porId(ofertaId);
        if (leida.isEmpty() || (usuario != null && !participa(leida.get(), usuario))) {
            return Result.err(new ErrorCredito.OfertaNoEncontrada());
        }
        var r = reglas();
        if (r instanceof Result.Err<ReglasCredito.Reglas, ErrorCredito> err) return Result.err(err.error());
        var plazo = ((Result.Ok<ReglasCredito.Reglas, ErrorCredito>) r).valor().plazoRespuestaAval();
        if (!leida.get().actualizarVencimientos(ahora(), clock.getZone(), plazo)) return Result.ok(leida.get());
        return Result.ok(ejecutor.ejecutar(() -> {
            var o = ofertas.porId(ofertaId).orElseThrow();
            o.actualizarVencimientos(ahora(), clock.getZone(), plazo);
            return ofertas.guardar(o);
        }));
    }

    Result<ResultadoEvaluacion, ErrorCredito> evaluacionDe(Oferta oferta) {
        return evaluacion.evaluacion(oferta.getEvaluacionId())
                .<Result<ResultadoEvaluacion, ErrorCredito>>map(Result::ok)
                .orElseGet(() -> Result.err(new ErrorCredito.OfertaNoEncontrada()));
    }

    private static boolean participa(Oferta o, UsuarioId usuario) {
        return o.getUsuarioId().equals(usuario)
                || o.getAvales().stream().anyMatch(a -> a.getUsuarioAvalId().equals(usuario));
    }
}
