package org.ossgang.commons.observables;

import java.util.Objects;
import java.util.function.Consumer;

final class ExceptionHandlers {
    private static volatile Consumer<Exception> uncaughtExceptionHandler = ExceptionHandlers::printExceptionToStderr;

    private ExceptionHandlers() {
        throw new UnsupportedOperationException("static only");
    }

    static void dispatchToUncaughtExceptionHandler(Exception exception) {
        try {
            uncaughtExceptionHandler.accept(exception);
        } catch (Exception e) {
            System.err.println("[Observable] An exception occurred in the global uncaught exception handler.");
            exception.printStackTrace();
        }
    }

    private static void printExceptionToStderr(Exception exception) {
        System.err.println("[Observable] An unhandled exception occurred.");
        exception.printStackTrace();
    }

    static void setUncaughtExceptionHandler(Consumer<Exception> handler) {
        Objects.requireNonNull(handler, "The exception handler must not be null");
        uncaughtExceptionHandler = handler;
    }
}
