package org.ossgang.commons.awaitables;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
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

    /**
     * Start construction of a retry request based on an Supplier of Optional. The supplier will be called until it
     * returns a non empty optional, which is then returned.
     *
     * @param producer the supplier to call
     * @param <T> the type of object to return
     * @return a DSL provider to create the retry request
     */
    public static <T> Retry<T> retryUntil(Supplier<Optional<T>> producer) {
        return new Retry<>(producer);
    }

    /**
     * Start construction of a retry request based on an Supplier of T and a Predicate in T. The supplier will be
     * called until the value it returns passes the provided predicate. The value is then returned.
     *
     * @param producer the supplier to call
     * @param <T> the type of object to return
     * @return a DSL provider to create the retry request
     */
    public static <T> OngoingRetryProducerCreation<T> retry(Supplier<T> producer) {
        return new OngoingRetryProducerCreation<>(producer);
    }

    /**
     * Run this await asynchronously and return it as a {@link CompletableFuture}. If the returned completable future
     * is cancelled, the internal wait loop will stop. Multiple invocations of this method will return a shared
     * {@link CompletableFuture} instance.
     *
     * @return the completable future
     */
    public CompletableFuture<T> asCompletableFuture() {
        return asCompletableFuture(null);
    }

    /**
     * Run this await asynchronously and return it as a {@link CompletableFuture}. If the returned completable future
     * is cancelled, the internal wait loop will stop. Multiple invocations of this method will return a shared
     * {@link CompletableFuture} instance.
     *
     * @param executor the executor to run on
     * @return the completable future
     */
    public CompletableFuture<T> asCompletableFuture(Executor executor) {
        return getAsCompletableFuture(executor);
    }

    /**
     * Block indefinitely until the supplier provides an acceptable result, or the maximum number of wait iterations
     * is exceeded.
     *
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitRetryCountException if the iteration count is exceeded
     * @return the produced result
     */
    public T indefinitely() {
        return getWaitingIndefinitely();
    }

    /**
     * Block until the supplier provides an acceptable result, or the provided timeout expires.
     *
     * @param aTimeout the timeout
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitRetryCountException if the iteration count is exceeded
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException if the timeout is exceeded
     * @return the produced value
     */
    public T atMost(Duration aTimeout) {
        return getWaitingAtMost(aTimeout);
    }

    public static class OngoingRetryProducerCreation<T> {
        private final Supplier<T> supplier;
        private OngoingRetryProducerCreation(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public Retry<T> until(Predicate<T> predicate) {
            return retryUntil(() -> Optional.ofNullable(supplier.get()).filter(predicate));
        }

        public Retry<T> untilNotNull() {
            return retryUntil(() -> Optional.ofNullable(supplier.get()));
        }
    }
}
