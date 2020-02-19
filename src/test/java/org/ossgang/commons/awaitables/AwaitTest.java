package org.ossgang.commons.awaitables;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.ossgang.commons.awaitables.exceptions.AwaitRetryCountException;
import org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.ossgang.commons.awaitables.Await.await;

public class AwaitTest {
    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    @Test
    public void awaitFulfilledCondition_shouldReturnImmediately() {
        await(() -> true).indefinitely();
    }

    @Test
    public void awaitUnfulfilledConditionWithTimeout_shouldTimeout() {
        assertThatExceptionOfType(AwaitTimeoutException.class).isThrownBy(
                () -> await(() -> false).atMost(Duration.ofMillis(20)));
    }

    @Test
    public void awaitUnfulfilledConditionWithLimitedRetries_shouldHitLimit() {
        assertThatExceptionOfType(AwaitRetryCountException.class).isThrownBy(
                () -> await(() -> false).withRetryCount(5).withRetryInterval(Duration.ZERO).indefinitely());
    }

    @Test
    public void awaitUnfulfilledConditionWithTimeoutAndErrorMessage_shouldTimeout() {
        assertThatExceptionOfType(AwaitTimeoutException.class).isThrownBy(
                () -> await(() -> false).withErrorMessage("ERROR_MESSAGE").atMost(Duration.ofMillis(20)))
                .withMessageContaining("ERROR_MESSAGE");
    }

    @Test
    public void awaitUnfulfilledConditionWithLimitedRetriesAndErrorMessage_shouldHitLimit() {
        assertThatExceptionOfType(AwaitRetryCountException.class).isThrownBy(
                () -> await(() -> false).withErrorMessage("ERROR_MESSAGE").withRetryCount(5).indefinitely())
                .withMessageContaining("ERROR_MESSAGE");
    }

    @Test
    public void awaitConditionFulfilledLater_shouldReturnAfterConditionFulfilled() throws InterruptedException {
        AtomicBoolean condition = new AtomicBoolean(false);
        CompletableFuture<Void> awaitFuture = await(condition::get).asCompletableFuture();
        MILLISECONDS.sleep(100);
        assertThat(awaitFuture.isDone()).isFalse();
        condition.set(true);
        awaitFuture.join();
    }

    @Test
    public void apiMisuse_asCompletableFutureCalledTwice_shouldThrow() {
        Await await = await(() -> false);
        await.asCompletableFuture();
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(await::asCompletableFuture);
    }

    @Test
    public void asCompletableFuture_futureCancelled_shouldFreeWorkerThread() throws InterruptedException {
        ForkJoinPool threadPool = new ForkJoinPool();
        assertThat(threadPool.isQuiescent()).isTrue();
        CompletableFuture<Void> completableFuture = await(() -> false).asCompletableFuture(threadPool);
        MILLISECONDS.sleep(100);
        assertThat(threadPool.isQuiescent()).isFalse();
        completableFuture.cancel(true);
        threadPool.awaitQuiescence(1000, MILLISECONDS);
        assertThat(threadPool.isQuiescent()).isTrue();
    }
}