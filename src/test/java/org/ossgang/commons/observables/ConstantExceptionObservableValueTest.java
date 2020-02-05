package org.ossgang.commons.observables;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observables.Observers.withErrorHandling;

public class ConstantExceptionObservableValueTest {
    @Test
    public void getReturnsNull() {
        ConstantExceptionObservableValue<Object> exceptionObservableValue =
                new ConstantExceptionObservableValue<>(new RuntimeException("fail"));
        assertThat(exceptionObservableValue.get()).isNull();
    }

    @Test
    public void subscribeForwardsExceptionToSubscriber() throws InterruptedException, ExecutionException, TimeoutException {
        RuntimeException anyException = new RuntimeException("fail");
        ConstantExceptionObservableValue<Object> exceptionObservableValue =
                new ConstantExceptionObservableValue<>(anyException);
        CompletableFuture<Object> valueUpdate = new CompletableFuture<>();
        CompletableFuture<Throwable> exceptionUpdate = new CompletableFuture<>();
        exceptionObservableValue.subscribe(withErrorHandling(valueUpdate::complete, exceptionUpdate::complete));
        assertThat(exceptionUpdate.get(1, SECONDS)).isSameAs(anyException);
        assertThat(valueUpdate.isDone()).isFalse();
    }

    @Test
    public void unhandledExceptionFromConsumerDoesNotLeak() {
        RuntimeException anyException = new RuntimeException("fail");
        ConstantExceptionObservableValue<Object> exceptionObservableValue =
                new ConstantExceptionObservableValue<>(anyException);

        exceptionObservableValue.subscribe(e -> {});
    }

}