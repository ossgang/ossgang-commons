/*
 *
 * This file is part of ossgang-commons.
 *
 * Copyright (c) 2008-2020, CERN. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ossgang.commons.observables;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.ossgang.commons.monads.Function3;
import org.ossgang.commons.monads.Function4;
import org.ossgang.commons.monads.Function5;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observables;
import org.ossgang.commons.properties.Property;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.GcTests.forceGc;
import static org.ossgang.commons.observables.Observables.combineLatest;
import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.properties.Properties.property;

public class ObservableValueCombineLatestTest {

    @Test
    public void combineLatest_withDifferentClasses_shouldWorkWithMap() {
        Property<String> valueAProperty = property("A");
        Property<AtomicReference<Integer>> valueBProperty = property(new AtomicReference<>(1));

        Map<String, Observable<?>> inputs = new HashMap<>();
        inputs.put("string", valueAProperty);
        inputs.put("integer", valueBProperty);

        ObservableValue<String> combineLatest = Observables.combineLatestObjects(inputs, (valuesMap) -> {
            String valueA = (String) valuesMap.get("string");
            @SuppressWarnings("unchecked")
            AtomicReference<Integer> valueB = (AtomicReference<Integer>) valuesMap.get("integer");
            return "Got " + valueA + " and " + valueB.get();
        });
        Assertions.assertThat(combineLatest.get()).isEqualTo("Got A and 1");
    }

    @Test
    public void combineLatest_withDifferentClasses_shouldWorkWithList() {
        Property<String> valueAProperty = property("A");
        Property<AtomicReference<Integer>> valueBProperty = property(new AtomicReference<>(1));

        List<Observable<?>> inputs = Arrays.asList(valueAProperty, valueBProperty);

        ObservableValue<String> combineLatest = Observables.combineLatestObjects(inputs, (values) -> {
            String valueA = (String) values.get(0);
            @SuppressWarnings("unchecked")
            AtomicReference<Integer> valueB = (AtomicReference<Integer>) values.get(1);
            return "Got " + valueA + " and " + valueB.get();
        });
        Assertions.assertThat(combineLatest.get()).isEqualTo("Got A and 1");
    }

    @Test
    public void combineLatest_withDifferentClasses_shouldWorkWithBiFunction() {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);

        BiFunction<String, Integer, String> combiner = (valueA, valueB) ->
                String.format("%s %s", valueA, valueB);

        ObservableValue<String> combineLatest = Observables.combineLatest(valueAProperty, valueBProperty, combiner);

        Assertions.assertThat(combineLatest.get()).isEqualTo("A 1");
    }

    @Test
    public void combineLatest_withDifferentClasses_shouldWorkWithFunction3() {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);
        Property<Integer> valueCProperty = property(2);

        Function3<String, Integer, Integer, String> combiner = (valueA, valueB, valueC) ->
                String.format("%s %s %s", valueA, valueB, valueC);

        ObservableValue<String> combineLatest = Observables.combineLatest(valueAProperty, valueBProperty, valueCProperty,
                combiner);

        Assertions.assertThat(combineLatest.get()).isEqualTo("A 1 2");
    }

    @Test
    public void combineLatest_withDifferentClasses_shouldWorkWithFunction4() {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);
        Property<Integer> valueCProperty = property(2);
        Property<String> valueDProperty = property("D");

        Function4<String, Integer, Integer, String, String> combiner = (valueA, valueB, valueC, valueD) ->
                String.format("%s %s %s %s", valueA, valueB, valueC, valueD);

        ObservableValue<String> combineLatest = Observables.combineLatest(valueAProperty, valueBProperty, valueCProperty,
                valueDProperty, combiner);

        Assertions.assertThat(combineLatest.get()).isEqualTo("A 1 2 D");
    }

    @Test
    public void combineLatest_withDifferentClasses_shouldWorkWithFunction5() {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);
        Property<Integer> valueCProperty = property(2);
        Property<String> valueDProperty = property("D");
        Property<String> valueEProperty = property("E");

        Function5<String, Integer, Integer, String, String, String> combiner = (valueA, valueB, valueC, valueD, valueE) ->
                String.format("%s %s %s %s %s", valueA, valueB, valueC, valueD, valueE);

        ObservableValue<String> combineLatest = Observables.combineLatest(valueAProperty, valueBProperty, valueCProperty,
                valueDProperty, valueEProperty, combiner);

        Assertions.assertThat(combineLatest.get()).isEqualTo("A 1 2 D E");
    }

    @Test
    public void combineLatest_noSubscription_shouldAllowGc() {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest = combineLatest_noSubscription_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNull();
    }

    @Test
    public void combineLatest_afterUnsubscribe_shouldAllowGc() {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        WeakReference<ObservableValue<List<String>>> combineLatest =
                combineLatest_afterUnsubscribe_create(valueA, valueB);
        forceGc();
        assertThat(combineLatest.get()).isNull();
    }

    @Test
    public void combineLatest_withSubscription_shouldPreventGc() {
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
    public void combineLatestObjects_shouldWorkWithList() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueA = property("A");
        Property<String> valueB = property("B");

        CompletableFuture<List<Object>> mergedValue = new CompletableFuture<>();
        Observables.combineLatestObjects(asList(valueA, valueB)).subscribe(mergedValue::complete, FIRST_UPDATE);

        assertThat(mergedValue.get(5, TimeUnit.SECONDS)).containsExactly("A", "B");
    }

    @Test
    public void combineLatestObjects_shouldWorkWithMap() throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, ObservableValue<String>> inputs = new HashMap<>();
        inputs.put("FIRST", property("A"));
        inputs.put("SECOND", property("B"));

        CompletableFuture<Map<String, Object>> valuesFuture = new CompletableFuture<>();
        Observables.combineLatestObjects(inputs).subscribe(valuesFuture::complete, FIRST_UPDATE);

        Map<String, Object> values = valuesFuture.get(5, TimeUnit.SECONDS);
        assertThat(values.get("FIRST")).isEqualTo("A");
        assertThat(values.get("SECOND")).isEqualTo("B");
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