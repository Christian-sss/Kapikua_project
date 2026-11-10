package com.projects.kapikua_api.shared;

/**
 * Error de una regla de negocio. Cada contexto define los suyos
 * (por ejemplo, {@code RegistroError} en Identidad).
 */
public interface ErrorDominio {

    /** Código estable del error, por ejemplo "ID-01". */
    String codigo();

    /** Texto que ve el usuario. */
    String mensaje();
}
