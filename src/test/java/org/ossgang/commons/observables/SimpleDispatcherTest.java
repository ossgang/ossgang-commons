package org.ossgang.commons.observables;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observables.Observers.forExceptions;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.ossgang.commons.monads.Maybe;

public class SimpleDispatcherTest {

    private static final RuntimeException ANY_EXCEPTION = new RuntimeException("Exception");
    private static final String ANY_VALUE = "foo";

    @Test
    public void setUpdatesValueAndSubscriptions() throws Exception {
        CompletableFuture<String> valueFuture = new CompletableFuture<>();

        Dispatcher<String> sink = Observables.dispatcher();
        sink.subscribe(valueFuture::complete);
        sink.dispatchValue(ANY_VALUE);

        assertThat(sink.get()).isEqualTo(ANY_VALUE);
        assertThat(valueFuture.get(1, SECONDS)).isEqualTo(ANY_VALUE);
    }

    @Test
    public void setUpdatesExceptionAndSubscriptions() throws Exception {
        CompletableFuture<Throwable> exceptionFuture = new CompletableFuture<>();

        Dispatcher<Object> sink = Observables.dispatcher();
        sink.subscribe(forExceptions(exceptionFuture::complete));
        sink.dispatchException(ANY_EXCEPTION);

        assertThat(sink.get()).isNull();
        assertThat(exceptionFuture.get(1, SECONDS)).isEqualTo(ANY_EXCEPTION);
    }

    @Test
    public void setUpdatesMaybeAndSubscriptions() throws Exception {
        CompletableFuture<String> valueFuture = new CompletableFuture<>();
        CompletableFuture<Throwable> exceptionFuture = new CompletableFuture<>();

        Dispatcher<String> sink = Observables.dispatcher();
        sink.subscribe(forExceptions(exceptionFuture::complete));
        sink.subscribe(valueFuture::complete);

        sink.dispatch(Maybe.ofException(ANY_EXCEPTION));
        sink.dispatch(Maybe.ofValue(ANY_VALUE));

        assertThat(sink.get()).isEqualTo(ANY_VALUE);
        assertThat(exceptionFuture.get(1, SECONDS)).isEqualTo(ANY_EXCEPTION);
        assertThat(valueFuture.get(1, SECONDS)).isEqualTo(ANY_VALUE);
    }

    @Test
    public void observableValueSinkWithInitialValue() {
        Dispatcher<String> sink = Observables.dispatcher(ANY_VALUE);
        assertThat(sink.get()).isEqualTo(ANY_VALUE);
    }

}