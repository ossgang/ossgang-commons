package org.ossgang.commons.observable;

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
        listener.onValue(value);
        return () -> {};
    }
}
