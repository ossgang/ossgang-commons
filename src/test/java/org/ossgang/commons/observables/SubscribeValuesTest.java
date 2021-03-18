/*
 * @formatter:off
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
 * @formatter:on
 */

package org.ossgang.commons.observables;

import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observables.ValueCombinationPolicies.COMBINE_LATEST;
import static org.ossgang.commons.properties.Properties.property;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.ossgang.commons.monads.Consumer3;
import org.ossgang.commons.monads.Consumer4;
import org.ossgang.commons.monads.Consumer5;
import org.ossgang.commons.observables.operators.SubscribeValuesOperators;
import org.ossgang.commons.properties.Property;

public class SubscribeValuesTest {

    @Test
    public void subscribeLatest_withDifferentClasses_shouldWorkWithBiConsumer() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);

        CompletableFuture<String> result = new CompletableFuture<>();
        BiConsumer<String, Integer> consumer = (valueA, valueB) ->
                result.complete(String.format("%s %s", valueA, valueB));

        SubscribeValuesOperators.subscribeValues(valueAProperty, valueBProperty, consumer, COMBINE_LATEST, FIRST_UPDATE);

        Assertions.assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("A 1");
    }

    @Test
    public void subscribeLatest_withDifferentClasses_shouldWorkWithConsumer3() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);
        Property<Integer> valueCProperty = property(2);

        CompletableFuture<String> result = new CompletableFuture<>();
        Consumer3<String, Integer, Integer> consumer = (valueA, valueB, valueC) ->
                result.complete(String.format("%s %s %s", valueA, valueB, valueC));

        SubscribeValuesOperators.subscribeValues(valueAProperty, valueBProperty, valueCProperty, consumer, COMBINE_LATEST, FIRST_UPDATE);

        Assertions.assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("A 1 2");
    }

    @Test
    public void subscribeLatest_withDifferentClasses_shouldWorkWithConsumer4() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);
        Property<Integer> valueCProperty = property(2);
        Property<String> valueDProperty = property("D");

        CompletableFuture<String> result = new CompletableFuture<>();
        Consumer4<String, Integer, Integer, String> consumer = (valueA, valueB, valueC, valueD) ->
                result.complete(String.format("%s %s %s %s", valueA, valueB, valueC, valueD));

        SubscribeValuesOperators.subscribeValues(valueAProperty, valueBProperty, valueCProperty, valueDProperty, consumer, COMBINE_LATEST, FIRST_UPDATE);

        Assertions.assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("A 1 2 D");
    }

    @Test
    public void subscribeLatest_withDifferentClasses_shouldWorkWithConsumer5() throws InterruptedException, ExecutionException, TimeoutException {
        Property<String> valueAProperty = property("A");
        Property<Integer> valueBProperty = property(1);
        Property<Integer> valueCProperty = property(2);
        Property<String> valueDProperty = property("D");
        Property<String> valueEProperty = property("E");

        CompletableFuture<String> result = new CompletableFuture<>();
        Consumer5<String, Integer, Integer, String, String> consumer = (valueA, valueB, valueC, valueD, valueE) ->
                result.complete(String.format("%s %s %s %s %s", valueA, valueB, valueC, valueD, valueE));

        SubscribeValuesOperators.subscribeValues(valueAProperty, valueBProperty, valueCProperty,
                valueDProperty, valueEProperty, consumer, COMBINE_LATEST, FIRST_UPDATE);

        Assertions.assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("A 1 2 D E");
    }
}
