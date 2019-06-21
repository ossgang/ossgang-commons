package io.github.ossgang.commons.monads;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}