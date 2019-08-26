package org.ossgang.commons.observable;

import org.junit.Test;
import org.ossgang.commons.property.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observable.ObservableValue.ObservableValueSubscriptionOption.FIRST_UPDATE;
import static org.ossgang.commons.observable.Observables.combineLatest;
import static org.ossgang.commons.property.Properties.property;

public class ObservableValueCombineLatestTest {

    @Test
    public void testCombiningWithFirstUpdateValues() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        CompletableFuture<String> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(asList(valueA, valueB), values -> String.join("", values))
                .subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).isEqualTo("AB");
    }

    @Test
    public void testCombiningLatestValues() throws InterruptedException, TimeoutException, ExecutionException {
        Property<String> valueA = property();
        Property<String> valueB = property();

        CompletableFuture<String> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(asList(valueA, valueB), values -> String.join("", values)).subscribe(mergedValue::complete);

        valueA.set("A");
        valueB.set("B");

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).isEqualTo("AB");
    }

    @Test
    public void testCombiningWithoutMapper() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        CompletableFuture<List<String>> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(asList(valueA, valueB)).subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).containsExactly("A", "B");
    }

    @Test
    public void testCombiningWithMap() throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, ObservableValue<String>> inputs = new HashMap<>();
        inputs.put("FIRST", property("A"));
        inputs.put("SECOND", property("B"));

        CompletableFuture<String> mergedValue = new CompletableFuture<>();
        Observables.combineLatest(inputs, values -> values.get("FIRST") + values.get("SECOND"))
                .subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).isEqualTo("AB");
    }

    @Test
    public void testCombiningWithMapWithoutMapper() throws InterruptedException, ExecutionException, TimeoutException {
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

}