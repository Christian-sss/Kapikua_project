package com.projects.kapikua_api.shared.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import com.projects.kapikua_api.shared.ErrorDominio;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Implementación con TransactionTemplate: si el bloque pierde un conflicto de bloqueo optimista (@Version),
 * lo vuelve a ejecutar desde cero hasta 3 veces, con una espera corta y aleatoria entre intentos.
 */
@Component
public class EjecutorTransaccionalSpring implements EjecutorTransaccional {

    private static final Logger log = LoggerFactory.getLogger(EjecutorTransaccionalSpring.class);
    private static final int INTENTOS = 3;

    private final TransactionTemplate transaccion;

    public EjecutorTransaccionalSpring(TransactionTemplate transaccion) {
        this.transaccion = transaccion;
    }

    /** Si agota los reintentos, lanza la OptimisticLockingFailureException: el llamador la convierte en un Err. */
    @Override
    public <T> T ejecutar(Supplier<T> bloque) {
        return conReintentos(estado -> bloque.get());
    }

    /**
     * Como {@link #ejecutar}, pero si el bloque devuelve un Err, revierte todo lo que escribió.
     * Sirve cuando una regla falla después de alguna escritura (por ejemplo, al acreditar en otro contexto).
     */
    @Override
    public <T, E extends ErrorDominio> Result<T, E> ejecutarResultado(Supplier<Result<T, E>> bloque) {
        return conReintentos(estado -> {
            var resultado = bloque.get();
            if (resultado instanceof Result.Err<T, E>) estado.setRollbackOnly();
            return resultado;
        });
    }

    private <T> T conReintentos(TransactionCallback<T> bloque) {
        for (int intento = 1; ; intento++) {
            try {
                return transaccion.execute(bloque);
            } catch (OptimisticLockingFailureException e) {
                if (intento == INTENTOS) throw e;
                log.info("Conflicto de concurrencia; reintento {} de {}", intento + 1, INTENTOS);
                esperar(intento);
            }
        }
    }

    /** Espera corta y aleatoria para que las operaciones que compiten no vuelvan a chocar a la vez. */
    private static void esperar(int intento) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10, 40) * intento);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        }
    }
}
