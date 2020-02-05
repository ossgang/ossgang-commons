package org.ossgang.commons.observables;

import org.junit.Before;
import org.junit.Test;
import org.ossgang.commons.observables.exceptions.UnhandledException;
import org.ossgang.commons.observables.exceptions.UpdateDeliveryException;
import org.ossgang.commons.observables.testing.TestObserver;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionHandlersTest {

    private final CompletableFuture<Throwable> exception = new CompletableFuture<>();

    @Before
    public void setUncaughtExceptionHandler() {
        Observables.setUncaughtExceptionHandler(exception::complete);
    }

    @Test
    public void subscriberThrows_shouldDeflect() throws Exception {
        Property<String> property = Properties.property("A");
        property.subscribe(v -> {
            throw new RuntimeException("TEST-EXCEPTION");
        });
        property.set("b");

        Throwable ex = exception.get(1, SECONDS);
        assertThat(ex).isInstanceOf(UpdateDeliveryException.class).hasMessageContaining("TEST-EXCEPTION");
        assertThat(((UpdateDeliveryException) ex).getValue()).isEqualTo("b");
    }

    @Test
    public void mapThrows_subscriberDoesNotHandle_shouldDeflect() throws Exception {
        Property<String> property = Properties.property("A");
        property.map(Integer::valueOf).subscribe(System.out::println);
        property.set("THIS-IS-NOT-A-NUMBER");

        assertThat(exception.get(1, SECONDS))
                .isInstanceOf(UnhandledException.class)
                .hasMessageContaining("THIS-IS-NOT-A-NUMBER")
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test(expected = TimeoutException.class)
    public void nothingThrows_shouldNotCatchAnything() throws Exception {
        Property<String> property = Properties.property("A");
        property.subscribe(System.out::println);
        property.set("b");

        exception.get(200, MILLISECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void mapThrows_subscriberHandles_shouldNotDeflect() throws Exception {
        TestObserver testObserver = new TestObserver();
        Property<String> property = Properties.property("A");
        property.map(Integer::valueOf).subscribe(testObserver);
        property.set("THIS-IS-NOT-A-NUMBER");

        exception.get(200, MILLISECONDS);
        assertThat(testObserver.receivedExceptions()).hasSize(1);
        assertThat((Exception) testObserver.receivedExceptions().get(0))
                .isInstanceOf(UnhandledException.class)
                .hasMessageContaining("THIS-IS-NOT-A-NUMBER")
                .hasCauseInstanceOf(NumberFormatException.class);
    }
}
