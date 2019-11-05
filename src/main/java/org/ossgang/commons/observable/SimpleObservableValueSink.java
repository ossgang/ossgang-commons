package org.ossgang.commons.observable;

/**
 * Simple implementation of {@link ObservableValueSink}.
 *
 * @param <T> the type of the observable
 */
public class SimpleObservableValueSink<T> extends DispatchingObservableValue<T> implements ObservableValueSink<T> {

    SimpleObservableValueSink(T initial) {
        super(initial);
    }

    @Override
    public void publishException(Throwable exception) {
        dispatchException(exception);
    }

    @Override
    public void publishValue(T value) {
        dispatchValue(value);
    }
}
