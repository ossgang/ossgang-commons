package org.ossgang.commons.monads;

@FunctionalInterface
public interface ThrowingPredicate<T> {
    boolean test(T t);
}
