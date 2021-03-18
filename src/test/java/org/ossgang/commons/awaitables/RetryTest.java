package org.ossgang.commons.awaitables;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.ossgang.commons.awaitables.Retry.retry;
import static org.ossgang.commons.awaitables.Retry.retryUntil;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.ossgang.commons.awaitables.exceptions.AwaitRetryCountException;
import org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException;

public class RetryTest {
    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    @Test
    public void retrySuccessfulWithDsl_shouldReturnImmediately() {
        assertThat(retry(() -> "test").until("test"::equals).indefinitely()).isEqualTo("test");
    }

    @Test
    public void retrySuccessful_shouldReturnImmediately() {
        assertThat(retryUntil(() -> Optional.of(42)).indefinitely()).isEqualTo(42);
    }

    @Test
    public void retryFailingWithTimeout_shouldTimeout() {
        assertThatExceptionOfType(AwaitTimeoutException.class).isThrownBy(
                () -> retryUntil(Optional::empty).atMost(Duration.ofMillis(20)));
    }

    @Test
    public void retryFailingWithLimitedRetries_shouldHitLimit() {
        assertThatExceptionOfType(AwaitRetryCountException.class).isThrownBy(
                () -> retryUntil(Optional::empty).withRetryCount(5).withRetryInterval(Duration.ZERO).indefinitely());
    }

    @Test
    public void retryFailingWithTimeoutAndErrorMessage_shouldTimeout() {
        assertThatExceptionOfType(AwaitTimeoutException.class).isThrownBy(
                () -> retryUntil(Optional::empty).withErrorMessage("ERROR_MESSAGE").atMost(Duration.ofMillis(20)))
                .withMessageContaining("ERROR_MESSAGE");
    }

    @Test
    public void retryFailingWithLimitedRetriesAndErrorMessage_shouldHitLimit() {
        assertThatExceptionOfType(AwaitRetryCountException.class).isThrownBy(
                () -> retryUntil(Optional::empty).withErrorMessage("ERROR_MESSAGE").withRetryCount(5).indefinitely())
                .withMessageContaining("ERROR_MESSAGE");
    }

    @Test
    public void retrySuccessfulAfterRetries_shouldReturnAfterConditionFulfilled() throws InterruptedException {
        AtomicReference<Optional<Integer>> result = new AtomicReference<>(Optional.empty());
        CompletableFuture<Integer> retryFuture = retryUntil(result::get).asCompletableFuture();
        MILLISECONDS.sleep(100);
        assertThat(retryFuture.isDone()).isFalse();
        result.set(Optional.of(42));
        assertThat(retryFuture.join()).isEqualTo(42);
    }

    @Test
    public void asCompletableFutureCalledTwice_shouldReturnSameFuture() {
        Retry<Boolean> retry = retryUntil(Optional::empty);
        CompletableFuture<Boolean> future1 = retry.asCompletableFuture();
        CompletableFuture<Boolean> future2 = retry.asCompletableFuture();
        assertThat(future1).isSameAs(future2);
        future1.cancel(true);
    }

    @Test
    public void asCompletableFuture_futureCancelled_shouldFreeWorkerThread() throws InterruptedException {
        ForkJoinPool threadPool = new ForkJoinPool();
        assertThat(threadPool.isQuiescent()).isTrue();
        CompletableFuture<Object> completableFuture = retryUntil(Optional::empty).asCompletableFuture(threadPool);
        MILLISECONDS.sleep(100);
        assertThat(threadPool.isQuiescent()).isFalse();
        completableFuture.cancel(true);
        threadPool.awaitQuiescence(1000, MILLISECONDS);
        assertThat(threadPool.isQuiescent()).isTrue();
    }
}