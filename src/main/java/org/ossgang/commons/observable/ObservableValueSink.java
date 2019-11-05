package org.ossgang.commons.observable;

import org.ossgang.commons.monads.Maybe;

/**
 * A "sink" for values of type T. It allows to publish values or exceptions to subscribers.
 *
 * @param <T> the type of the observable
 */
public interface ObservableValueSink<T> extends ObservableValue<T> {

    /**
     * Publish the provided exception to subscribers
     *
     * @param exception the exception to publish
     */
    void publishException(Throwable exception);

    /**
     * Publish the provided value to subscribers
     *
     * @param value the value to publish
     */
    void publishValue(T value);

    /**
     * Convenience method to publish a {@link Maybe}. It will publish a value or an exception depending of the content
     * of the {@link Maybe}
     *
     * @param maybe the maybe holding the value or exception to publish
     */
    default void publish(Maybe<T> maybe) {
        maybe.ifValue(this::publishValue).ifException(this::publishException);
    }

}
