package org.ossgang.commons.observable;

import org.junit.Test;
import org.ossgang.commons.monads.Maybe;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observable.Observers.forExceptions;

public class SimpleObservableSinkTest {

    private static final RuntimeException ANY_EXCEPTION = new RuntimeException("Exception");
    private static final String ANY_VALUE = "foo";

    @Test
    public void setUpdatesValueAndSubscriptions() throws Exception {
        CompletableFuture<String> valueFuture = new CompletableFuture<>();

        ObservableValueSink<String> sink = Observables.observableValueSink();
        sink.subscribe(valueFuture::complete);
        sink.publishValue(ANY_VALUE);

        assertThat(sink.get()).isEqualTo(ANY_VALUE);
        assertThat(valueFuture.get(1, SECONDS)).isEqualTo(ANY_VALUE);
    }

    @Test
    public void setUpdatesExceptionAndSubscriptions() throws Exception {
        CompletableFuture<Throwable> exceptionFuture = new CompletableFuture<>();

        ObservableValueSink<Object> sink = Observables.observableValueSink();
        sink.subscribe(forExceptions(exceptionFuture::complete));
        sink.publishException(ANY_EXCEPTION);

        assertThat(sink.get()).isNull();
        assertThat(exceptionFuture.get(1, SECONDS)).isEqualTo(ANY_EXCEPTION);
    }

    @Test
    public void setUpdatesMaybeAndSubscriptions() throws Exception {
        CompletableFuture<String> valueFuture = new CompletableFuture<>();
        CompletableFuture<Throwable> exceptionFuture = new CompletableFuture<>();

        ObservableValueSink<String> sink = Observables.observableValueSink();
        sink.subscribe(forExceptions(exceptionFuture::complete));
        sink.subscribe(valueFuture::complete);

        sink.publish(Maybe.ofException(ANY_EXCEPTION));
        sink.publish(Maybe.ofValue(ANY_VALUE));

        assertThat(sink.get()).isEqualTo(ANY_VALUE);
        assertThat(exceptionFuture.get(1, SECONDS)).isEqualTo(ANY_EXCEPTION);
        assertThat(valueFuture.get(1, SECONDS)).isEqualTo(ANY_VALUE);
    }

    @Test
    public void observableValueSinkWithInitialValue() {
        ObservableValueSink<String> sink = Observables.observableValueSink(ANY_VALUE);
        assertThat(sink.get()).isEqualTo(ANY_VALUE);
    }

}