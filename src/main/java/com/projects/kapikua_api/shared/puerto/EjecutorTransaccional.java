package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.ErrorDominio;
import com.projects.kapikua_api.shared.Result;

import java.util.function.Supplier;

/**
 * Ejecuta un bloque en su propia transacción y lo reintenta si pierde un conflicto de bloqueo optimista
 * (SAD 2.4, RNF-08). El bloque debe leer los datos dentro de sí mismo, para que cada intento vea el estado actual.
 */
public interface EjecutorTransaccional {

    /** Si agota los reintentos, propaga el conflicto de concurrencia: el llamador lo convierte en un Err. */
    <T> T ejecutar(Supplier<T> bloque);

    /** Como {@link #ejecutar}, pero si el bloque devuelve un Err, revierte todo lo que escribió. */
    <T, E extends ErrorDominio> Result<T, E> ejecutarResultado(Supplier<Result<T, E>> bloque);
}
