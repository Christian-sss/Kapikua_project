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

@Component
public class EjecutorTransaccionalSpring implements EjecutorTransaccional {
    private static final Logger log = LoggerFactory.getLogger(EjecutorTransaccionalSpring.class);
    private static final int INTENTOS = 3;

    private final TransactionTemplate transaccion;

    public EjecutorTransaccionalSpring(TransactionTemplate transaccion) {
        this.transaccion = transaccion;
    }

    @Override
    public <T> T ejecutar(Supplier<T> bloque) {
        return conReintentos(estado -> bloque.get());
    }

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

    private static void esperar(int intento) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10, 40) * intento);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        }
    }
}
