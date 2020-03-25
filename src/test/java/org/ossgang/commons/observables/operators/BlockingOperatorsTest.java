package org.ossgang.commons.observables.operators;

import org.junit.Test;
import org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException;
import org.ossgang.commons.monads.Maybe;
import org.ossgang.commons.observables.Dispatcher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.ossgang.commons.monads.Maybe.attempt;
import static org.ossgang.commons.observables.Observables.dispatcher;

public class BlockingOperatorsTest {
    public static final Duration TIMEOUT = Duration.ofSeconds(10);
    public static final Duration REACHABLE_TIMEOUT = Duration.ofMillis(100);
    private Dispatcher<String> observable = dispatcher("initial");

    @Test
    public void awaitNextValue_shouldIgnoreExceptionsAndReturnNextValue() {
        CompletableFuture<String> nextValue = supplyAsync(() -> observable.awaitNextValue(TIMEOUT));
        waitForCompletableFuture();
        observable.dispatchException(new Exception("some exception"));
        observable.dispatchException(new Exception("another exception"));
        assertThat(nextValue.isDone()).isFalse();
        String dispatchedUpdate = "UPDATE";
        observable.dispatchValue(dispatchedUpdate);
        assertThat(nextValue.join()).isEqualTo(dispatchedUpdate);
    }

    @Test
    public void awaitNext_nextUpdateIsException_shouldReturnException() {
        CompletableFuture<Maybe<String>> nextValue = supplyAsync(() -> observable.awaitNext(TIMEOUT));
        waitForCompletableFuture();
        Exception dispatchedException = new Exception("some exception");
        observable.dispatchException(dispatchedException);
        assertThat(nextValue.join().exception()).isEqualTo(dispatchedException);
    }

    @Test
    public void awaitNext_nextUpdateIsValue_shouldReturnValue() {
        CompletableFuture<Maybe<String>> nextValue = supplyAsync(() -> observable.awaitNext(TIMEOUT));
        waitForCompletableFuture();
        String dispatchedUpdate = "UPDATE";
        observable.dispatchValue(dispatchedUpdate);
        assertThat(nextValue.join().value()).isEqualTo(dispatchedUpdate);
    }

    @Test
    public void awaitNextValue_noValueUpdateWithinTimeout_shouldThrow() {
        CompletableFuture<String> nextValue = supplyAsync(() -> observable.awaitNextValue(REACHABLE_TIMEOUT));
        waitForCompletableFuture();
        observable.dispatchException(new Exception("some exception"));
        observable.dispatchException(new Exception("another exception"));
        assertThat(nextValue.isDone()).isFalse();
        assertThatExceptionOfType(CompletionException.class).isThrownBy(nextValue::join)
                .withCauseExactlyInstanceOf(AwaitTimeoutException.class)
                .withMessageContaining("Timeout exceeded");
    }

    @Test
    public void awaitNext_noUpdateWithinTimeout_shouldThrow() {
        CompletableFuture<Maybe<String>> nextValue = supplyAsync(() -> observable.awaitNext(REACHABLE_TIMEOUT));
        waitForCompletableFuture();
        assertThatExceptionOfType(CompletionException.class).isThrownBy(nextValue::join)
                .withCauseExactlyInstanceOf(AwaitTimeoutException.class)
                .withMessageContaining("Timeout exceeded");
    }

    private void waitForCompletableFuture() {
        attempt(() -> MILLISECONDS.sleep(50)).throwOnException();
    }
}