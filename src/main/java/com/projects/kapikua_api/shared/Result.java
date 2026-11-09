package com.projects.kapikua_api.shared;

import java.util.function.Function;

/**
 * Resultado de una operación que puede fallar por una regla de negocio.
 * Las reglas de negocio no lanzan excepciones: devuelven {@code Err}.
 */
public sealed interface Result<T, E extends ErrorDominio> {

    record Ok<T, E extends ErrorDominio>(T valor) implements Result<T, E> {}

    record Err<T, E extends ErrorDominio>(E error) implements Result<T, E> {
        public Err {
            if (error == null) throw new IllegalArgumentException("El error no puede ser nulo");
        }
    }

    static <T, E extends ErrorDominio> Result<T, E> ok(T valor) { return new Ok<>(valor); }

    static <E extends ErrorDominio> Result<Void, E> ok() { return new Ok<>(null); }

    static <T, E extends ErrorDominio> Result<T, E> err(E error) { return new Err<>(error); }

    default boolean esOk() { return this instanceof Ok; }

    default <U> Result<U, E> map(Function<? super T, ? extends U> f) {
        return switch (this) {
            case Ok<T, E> ok   -> new Ok<>(f.apply(ok.valor()));
            case Err<T, E> err -> new Err<>(err.error());
        };
    }

    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> f) {
        return switch (this) {
            case Ok<T, E> ok   -> f.apply(ok.valor());
            case Err<T, E> err -> new Err<>(err.error());
        };
    }
}
