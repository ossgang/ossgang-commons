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

package org.ossgang.commons.observables.operators;

import static org.ossgang.commons.observables.operators.DerivedObservableValue.derive;
import static org.ossgang.commons.observables.operators.OperatorUtils.fromIndexMap;
import static org.ossgang.commons.observables.operators.OperatorUtils.toIndexMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.ossgang.commons.monads.Function3;
import org.ossgang.commons.monads.Function4;
import org.ossgang.commons.monads.Function5;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;

public final class CombinationOperators {

    /**
     * @see org.ossgang.commons.observables.Observables#merge(Collection)
     */
    public static <O, I extends O> ObservableValue<O> merge(Collection<? extends Observable<I>> sources) {
        return derive(toIndexMap(new ArrayList<>(sources)), (k, v) -> Optional.of(v));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(Map, Function)
     */
    public static <K, O> ObservableValue<O> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap,
            Function<Map<K, Object>, O> combiner) {
        @SuppressWarnings("unchecked") /* safe, ordering for latest cast is manually ensured */ Map<K, Observable<Object>> sourcesMapObject = (Map<K, Observable<Object>>) sourcesMap;
        Map<K, Object> valueMap = new HashMap<>();
        Set<K> keys = new HashSet<>(sourcesMapObject.keySet());
        return derive(sourcesMapObject, (k, v) -> {
            synchronized (valueMap) {
                valueMap.put(k, v);
                if (valueMap.keySet().containsAll(keys)) {
                    return Optional.of(combiner.apply(new HashMap<>(valueMap)));
                }
                return Optional.empty();
            }
        });
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(List, Function)
     */
    public static <O> ObservableValue<O> combineLatestObjects(List<? extends Observable<?>> sources,
            Function<List<Object>, O> combiner) {
        return combineLatestObjects(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(Map)
     */
    public static <K> ObservableValue<Map<K, Object>> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap) {
        return combineLatestObjects(sourcesMap, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(List)
     */
    public static ObservableValue<List<Object>> combineLatestObjects(List<? extends Observable<?>> sources) {
        return combineLatestObjects(sources, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Map, Function)
     */
    public static <K, I, O> ObservableValue<O> combineLatest(Map<K, ? extends Observable<I>> sourcesMap,
            Function<Map<K, I>, O> combiner) {
        Function<Map<K, Object>, Map<K, I>> typedTranslator = OperatorUtils::typeTranslator;
        return combineLatestObjects(sourcesMap, typedTranslator.andThen(combiner));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(List, Function)
     */
    public static <I, O> ObservableValue<O> combineLatest(List<? extends Observable<I>> sources,
            Function<List<I>, O> combiner) {
        Function<List<Object>, List<I>> typedTranslator = OperatorUtils::typeTranslator;
        return combineLatestObjects(toIndexMap(sources),
                idxMap -> typedTranslator.andThen(combiner).apply(fromIndexMap(idxMap)));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Map)
     */
    public static <K, I> ObservableValue<Map<K, I>> combineLatest(Map<K, ? extends Observable<I>> sourcesMap) {
        return combineLatestObjects(sourcesMap, OperatorUtils::typeTranslator);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(List)
     */
    public static <I> ObservableValue<List<I>> combineLatest(List<? extends Observable<I>> sources) {
        return combineLatestObjects(sources, OperatorUtils::typeTranslator);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, BiFunction)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
            BiFunction<I1, I2, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            return combiner.apply(sourceValue1, sourceValue2);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, Observable, Function3)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
            Observable<I3> source3, Function3<I1, I2, I3, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, Observable, Observable, Function4)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
            Observable<I3> source3, Observable<I4> source4, Function4<I1, I2, I3, I4, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3, source4);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            I4 sourceValue4 = (I4) values.get(3);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3, sourceValue4);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, Observable, Observable, Observable, Function5)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4, I5, O> ObservableValue<O> combineLatest(Observable<I1> source1,
            Observable<I2> source2, Observable<I3> source3, Observable<I4> source4, Observable<I5> source5,
            Function5<I1, I2, I3, I4, I5, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3, source4, source5);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            I4 sourceValue4 = (I4) values.get(3);
            I5 sourceValue5 = (I5) values.get(4);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3, sourceValue4, sourceValue5);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zipObjects(Map, Function)
     */
    public static <K, O> ObservableValue<O> zipObjects(Map<K, ? extends Observable<?>> sourcesMap,
            Function<Map<K, Object>, O> combiner) {
        @SuppressWarnings("unchecked") /* safe, ordering for latest cast is manually ensured */ Map<K, Observable<Object>> sourcesMapObject = (Map<K, Observable<Object>>) sourcesMap;
        Map<K, Object> valueMap = new HashMap<>();
        Set<K> keys = new HashSet<>(sourcesMapObject.keySet());
        return derive(sourcesMapObject, (k, v) -> {
            synchronized (valueMap) {
                valueMap.put(k, v);
                if (valueMap.keySet().containsAll(keys)) {
                    Map<K, Object> valueMapCopy = new HashMap<>(valueMap);
                    valueMap.clear();
                    return Optional.of(combiner.apply(valueMapCopy));
                }
                return Optional.empty();
            }
        });
    }

    /*
     * @see org.ossgang.commons.observables.Observables#zipObjects(List, Function)
     */
    public static <O> ObservableValue<O> zipObjects(List<? extends Observable<?>> sources,
            Function<List<Object>, O> combiner) {
        return zipObjects(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zipObjects(Map)
     */
    public static <K> ObservableValue<Map<K, Object>> zipObjects(Map<K, ? extends Observable<?>> sourcesMap) {
        return zipObjects(sourcesMap, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zipObjects(List)
     */
    public static ObservableValue<List<Object>> zipObjects(List<? extends Observable<?>> sources) {
        return zipObjects(sources, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(Map, Function)
     */
    public static <K, I, O> ObservableValue<O> zip(Map<K, ? extends Observable<I>> sourcesMap,
            Function<Map<K, I>, O> combiner) {
        Function<Map<K, Object>, Map<K, I>> typedTranslator = OperatorUtils::typeTranslator;
        return zipObjects(sourcesMap, typedTranslator.andThen(combiner));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(List, Function)
     */
    public static <I, O> ObservableValue<O> zip(List<? extends Observable<I>> sources, Function<List<I>, O> combiner) {
        return zip(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(Map)
     */
    public static <K, I> ObservableValue<Map<K, I>> zip(Map<K, ? extends Observable<I>> sourcesMap) {
        return zip(sourcesMap, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(List)
     */
    public static <I> ObservableValue<List<I>> zip(List<? extends Observable<I>> sources) {
        return zip(sources, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(Observable, Observable, BiFunction)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
            BiFunction<I1, I2, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            return combiner.apply(sourceValue1, sourceValue2);
        };
        return zipObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(Observable, Observable, Observable, Function3)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
            Observable<I3> source3, Function3<I1, I2, I3, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3);
        };
        return zipObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(Observable, Observable, Observable, Observable, Function4)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
            Observable<I3> source3, Observable<I4> source4, Function4<I1, I2, I3, I4, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3, source4);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            I4 sourceValue4 = (I4) values.get(3);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3, sourceValue4);
        };
        return zipObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(Observable, Observable, Observable, Observable, Observable, Function5)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4, I5, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
            Observable<I3> source3, Observable<I4> source4, Observable<I5> source5,
            Function5<I1, I2, I3, I4, I5, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3, source4, source5);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            I4 sourceValue4 = (I4) values.get(3);
            I5 sourceValue5 = (I5) values.get(4);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3, sourceValue4, sourceValue5);
        };
        return zipObjects(sourcesListObject, combinerObject);
    }

    private CombinationOperators() {
        throw new UnsupportedOperationException("static only");
    }
}
