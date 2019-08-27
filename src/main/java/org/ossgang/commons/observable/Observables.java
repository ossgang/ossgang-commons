package org.ossgang.commons.observable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.ossgang.commons.observable.DerivedObservableValue.derive;

/**
 * Static support class for dealing with {@link Observable} and {@link ObservableValue}.
 */
public class Observables {
    private Observables() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Create an {@link ObservableValue} from any {@link Observable}. If the observable passed in is an instance of
     * {@link ObservableValue}, it is returned unchanged. Otherwise, a derived observable value is created which
     * subscribes to the upstream observable and caches its latest value.
     *
     * @param observable the observable
     * @param <T>        the type
     * @return an {@link ObservableValue} reflecting the observable provided
     */
    public static <T> ObservableValue<T> observableValueOf(Observable<T> observable) {
        if (observable instanceof ObservableValue) {
            return (ObservableValue<T>) observable;
        }
        return derive(observable, Optional::of);
    }

    /**
     * Create a constant {@link ObservableValue} holding any value. This observable is immutable, and will always
     * return the given value on get(). On subscribe(), it will immediately send the given value to the listener.
     *
     * @param value the value
     * @param <T>   the value type
     * @return a constant ObservableValue
     */
    public static <T> ObservableValue<T> constant(T value) {
        return new ConstantObservableValue<>(value);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper {@link Function}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the indexing type
     * @param <V>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper {@link Function}.
     */
    public static <K, V, O> ObservableValue<O> zip(Map<K, ObservableValue<V>> sourcesMap, Function<Map<K, V>, O> combiner) {
        Map<K, V> valueMap = new HashMap<>();
        Set<K> keys = new HashSet<>(sourcesMap.keySet());
        return derive(sourcesMap, (k, v) -> {
            synchronized (valueMap) {
                valueMap.put(k, v);
                if (valueMap.keySet().containsAll(keys)) {
                    Map<K, V> sourceMapCopy = new HashMap<>(valueMap);
                    valueMap.clear();
                    return Optional.of(combiner.apply(sourceMapCopy));
                }
                return Optional.empty();
            }
        });
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources  the input {@link ObservableValue}s
     * @param combiner the combining function that will produce the result
     * @param <V>      the input type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     */
    public static <V, O> ObservableValue<O> zip(List<ObservableValue<V>> sources, Function<List<V>, O> combiner) {
        return zip(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }


    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the resulting {@link ObservableValue} values.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param <K>        the indexing type
     * @param <V>        the input type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map} parameter provides the indexes of the
     * resulting {@link ObservableValue} values.
     */
    public static <K, V> ObservableValue<Map<K, V>> zip(Map<K, ObservableValue<V>> sourcesMap) {
        return zip(sourcesMap, Function.identity());
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources the input {@link ObservableValue}s
     * @param <V>     the input type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting {@link ObservableValue}.
     */
    public static <V> ObservableValue<List<V>> zip(List<ObservableValue<V>> sources) {
        return zip(sources, Function.identity());
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <V>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
     */
    public static <K, V, O> ObservableValue<O> combineLatest(Map<K, ObservableValue<V>> sourcesMap,
                                                             Function<Map<K, V>, O> combiner) {
        Map<K, V> valueMap = new HashMap<>();
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
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The order of the input values of the combiner is the same as the order of the provided source {@link ObservableValue}s
     *
     * @param sources  the input {@link ObservableValue}s
     * @param combiner the combining function that will produce the result
     * @param <V>      the input type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source publishes the result of the
     * combiner applied with the latest values of the other inputs.
     */
    public static <V, O> ObservableValue<O> combineLatest(List<ObservableValue<V>> sources,
                                                          Function<List<V>, O> combiner) {
        return combineLatest(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <V>        the input type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     */
    public static <K, V> ObservableValue<Map<K, V>> combineLatest(Map<K, ObservableValue<V>> sourcesMap) {
        return combineLatest(sourcesMap, Function.identity());
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a list containing the values of each {@link ObservableValue}
     *
     * @param sources the input {@link ObservableValue}s
     * @param <V>     the input type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a list containing the values of each {@link ObservableValue}
     */
    public static <V> ObservableValue<List<V>> combineLatest(List<ObservableValue<V>> sources) {
        return combineLatest(sources, Function.identity());
    }


    private static <V> Map<Integer, V> toIndexMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(toMap(Function.identity(), list::get));
    }

    private static <V> List<V> fromIndexMap(Map<Integer, V> map) {
        return IntStream.range(0, map.size()).boxed().map(map::get).collect(toList());
    }


}
