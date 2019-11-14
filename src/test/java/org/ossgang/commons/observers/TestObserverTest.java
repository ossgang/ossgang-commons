package org.ossgang.commons.observers;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ossgang.commons.property.Properties;
import org.ossgang.commons.property.Property;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Ignore("Wait for merge of ObservableValueSink branch!")
    @Test
    public void testAwaitForException() {
        Assertions.fail("Wait for merge of ObservableValueSink branch!");
    }

}
