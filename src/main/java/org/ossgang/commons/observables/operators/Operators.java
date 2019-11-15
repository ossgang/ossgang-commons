package org.ossgang.commons.observables.operators;

import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.ossgang.commons.observables.operators.DerivedObservableValue.derive;

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

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Map, Function)
     */
    public static <K, I, O> ObservableValue<O> combineLatest(Map<K, ? extends Observable<I>> sourcesMap,
                                                             Function<Map<K, I>, O> combiner) {
        Map<K, I> valueMap = new HashMap<>();
        Set<K> keys = new HashSet<>(sourcesMap.keySet());
        return derive(sourcesMap, (k, v) -> {
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
     * @see org.ossgang.commons.observables.Observables#combineLatest(List, Function)
     */
    public static <I, O> ObservableValue<O> combineLatest(List<? extends Observable<I>> sources,
                                                          Function<List<I>, O> combiner) {
        return combineLatest(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Map)
     */
    public static <K, I> ObservableValue<Map<K, I>> combineLatest(Map<K, ? extends Observable<I>> sourcesMap) {
        return combineLatest(sourcesMap, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(List)
     */
    public static <I> ObservableValue<List<I>> combineLatest(List<? extends Observable<I>> sources) {
        return combineLatest(sources, Function.identity());
    }

    private static <V> Map<Integer, V> toIndexMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(toMap(Function.identity(), list::get));
    }

    private static <V> List<V> fromIndexMap(Map<Integer, V> map) {
        return IntStream.range(0, map.size()).boxed().map(map::get).collect(toList());
    }

}
