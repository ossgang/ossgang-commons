package org.ossgang.commons.observables;

import org.junit.Before;
import org.junit.Test;
import org.ossgang.commons.observables.testing.TestObserver;
import org.ossgang.commons.properties.Property;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.GcTests.forceGc;
import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.properties.Properties.property;

public class ObservableValueZipTest {

    private Property<String> valueA;
    private Property<String> valueB;

    @Before
    public void setup() {
        valueA = property();
        valueB = property();
    }

    @Test
    public void zip_noSubscription_shouldAllowGc() {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest = zip_noSubscription_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNull();
    }

    @Test
    public void zip_afterUnsubscribe_shouldAllowGc() {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest =
                zip_afterUnsubscribe_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNull();
    }

    @Test
    public void zip_withSubscription_shouldPreventGc() {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest =
                zip_withSubscription_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNotNull();
    }

    @SafeVarargs
    private static WeakReference<ObservableValue<List<String>>> zip_noSubscription_create(Property<String>... values) {
        return new WeakReference<>(Observables.zip(asList(values)));
    }

    @SafeVarargs
    private static WeakReference<ObservableValue<List<String>>> zip_afterUnsubscribe_create(Property<String>... values) {
        ObservableValue<List<String>> combined = Observables.zip(asList(values));
        combined.subscribe(i -> {
            /* no op */
        }).unsubscribe();
        return new WeakReference<>(combined);
    }

    @SafeVarargs
    private static WeakReference<ObservableValue<List<String>>> zip_withSubscription_create(Property<String>... values) {
        ObservableValue<List<String>> combined = Observables.zip(asList(values));
        combined.subscribe(i -> {
            /* no op */
        });
        return new WeakReference<>(combined);
    }

    @Test
    public void zip_withFirstUpdate() {
        TestObserver<List<String>> testObserver = new TestObserver<>();

        Observables.zip(asList(valueA, valueB)).subscribe(testObserver, FIRST_UPDATE);

        valueA.set("A1");
        valueB.set("B1");
        testObserver.awaitForValueCountToBe(1);

        valueA.set("A2");
        valueB.set("B2");
        testObserver.awaitForValueCountToBe(2);

        assertThat(testObserver.receivedValues()).containsExactly(asList("A1", "B1"), asList("A2", "B2"));
    }

    @Test
    public void zip_withCombiningFunction() {
        TestObserver<String> testObserver = new TestObserver<>();

        Observables.zip(asList(valueA, valueB), values -> String.join("", values)).subscribe(testObserver, FIRST_UPDATE);

        valueA.set("A1");
        valueB.set("B1");
        testObserver.awaitForValueCountToBe(1);

        valueA.set("A2");
        valueB.set("B2");
        testObserver.awaitForValueCountToBe(2);

        assertThat(testObserver.receivedValues()).containsExactly("A1B1", "A2B2");
    }

    @Test
    public void zip_withIndexedSources() {
        TestObserver<Map<Integer, String>> testObserver = new TestObserver<>();

        Map<Integer, ObservableValue<String>> indexedInputs = new HashMap<>();
        indexedInputs.put(1, valueA);
        indexedInputs.put(2, valueB);

        Observables.zip(indexedInputs).subscribe(testObserver, FIRST_UPDATE);

        valueA.set("A1");
        valueB.set("B1");

        testObserver.awaitForValueCountToBe(1);

        valueA.set("A2");
        valueB.set("B2");

        testObserver.awaitForValueCountToBe(2);

        Map<Integer, String> firstValue = new HashMap<>();
        firstValue.put(1, "A1");
        firstValue.put(2, "B1");
        Map<Integer, String> secondValue = new HashMap<>();
        secondValue.put(1, "A2");
        secondValue.put(2, "B2");
        assertThat(testObserver.receivedValues()).containsExactly(firstValue, secondValue);
    }

    @Test
    public void zip_withIndexedSourcesAndCombiningFunction() {
        TestObserver<String> testObserver = new TestObserver<>();

        Map<Integer, ObservableValue<String>> indexedInputs = new HashMap<>();
        indexedInputs.put(1, valueA);
        indexedInputs.put(2, valueB);

        Function<Map<Integer, String>, String> mapper = map -> String.join("", map.values());

        Observables.zip(indexedInputs, mapper).subscribe(testObserver, FIRST_UPDATE);

        valueA.set("A1");
        valueB.set("B1");

        testObserver.awaitForValueCountToBe(1);

        valueA.set("A2");
        valueB.set("B2");

        testObserver.awaitForValueCountToBe(2);

        assertThat(testObserver.receivedValues()).containsExactly("A1B1", "A2B2");
    }

}
