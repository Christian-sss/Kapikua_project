package com.projects.kapikua_api.evaluacion.application.service;

import com.projects.kapikua_api.evaluacion.application.port.in.ActualizarHistorialUseCase;
import com.projects.kapikua_api.evaluacion.application.port.out.HistorialRepositoryPort;
import com.projects.kapikua_api.evaluacion.application.port.out.ParametrosPolitica;
import com.projects.kapikua_api.evaluacion.domain.model.HistorialCrediticio;
import com.projects.kapikua_api.evaluacion.domain.model.ParametrosCredito;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.BiConsumer;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ActualizarHistorialService implements ActualizarHistorialUseCase {
    private final HistorialRepositoryPort historiales;
    private final ParametrosPolitica parametros;
    private final Clock clock;

    public ActualizarHistorialService(HistorialRepositoryPort historiales, ParametrosPolitica parametros, Clock clock) {
        this.historiales = historiales;
        this.parametros = parametros;
        this.clock = clock;
    }

    @Override
    public void registrarDesembolso(UsuarioId usuarioId) {
        aplicar(usuarioId, (h, p) -> h.registrarDesembolso());
    }

    @Override
    public void registrarAtraso(UsuarioId usuarioId, int dias) {
        aplicar(usuarioId, (h, p) -> h.registrarAtraso(dias, p));
    }

    @Override
    public void registrarCreditoPagado(UsuarioId usuarioId, boolean pagadoPorAval) {
        aplicar(usuarioId, (h, p) -> h.registrarCreditoPagado(pagadoPorAval, p));
    }

    private void aplicar(UsuarioId usuarioId, BiConsumer<HistorialCrediticio, ParametrosCredito> cambio) {
        var resultado = parametros.vigentes(LocalDate.now(clock));
        if (!(resultado instanceof Result.Ok<ParametrosCredito, ?> ok)) {
            throw new IllegalStateException("No hay política de crédito vigente para actualizar el historial");
        }
        var historial = historiales.porUsuario(usuarioId).orElseGet(() -> HistorialCrediticio.nuevo(usuarioId));
        cambio.accept(historial, ok.valor());
        historiales.guardar(historial);
    }
}
