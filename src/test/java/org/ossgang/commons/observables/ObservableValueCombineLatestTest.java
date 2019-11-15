package org.ossgang.commons.observables;

import org.junit.Test;
import org.ossgang.commons.properties.Property;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.GcTests.forceGc;
import static org.ossgang.commons.observables.Observables.combineLatest;
import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.properties.Properties.property;

public class ObservableValueCombineLatestTest {

    @Test
    public void combineLatest_noSubscription_shouldAllowGc() throws Exception {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest = combineLatest_noSubscription_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNull();
    }

    @Test
    public void combineLatest_afterUnsubscribe_shouldAllowGc() throws Exception {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest =
                combineLatest_afterUnsubscribe_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNull();
    }

    @Test
    public void combineLatest_withSubscription_shouldPreventGc() throws Exception {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest =
                combineLatest_withSubscription_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNotNull();
    }

    @Test
    public void combineLatest_firstUpdate() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        CompletableFuture<String> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(asList(valueA, valueB), values -> String.join("", values))
                .subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).isEqualTo("AB");
    }

    @Test
    public void combineLatest_set() throws InterruptedException, TimeoutException, ExecutionException {
        Property<String> valueA = property();
        Property<String> valueB = property();

        CompletableFuture<String> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(asList(valueA, valueB), values -> String.join("", values)).subscribe(mergedValue::complete);

        valueA.set("A");
        valueB.set("B");

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).isEqualTo("AB");
    }

    @Test
    public void combineLatest_withoutMapping() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        CompletableFuture<List<String>> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(asList(valueA, valueB)).subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).containsExactly("A", "B");
    }

    @Test
    public void combineLatest_withMapping() throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, ObservableValue<String>> inputs = new HashMap<>();
        inputs.put("FIRST", property("A"));
        inputs.put("SECOND", property("B"));

        CompletableFuture<String> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(inputs, values -> values.get("FIRST") + values.get("SECOND"))
                .subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).isEqualTo("AB");
    }

    @Test
    public void combineLatest_withIndexedSources() throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, ObservableValue<String>> inputs = new HashMap<>();
        inputs.put("FIRST", property("A"));
        inputs.put("SECOND", property("B"));

        CompletableFuture<Map<String, String>> mergedValue = new CompletableFuture<>();
        combineLatest(inputs).subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS))
                .containsOnlyKeys("FIRST", "SECOND")
                .containsEntry("FIRST", "A")
                .containsEntry("SECOND", "B");
    }

    @SafeVarargs
    private static WeakReference<ObservableValue<List<String>>> combineLatest_noSubscription_create(Property<String>... values) {
        return new WeakReference<>(Observables.combineLatest(asList(values)));
    }

    @SafeVarargs
    private static WeakReference<ObservableValue<List<String>>> combineLatest_afterUnsubscribe_create(Property<String>... values) {
        ObservableValue<List<String>> combined = combineLatest(asList(values));
        combined.subscribe(i -> {
            /* no op */
        }).unsubscribe();
        return new WeakReference<>(combined);
    }

    @SafeVarargs
    private static WeakReference<ObservableValue<List<String>>> combineLatest_withSubscription_create(Property<String>... values) {
        ObservableValue<List<String>> combined = combineLatest(asList(values));
        combined.subscribe(i -> {
            /* no op */
        });
        return new WeakReference<>(combined);
    }
}