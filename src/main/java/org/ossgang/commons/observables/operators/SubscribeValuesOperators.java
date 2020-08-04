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

package org.ossgang.commons.observables.operators;

import org.ossgang.commons.monads.Consumer3;
import org.ossgang.commons.monads.Consumer4;
import org.ossgang.commons.monads.Consumer5;
import org.ossgang.commons.observables.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public final class SubscribeValuesOperators {

    /**
     * @see Observables#subscribeValues(Observable, Observable, BiConsumer, ValueCombinationPolicy, SubscriptionOption...)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2> Subscription subscribeValues(Observable<I1> observable1, Observable<I2> observable2,
                                                        BiConsumer<I1, I2> consumer,
                                                        ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        List<Observable<?>> inputObservables = Arrays.asList(observable1, observable2);
        return observableWithCombinedValues(combinationPolicy, inputObservables).subscribe(values -> {
            I1 value1 = (I1) values.get(0);
            I2 value2 = (I2) values.get(1);
            consumer.accept(value1, value2);
        }, subscriptionOptions);
    }

    /**
     * @see Observables#subscribeValues(Observable, Observable, Observable, Consumer3, ValueCombinationPolicy, SubscriptionOption...)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3> Subscription subscribeValues(Observable<I1> observable1, Observable<I2> observable2, Observable<I3> observable3,
                                                            Consumer3<I1, I2, I3> consumer,
                                                            ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        List<Observable<?>> inputObservables = Arrays.asList(observable1, observable2, observable3);
        return observableWithCombinedValues(combinationPolicy, inputObservables).subscribe(values -> {
            I1 value1 = (I1) values.get(0);
            I2 value2 = (I2) values.get(1);
            I3 value3 = (I3) values.get(2);
            consumer.accept(value1, value2, value3);
        }, subscriptionOptions);
    }

    /**
     * @see Observables#subscribeValues(Observable, Observable, Observable, Observable, Consumer4, ValueCombinationPolicy, SubscriptionOption...)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4> Subscription subscribeValues(Observable<I1> observable1, Observable<I2> observable2,
                                                                Observable<I3> observable3, Observable<I4> observable4,
                                                                Consumer4<I1, I2, I3, I4> consumer,
                                                                ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        List<Observable<?>> inputObservables = Arrays.asList(observable1, observable2, observable3, observable4);
        return observableWithCombinedValues(combinationPolicy, inputObservables).subscribe(values -> {
            I1 value1 = (I1) values.get(0);
            I2 value2 = (I2) values.get(1);
            I3 value3 = (I3) values.get(2);
            I4 value4 = (I4) values.get(3);
            consumer.accept(value1, value2, value3, value4);
        }, subscriptionOptions);
    }

    /**
     * @see Observables#subscribeValues(Observable, Observable, Observable, Observable, Observable, Consumer5, ValueCombinationPolicy, SubscriptionOption...)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4, I5> Subscription subscribeValues(Observable<I1> observable1, Observable<I2> observable2,
                                                                    Observable<I3> observable3, Observable<I4> observable4,
                                                                    Observable<I5> observable5,
                                                                    Consumer5<I1, I2, I3, I4, I5> consumer,
                                                                    ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        List<Observable<?>> inputObservables = Arrays.asList(observable1, observable2, observable3, observable4, observable5);
        return observableWithCombinedValues(combinationPolicy, inputObservables).subscribe(values -> {
            I1 value1 = (I1) values.get(0);
            I2 value2 = (I2) values.get(1);
            I3 value3 = (I3) values.get(2);
            I4 value4 = (I4) values.get(3);
            I5 value5 = (I5) values.get(4);
            consumer.accept(value1, value2, value3, value4, value5);
        }, subscriptionOptions);
    }

    private static Observable<List<Object>> observableWithCombinedValues(ValueCombinationPolicy combinationPolicy,
                                                                         List<Observable<?>> observables) {
        if (combinationPolicy == ValueCombinationPolicy.COMBINE_LATEST) {
            return Observables.combineLatestObjects(observables);
        }
        throw new IllegalArgumentException("Unsupported value combination policy: " + combinationPolicy);
    }

    private SubscribeValuesOperators() {
        throw new UnsupportedOperationException("static only");
    }

}
