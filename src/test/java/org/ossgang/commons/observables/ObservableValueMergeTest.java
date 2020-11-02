package org.ossgang.commons.observables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.properties.Properties.property;

import org.junit.Test;
import org.ossgang.commons.observables.testing.TestObserver;
import org.ossgang.commons.properties.Property;

public class ObservableValueMergeTest {
    @Test
    public void merge_multipleInputs_getReturnsLast() {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");
        Property<String> valueC = property("C");

        ObservableValue<String> merge = Observables.merge(valueA, valueB, valueC);
        assertThat(merge.get()).isEqualTo("C");
    }

    @Test
    public void merge_multipleInputs_mergesStreams() {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");
        Property<String> valueC = property("C");

        ObservableValue<String> merge = Observables.merge(valueA, valueB, valueC);
        TestObserver<String> testObserver = new TestObserver<>();
        merge.subscribe(testObserver);

        valueA.set("1");
        testObserver.awaitForValueCountToBe(1);
        valueC.set("2");
        testObserver.awaitForValueCountToBe(2);
        valueA.set("3");
        testObserver.awaitForValueCountToBe(3);
        valueB.set("4");
        testObserver.awaitForValueCountToBe(4);
        valueC.set("42");
        testObserver.awaitForValueCountToBe(5);

        assertThat(testObserver.receivedValues()).containsExactly("1", "2", "3", "4", "42");
    }
}
