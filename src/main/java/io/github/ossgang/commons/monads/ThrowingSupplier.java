package io.github.ossgang.commons.monads;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}