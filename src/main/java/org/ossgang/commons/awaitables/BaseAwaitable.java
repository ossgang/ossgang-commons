package org.ossgang.commons.awaitables;

import org.ossgang.commons.awaitables.exceptions.AwaitRetryCountException;
import org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.time.Duration.ZERO;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.ossgang.commons.awaitables.AwaitDefaults.DEFAULT_RETRY_COUNT;
import static org.ossgang.commons.awaitables.AwaitDefaults.DEFAULT_RETRY_INTERVAL;
import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;

@SuppressWarnings("unchecked")
class BaseAwaitable<T, A extends BaseAwaitable<T, A>> {
    private static final ExecutorService AWAITER_POOL =
            newCachedThreadPool(daemonThreadFactoryWithPrefix("ossgang-Awaitables-awaiter"));

    private Supplier<String> message;
    private Duration retryInterval;
    private int retryCount;
    private Supplier<Optional<T>> supplier;
    private final AtomicReference<CompletableFuture<?>> completableFuture = new AtomicReference<>();

    BaseAwaitable(Supplier<Optional<T>> supplier) {
        this.supplier = supplier;
        this.message = () -> "";
        this.retryInterval = DEFAULT_RETRY_INTERVAL;
        this.retryCount = DEFAULT_RETRY_COUNT;
    }

    public A withErrorMessage(String errorMessage) {
        this.message = () -> errorMessage;
        return (A) this;
    }

    public A withErrorMessage(Supplier<String> errorMessage) {
        this.message = errorMessage;
        return (A) this;
    }

    public A withRetryCount(int numberOfRetry) {
        if (numberOfRetry < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative");
        }
        this.retryCount = numberOfRetry;
        return (A) this;
    }

    public A withRetryInterval(Duration interval) {
        if (interval.isNegative()) {
            throw new IllegalArgumentException("Retry interval cannot be negative");
        }
        this.retryInterval = interval;
        return (A) this;
    }

    private T doAwait(Duration timeout) {
        boolean timeoutEnabled = !timeout.equals(ZERO);
        int count = 1;
        Optional<T> value;
        Instant beforeWaiting = Instant.now();
        while (!(value = supplier.get()).isPresent()) {
            if (timeoutEnabled && timeoutExceeded(beforeWaiting, timeout)) {
                throw new AwaitTimeoutException("Timeout exceeded " + timeout + ": " + message.get());
            }
            if (count > retryCount) {
                throw new AwaitRetryCountException("Retry count exceeded " + count + ": " + message.get());
            }
            if (completableFutureCancelled()) {
                throw new CancellationException("Wrapping CompletableFuture was cancelled.");
            }
            try {
                if (retryInterval.equals(ZERO)) {
                    Thread.yield();
                } else {
                    MILLISECONDS.sleep(retryInterval.toMillis());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            count++;
        }
        return value.get();
    }

    private boolean completableFutureCancelled() {
        CompletableFuture<?> completableFuture = this.completableFuture.get();
        if (completableFuture == null) {
            return false;
        }
        return completableFuture.isCancelled();
    }

    private static boolean timeoutExceeded(Instant beforeWaiting, Duration timeout) {
        return Duration.between(beforeWaiting, Instant.now()).compareTo(timeout) > 0;
    }

    protected CompletableFuture<T> getAsCompletableFuture(Executor executor) {
        CompletableFuture<T> waitingFuture = supplyAsync(() -> doAwait(ZERO),
                Optional.ofNullable(executor).orElse(AWAITER_POOL));
        if (!completableFuture.compareAndSet(null, waitingFuture)) {
            throw new IllegalStateException("getAsCompletableFuture called twice. Do not share Awaitable objects.");
        }
        return waitingFuture;
    }

    protected T getWaitingIndefinitely() {
        return doAwait(ZERO);
    }

    protected T getWaitingAtMost(Duration aTimeout) {
        if (aTimeout.equals(ZERO)) {
            throw new IllegalArgumentException("If you want a ZERO timeout, use indefinitely()");
        }
        if (aTimeout.isNegative()) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }

        return doAwait(aTimeout);
    }
}
