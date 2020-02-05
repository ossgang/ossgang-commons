package org.ossgang.commons.observables;

import org.ossgang.commons.observables.exceptions.UnhandledException;

import static org.ossgang.commons.observables.ExceptionHandlers.dispatchToUncaughtExceptionHandler;

/**
 * An immutable implementation of {@link ObservableValue} which always returns null on get(),
 * and immediately dispatches a constant exception as a single update on subscribe().
 *
 * @param <T> any type for compatibility with ObservableValue
 */
public final class ConstantExceptionObservableValue<T> implements ObservableValue<T> {
    private final Throwable throwable;

    ConstantExceptionObservableValue(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public T get() {
        return null;
    }

    @Override
    public Subscription subscribe(Observer<? super T> listener, SubscriptionOption... options) {
        try {
            listener.onException(throwable);
        } catch (UnhandledException e) {
            dispatchToUncaughtExceptionHandler(e);
        }
        return () -> {};
    }
}
