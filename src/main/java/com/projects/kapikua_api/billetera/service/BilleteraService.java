package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.Celular;
import com.projects.kapikua_api.billetera.repository.BilleteraRepository;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.function.Function;

/** Apertura de la billetera y cambios de estado (bloquear, retener, reactivar). */
@Service
public class BilleteraService {
    private final BilleteraRepository billeteras;
    private final Clock clock;

    public BilleteraService(BilleteraRepository billeteras, Clock clock) {
        this.billeteras = billeteras;
        this.clock = clock;
    }

    /** Se llama al registrarse el usuario, dentro de la transacción del registro. */
    public Result<Long, ErrorBilletera> abrir(UsuarioId usuarioId, String celular, String nombreNegocio) {
        var numero = Celular.de(celular);
        if (numero.isEmpty()) {
            return Result.err(new ErrorBilletera.CelularInvalido());
        }
        if (billeteras.porUsuario(usuarioId).isPresent() || billeteras.existeCelular(numero.get())) {
            throw new IllegalStateException("El usuario o el celular ya tienen billetera");
        }
        var billetera = Billetera.abrir(usuarioId, numero.get(), nombreNegocio, OffsetDateTime.now(clock));
        return Result.ok(billeteras.guardar(billetera).getId());
    }

    /** Lo llama Cumplimiento dentro de la revisión de una alerta. Bloquear una billetera ya bloqueada no es error. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Void, ErrorBilletera> bloquearPorCumplimiento(Long billeteraId, String motivo) {
        var resultado = cambiar(billeteraId, b -> b.bloquear(motivo, OffsetDateTime.now(clock)));
        if (resultado instanceof Result.Err<Void, ErrorBilletera> err
                && err.error() instanceof ErrorBilletera.BilleteraYaBloqueada) return Result.ok();
        return resultado;
    }

    @Transactional
    public Result<Void, ErrorBilletera> retener(Long billeteraId, String motivo) {
        return cambiar(billeteraId, b -> b.retener(motivo, OffsetDateTime.now(clock)));
    }

    @Transactional
    public Result<Void, ErrorBilletera> reactivar(Long billeteraId) {
        return cambiar(billeteraId, b -> b.reactivar(OffsetDateTime.now(clock)));
    }

    private Result<Void, ErrorBilletera> cambiar(Long billeteraId,
                                                Function<Billetera, Result<Void, ErrorBilletera>> transicion) {
        var billetera = billeteras.porId(billeteraId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        return transicion.apply(billetera.get()).map(ok -> {
            billeteras.guardar(billetera.get());
            return null;
        });
    }
}
