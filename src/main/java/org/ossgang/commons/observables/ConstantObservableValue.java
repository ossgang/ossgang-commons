package org.ossgang.commons.observables;

import static org.ossgang.commons.observables.ExceptionHandlers.dispatchToUncaughtExceptionHandler;

import org.ossgang.commons.observables.exceptions.UpdateDeliveryException;

/**
 * A fixed-value, immutable implementation of {@link ObservableValue}. Such an observable always returns the same
 * constant value on get(), and immediately dispatches one single update on subscribe().
 *
 * @param <T> the value
 */
public final class ConstantObservableValue<T> implements ObservableValue<T> {
    private final T value;

    ConstantObservableValue(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public Subscription subscribe(Observer<? super T> listener, SubscriptionOption... options) {
        try {
            listener.onValue(value);
        } catch (Exception e) {
            dispatchToUncaughtExceptionHandler(new UpdateDeliveryException(value, e));
        }
        return () -> {};
    }
}
