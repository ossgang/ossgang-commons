package io.github.ossgang.commons.monads;

@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T input) throws Exception;
}