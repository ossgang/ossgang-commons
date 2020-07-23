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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class OperatorUtils {

    public static <V> Map<Integer, V> toIndexMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(toMap(Function.identity(), list::get));
    }

    public static <V> List<V> fromIndexMap(Map<Integer, V> map) {
        return IntStream.range(0, map.size()).boxed().map(map::get).collect(toList());
    }

    public static <K, I, O> Map<K, O> applyToMapValues(Map<K, I> inputMap, Function<I, O> mapper) {
        return inputMap.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> mapper.apply(e.getValue())));
    }

    public static <K, I> Map<K, I> typeTranslator(Map<K, Object> source) {
        return (Map<K, I>) source;
    }

    public static <T> List<T> typeTranslator(List<Object> source) {
        return (List<T>) source;
    }

    private OperatorUtils() {
        throw new UnsupportedOperationException("static only");
    }
}
