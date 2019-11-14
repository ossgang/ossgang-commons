package org.ossgang.commons.observable;

/**
 * Simple implementation of {@link Dispatcher}.
 *
 * @param <T> the type of the dispatcher
 */
public class SimpleDispatcher<T> extends DispatchingObservableValue<T> implements Dispatcher<T> {

    SimpleDispatcher(T initial) {
        super(initial);
    }

    @Override
    public void dispatchException(Throwable exception) {
        super.dispatchException(exception);
    }

    @Override
    public void dispatchValue(T value) {
        super.dispatchValue(value);
    }
}
