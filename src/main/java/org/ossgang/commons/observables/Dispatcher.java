package org.ossgang.commons.observables;

import org.ossgang.commons.monads.Maybe;

/**
 * Dispatched the values or exceptions to subscribers.
 *
 * @param <T> the type of the dispatcher
 */
public interface Dispatcher<T> extends ObservableValue<T> {

    /**
     * Dispatch the provided exception to subscribers
     *
     * @param exception the exception to dispatch
     */
    void dispatchException(Throwable exception);

    /**
     * Dispatch the provided value to subscribers
     *
     * @param value the value to dispatch
     */
    void dispatchValue(T value);

    /**
     * Convenience method to dispatch a {@link Maybe}. It will diaptch a value or an exception depending of the content
     * of the {@link Maybe}
     *
     * @param maybe the maybe holding the value or exception to dispatch
     */
    default void dispatch(Maybe<T> maybe) {
        maybe.ifValue(this::dispatchValue).ifException(this::dispatchException);
    }

}
