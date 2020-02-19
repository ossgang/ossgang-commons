package org.ossgang.commons.awaitables;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A helper class to await a condition to become TRUE. Internally, this will evaluate the provided supplier in a waiting
 * loop until it either returns true, or the timeout or maximum retry count is reached.
 */
public class Await extends BaseAwaitable<Boolean, Await> {
    private Await(Supplier<Boolean> condition) {
        super(() -> Optional.of(condition.get()).filter(Boolean::booleanValue));
    }

    public static Await await(Supplier<Boolean> predicate) {
        return new Await(predicate);
    }

    public CompletableFuture<Void> asCompletableFuture() {
        return asCompletableFuture(null);
    }

    public CompletableFuture<Void> asCompletableFuture(Executor executor) {
        /* Make sure to propagate the cancellation of the future, to allow stopping the waiting worker thread!
         * Do not inline whenComplete()!
         * Check here if asCompletableFuture_futureCancelled_shouldFreeWorkerThread fails! */
        CompletableFuture<Boolean> awaitFuture = getAsCompletableFuture(executor);
        CompletableFuture<Void> voidFuture = CompletableFuture.allOf(awaitFuture);
        voidFuture.whenComplete((x, err) -> awaitFuture.cancel(true));
        return voidFuture;
    }

    public void indefinitely() {
        getWaitingIndefinitely();
    }

    public void atMost(Duration aTimeout) {
        getWaitingAtMost(aTimeout);
    }
}
