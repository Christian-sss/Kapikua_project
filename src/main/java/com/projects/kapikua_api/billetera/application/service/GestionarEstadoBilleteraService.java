package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.GestionarEstadoBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.event.EstadoCambiado;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.function.Function;

@Service
@Transactional
public class GestionarEstadoBilleteraService implements GestionarEstadoBilleteraUseCase {

    private final BilleteraRepositoryPort billeteras;
    private final PublicadorEventos eventos;
    private final Clock clock;

    public GestionarEstadoBilleteraService(BilleteraRepositoryPort billeteras, PublicadorEventos eventos, Clock clock) {
        this.billeteras = billeteras;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Void, ErrorBilletera> bloquearPorCumplimiento(Long billeteraId, String motivo) {
        var resultado = cambiar(billeteraId, b -> b.bloquear(motivo, OffsetDateTime.now(clock)));
        if (resultado instanceof Result.Err<Void, ErrorBilletera> err
                && err.error() instanceof ErrorBilletera.BilleteraYaBloqueada) return Result.ok();
        return resultado;
    }

    @Override
    public Result<Void, ErrorBilletera> retener(Long billeteraId, String motivo) {
        return cambiar(billeteraId, b -> b.retener(motivo, OffsetDateTime.now(clock)));
    }

    @Override
    public Result<Void, ErrorBilletera> reactivar(Long billeteraId) {
        return cambiar(billeteraId, b -> b.reactivar(OffsetDateTime.now(clock)));
    }

    private Result<Void, ErrorBilletera> cambiar(Long billeteraId,
                                                Function<Billetera, Result<EstadoCambiado, ErrorBilletera>> transicion) {
        var billetera = billeteras.porId(billeteraId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        return transicion.apply(billetera.get()).flatMap(evento -> {
            billeteras.guardar(billetera.get());
            eventos.publicar(evento);
            return Result.ok();
        });
    }
}
