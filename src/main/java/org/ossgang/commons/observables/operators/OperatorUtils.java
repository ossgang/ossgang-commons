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

    private OperatorUtils() {
        throw new UnsupportedOperationException("static only");
    }
}
