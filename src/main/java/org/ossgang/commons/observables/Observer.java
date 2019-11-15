package org.ossgang.commons.observables;

import org.ossgang.commons.observables.exceptions.UnhandledException;

/**
 * An observer which can consume an {@link Observable}. Instances of this class can be created from lambdas, method
 * references, or using the {@link Observers} utility class.
 * @param <T> the item type
 */
public interface Observer<T> {
    void onValue(T value);

    default void onException(Throwable exception) { throw new UnhandledException(exception); };

    default void onSubscribe(Subscription subscription) {};

    default void onUnsubscribe(Subscription subscription) {};
}
