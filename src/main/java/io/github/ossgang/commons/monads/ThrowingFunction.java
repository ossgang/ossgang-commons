package io.github.ossgang.commons.monads;

@FunctionalInterface
public interface ThrowingFunction<I, R> {
    R apply(I input) throws Exception;
}