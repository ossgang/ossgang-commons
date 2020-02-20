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

    /**
     * Start construction of an await request which evaluates the provided condition until it becomes true.
     *
     * @param condition the condition to evaluate
     * @return a DSL provider to create the await request
     */
    public static Await await(Supplier<Boolean> condition) {
        return new Await(condition);
    }

    /**
     * Run this await asynchronously and return it as a void {@link CompletableFuture} for chaining. If the returned
     * completable future is cancelled, the internal wait loop will stop.
     *
     * @return the completable future
     */
    public CompletableFuture<Void> asCompletableFuture() {
        return asCompletableFuture(null);
    }

    /**
     * Run this await asynchronously and return it as a void {@link CompletableFuture} for chaining. If the returned
     * completable future is cancelled, the internal wait loop will stop.
     *
     * @param executor the executor to run on
     * @return the completable future
     */
    public CompletableFuture<Void> asCompletableFuture(Executor executor) {
        /* Make sure to propagate the cancellation of the future, to allow stopping the waiting worker thread!
         * Do not inline whenComplete()!
         * Check here if asCompletableFuture_futureCancelled_shouldFreeWorkerThread fails! */
        CompletableFuture<Boolean> awaitFuture = getAsCompletableFuture(executor);
        CompletableFuture<Void> voidFuture = CompletableFuture.allOf(awaitFuture);
        voidFuture.whenComplete((x, err) -> awaitFuture.cancel(true));
        return voidFuture;
    }

    /**
     * Block indefinitely until the condition becomes true, or the maximum number of wait iterations is exceeded.
     *
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitRetryCountException if the iteration count is exceeded
     */
    public void indefinitely() {
        getWaitingIndefinitely();
    }

    /**
     * Block until the condition becomes true or the provided timeout expires.
     *
     * @param aTimeout the timeout
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitRetryCountException if the iteration count is exceeded
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException    if the timeout is exceeded
     */
    public void atMost(Duration aTimeout) {
        getWaitingAtMost(aTimeout);
    }
}
