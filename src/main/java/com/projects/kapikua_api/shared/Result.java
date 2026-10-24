package com.projects.kapikua_api.shared;

import java.util.function.Function;





public sealed interface Result<T, E> {

    record Ok<T, E>(T valor) implements Result<T, E> {}
    record Err<T, E>(E error) implements Result<T, E> {}

    static <T, E> Result<T, E> ok(T valor)  { return new Ok<>(valor); }
    static <T, E> Result<T, E> err(E error) { return new Err<>(error); }

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