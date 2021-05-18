package org.ossgang.commons.observables.operators;

import static org.ossgang.commons.awaitables.Retry.retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.ossgang.commons.monads.Maybe;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.Observers;
import org.ossgang.commons.observables.Subscription;

public final class BlockingOperators {
    private BlockingOperators() {
        throw new UnsupportedOperationException("static only");
    }

    public static <T> T awaitNextValue(Observable<T> observable, Duration timeout) {
        return awaitNextItem(observable, timeout, consumer -> consumer::accept);
    }

    public static <T> Maybe<T> awaitNext(Observable<T> observable, Duration timeout) {
        return awaitNextItem(observable, timeout, Observers::forMaybes);
    }

    private static <T, O> T awaitNextItem(Observable<O> observable, Duration timeout,
                                          Function<Consumer<T>, Observer<O>> observerFactory) {
        AtomicReference<T> update = new AtomicReference<>();
        Subscription subscription = observable.subscribe(observerFactory.apply(update::set));
        try {
            return retry(update::get).untilNotNull().atMost(timeout);
        } finally {
            subscription.unsubscribe();
        }
    }
}
