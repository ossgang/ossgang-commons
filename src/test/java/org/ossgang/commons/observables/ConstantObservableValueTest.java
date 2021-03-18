package org.ossgang.commons.observables;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class ConstantObservableValueTest {

    @Test
    public void valueIsProvided() throws InterruptedException, ExecutionException, TimeoutException {
        Object anyObject = new Object();
        ConstantObservableValue<Object> constantObservable = new ConstantObservableValue<>(anyObject);
        assertThat(constantObservable.get()).isSameAs(anyObject);
        CompletableFuture<Object> valueFromSubscription = new CompletableFuture<>();
        constantObservable.subscribe(valueFromSubscription::complete);
        assertThat(valueFromSubscription.get(1, TimeUnit.SECONDS)).isSameAs(anyObject);
    }

    @Test
    public void exceptionInConsumerDoesNotLeak() {
        Object anyObject = new Object();
        ConstantObservableValue<Object> constantObservable = new ConstantObservableValue<>(anyObject);

        constantObservable.subscribe(e -> {
            throw new RuntimeException("fail");
        });
    }

}