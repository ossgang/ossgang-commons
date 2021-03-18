package org.ossgang.commons.observables.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.ossgang.commons.observables.Dispatcher;
import org.ossgang.commons.observables.Observables;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

public class TestObserverTest {

    private Property<Object> source;
    private TestObserver<Object> observer;

    @Before
    public void setUp() {
        source = Properties.property();
        observer = new TestObserver<>();
        source.subscribe(observer);
    }

    @Test
    public void testAwaitForValues() {
        source.set(1);
        observer.awaitForPublishedValuesToContain(1);
        source.set(2);
        observer.awaitForPublishedValuesToContain(2);
        source.set(3);
        source.set(4);
        observer.awaitForPublishedValuesToContain(3);
        observer.awaitForPublishedValuesToContain(4);

        assertThat(observer.receivedValues()).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    public void testAwaitForValueCount() {
        source.set(1);
        observer.awaitForValueCountToBe(1);
        source.set(2);
        observer.awaitForValueCountToBe(2);
        source.set(3);
        source.set(4);
        observer.awaitForValueCountToBe(4);

        assertThat(observer.receivedValues()).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    public void testAwaitForException() {
        TestObserver<Object> observer = new TestObserver<>();

        Dispatcher<Object> dispatcher = Observables.dispatcher();
        dispatcher.subscribe(observer);

        dispatcher.dispatchException(new RuntimeException("TEST EXCEPTION!"));
        observer.awaitForExceptionCountToBe(1);

        assertThat(observer.receivedExceptions().get(0)).hasMessage("TEST EXCEPTION!");
    }

}
