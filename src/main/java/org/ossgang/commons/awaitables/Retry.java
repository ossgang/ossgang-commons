package org.ossgang.commons.awaitables;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A helper class to retry a certain operation until it returns a value. Internally, this will evaluate the provided
 * supplier in a waiting loop until it either returns a non-empty optional, or the timeout or maximum retry count
 * is reached. The value wrapped in the optional will then be returned.
 */
public class Retry<T> extends BaseAwaitable<T, Retry<T>> {
    private Retry(Supplier<Optional<T>> condition) {
        super(condition);
    }

    public static <T> Retry<T> retry(Supplier<Optional<T>> producer) {
        return new Retry<>(producer);
    }

    public CompletableFuture<T> asCompletableFuture() {
        return asCompletableFuture(null);
    }

    public CompletableFuture<T> asCompletableFuture(Executor executor) {
        return getAsCompletableFuture(executor);
    }

    public T indefinitely() {
        return getWaitingIndefinitely();
    }

    public T atMost(Duration aTimeout) {
        return getWaitingAtMost(aTimeout);
    }
}
