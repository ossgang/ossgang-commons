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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.time.Duration.ZERO;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;

@SuppressWarnings("unchecked")
class BaseAwaitable<T, A extends BaseAwaitable<T, A>> {
    private static final ExecutorService AWAITER_POOL =
            newCachedThreadPool(daemonThreadFactoryWithPrefix("ossgang-Awaitables-awaiter"));

    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(100);
    private static final int DEFAULT_RETRY_COUNT = Integer.MAX_VALUE;

    private final AtomicReference<Supplier<String>> message;
    private final AtomicReference<Duration> retryInterval;
    private final AtomicInteger retryCount;
    private final AtomicReference<CompletableFuture<T>> completableFuture;
    private final Supplier<Optional<T>> supplier;

    BaseAwaitable(Supplier<Optional<T>> supplier) {
        this.supplier = supplier;
        this.message = new AtomicReference<>(() -> "");
        this.completableFuture = new AtomicReference<>();
        this.retryInterval = new AtomicReference<>(DEFAULT_RETRY_INTERVAL);
        this.retryCount = new AtomicInteger(DEFAULT_RETRY_COUNT);
    }

    public A withErrorMessage(String errorMessage) {
        message.set(() -> errorMessage);
        return (A) this;
    }

    public A withErrorMessage(Supplier<String> errorMessage) {
        message.set(errorMessage);
        return (A) this;
    }

    public A withRetryCount(int numberOfRetry) {
        if (numberOfRetry < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative");
        }
        retryCount.set(numberOfRetry);
        return (A) this;
    }

    public A withRetryInterval(Duration interval) {
        if (interval.isNegative()) {
            throw new IllegalArgumentException("Retry interval cannot be negative");
        }
        retryInterval.set(interval);
        return (A) this;
    }

    private T doAwait(Duration timeout) {
        boolean timeoutEnabled = !timeout.equals(ZERO);
        int count = 1;
        Optional<T> value;
        Instant beforeWaiting = Instant.now();
        while (!(value = supplier.get()).isPresent()) {
            if (timeoutEnabled && timeoutExceeded(beforeWaiting, timeout)) {
                throw new AwaitTimeoutException("Timeout exceeded " + timeout + userMessage());
            }
            if (count > retryCount.get()) {
                throw new AwaitRetryCountException("Retry count exceeded " + count + userMessage());
            }
            if (completableFutureCancelled()) {
                throw new CancellationException("Wrapping CompletableFuture was cancelled.");
            }
            try {
                Duration interval = retryInterval.get();
                if (interval.equals(ZERO)) {
                    Thread.yield();
                } else {
                    MILLISECONDS.sleep(interval.toMillis());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            count++;
        }
        return value.get();
    }

    private String userMessage() {
        String userMessage = message.get().get();
        if (userMessage.isEmpty()) {
            return "";
        } else {
            return ": " + userMessage;
        }
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
        synchronized (completableFuture) {
            CompletableFuture<T> future = completableFuture.get();
            if (future == null) {
                future = supplyAsync(() -> doAwait(ZERO), Optional.ofNullable(executor).orElse(AWAITER_POOL));
                completableFuture.set(future);
            }
            return future;
        }
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