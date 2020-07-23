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

import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;

import java.util.*;
import java.util.function.Function;

import static org.ossgang.commons.observables.operators.DerivedObservableValue.derive;
import static org.ossgang.commons.observables.operators.OperatorUtils.fromIndexMap;
import static org.ossgang.commons.observables.operators.OperatorUtils.toIndexMap;

public final class Operators {

    private Operators() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * @see org.ossgang.commons.observables.Observables#observableValueOf(Observable)
     */
    public static <T> ObservableValue<T> observableValueOf(Observable<T> observable) {
        if (observable instanceof ObservableValue) {
            return (ObservableValue<T>) observable;
        }
        return derive(observable, Optional::of);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#zip(Map, Function)
     */
    public static <K, I, O> ObservableValue<O> zip(Map<K, ? extends Observable<I>> sourcesMap, Function<Map<K, I>, O> combiner) {
        Map<K, I> valueMap = new HashMap<>();
        Set<K> keys = new HashSet<>(sourcesMap.keySet());
        return derive(sourcesMap, (k, v) -> {
            synchronized (valueMap) {
                valueMap.put(k, v);
                if (valueMap.keySet().containsAll(keys)) {
                    Map<K, I> valueMapCopy = new HashMap<>(valueMap);
                    valueMap.clear();
                    return Optional.of(combiner.apply(valueMapCopy));
                }
                return Optional.empty();
            }
        });
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

}
