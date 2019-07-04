package io.github.ossgang.commons.observable;

import java.util.function.Consumer;
import io.github.ossgang.commons.monads.Maybe;

public interface Observer<T> {
    void onValue(T value);

    default void onException(Throwable exception) {};

    static <T> Observer<T> withErrorHandling(Consumer<T> valueConsumer, Consumer<Throwable> exceptionConsumer) {
        return new Observer<T>() {
            public void onValue(T value) {
                valueConsumer.accept(value);
            }

            public void onException(Throwable exception) {
                exceptionConsumer.accept(exception);
            }
        };
    }

    static <T> Observer<T> forMaybes(Consumer<Maybe<T>> maybeConsumer) {
        return new Observer<T>() {

            public void onValue(T value) {
                maybeConsumer.accept(Maybe.ofValue(value));
            }

            public void onException(Throwable exception) {
                maybeConsumer.accept(Maybe.ofException(exception));
            }

        };
    }

    static <T> Observer<T> forExceptions(Consumer<Throwable> exceptionConsumer) {
        return new Observer<T>() {
                public void onValue(T value) {}
            public void onException(Throwable exception) {
                exceptionConsumer.accept(exception);
            }
        };
    }
}
