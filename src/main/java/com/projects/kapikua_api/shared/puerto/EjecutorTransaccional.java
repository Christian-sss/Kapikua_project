package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.ErrorDominio;
import com.projects.kapikua_api.shared.Result;

import java.util.function.Supplier;

public interface EjecutorTransaccional {
    <T> T ejecutar(Supplier<T> bloque);

    <T, E extends ErrorDominio> Result<T, E> ejecutarResultado(Supplier<Result<T, E>> bloque);
}
