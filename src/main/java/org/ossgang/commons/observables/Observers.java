package org.ossgang.commons.observables;

import java.util.function.Consumer;

import org.ossgang.commons.monads.Maybe;

/**
 * Utility class to create {@link Observer} instances.
 */
public class Observers {
    private Observers() {
        throw new UnsupportedOperationException("static only");
    }

    public static <T> Observer<T> withErrorHandling(Consumer<T> valueConsumer, Consumer<Throwable> exceptionConsumer) {
        return new Observer<T>() {
            public void onValue(T value) {
                valueConsumer.accept(value);
            }

            public void onException(Throwable exception) {
                exceptionConsumer.accept(exception);
            }
        };
    }

    public static <T> Observer<T> forMaybes(Consumer<Maybe<T>> maybeConsumer) {
        return new Observer<T>() {

            public void onValue(T value) {
                maybeConsumer.accept(Maybe.ofValue(value));
            }

            public void onException(Throwable exception) {
                maybeConsumer.accept(Maybe.ofException(exception));
            }

        };
    }

    public static <T> Observer<T> forExceptions(Consumer<Throwable> exceptionConsumer) {
        return new Observer<T>() {
            public void onValue(T value) {
            }

            public void onException(Throwable exception) {
                exceptionConsumer.accept(exception);
            }
        };
    }

}