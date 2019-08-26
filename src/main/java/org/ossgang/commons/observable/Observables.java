package org.ossgang.commons.observable;

import org.ossgang.commons.property.Property;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.ossgang.commons.observable.ObservableValue.ObservableValueSubscriptionOption.FIRST_UPDATE;
import static org.ossgang.commons.property.Properties.property;

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
        return new DerivedObservableValue<>(observable, Optional::of);
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
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper {@link Function}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param <K>        the indexing type
     * @param <V>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper {@link Function}.
     */
    public static <K, V, O> ObservableValue<O> zip(Map<K, ObservableValue<V>> sourcesMap, Function<Map<K, V>, O> mapper) {
        Property<O> zipProperty = property();

        Map<K, ObservableValue<V>> sourcesMapCopy = new HashMap<>(sourcesMap);
        Set<K> keys = sourcesMapCopy.keySet();
        Map<K, V> valuesToZip = new HashMap<>();

        for (K key : keys) {
            sourcesMapCopy.get(key).subscribe(value -> {
                synchronized (valuesToZip) {
                    valuesToZip.put(key, value);
                    if (valuesToZip.keySet().containsAll(keys)) {
                        zipProperty.set(mapper.apply(new HashMap<>(valuesToZip)));
                        valuesToZip.clear();
                    }
                }
            }, FIRST_UPDATE);
        }

        return zipProperty;
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources the input {@link ObservableValue}s
     * @param mapper  the mapping function
     * @param <V>     the input type
     * @param <O>     the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     */
    public static <V, O> ObservableValue<O> zip(List<ObservableValue<V>> sources, Function<List<V>, O> mapper) {
        List<ObservableValue<V>> sourcesCopy = new ArrayList<>(sources);
        Map<Integer, ObservableValue<V>> indexToSources = IntStream.range(0, sourcesCopy.size()).boxed() //
                .collect(toMap(Function.identity(), sourcesCopy::get));

        return zip(indexToSources, (Map<Integer, V> sourceValuesIndexed) -> {
            List<V> sourcesValues = IntStream.range(0, sourceValuesIndexed.size()).boxed() //
                    .map(sourceValuesIndexed::get) //
                    .collect(toList());
            return mapper.apply(sourcesValues);
        });
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
        Property<O> mergedProperty = property();
        Map<ObservableValue<V>, V> latestValues = new HashMap<>();
        List<ObservableValue<V>> sourcesCopy = new ArrayList<>(sources);

        for (ObservableValue<V> source : sourcesCopy) {
            source.subscribe(sourceValue -> {
                synchronized (latestValues) {
                    latestValues.put(source, sourceValue);

                    if (latestValues.keySet().containsAll(sourcesCopy)) {
                        List<V> latestValueSnapshotInOrder = new ArrayList<>();
                        for (ObservableValue<V> s : sourcesCopy) {
                            latestValueSnapshotInOrder.add(latestValues.get(s));
                        }

                        mergedProperty.set(combiner.apply(latestValueSnapshotInOrder));
                    }
                }
            }, FIRST_UPDATE);
        }

        return mergedProperty;
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param mapper     the combining function that will produce the result
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <V>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
     */
    public static <K, V, O> ObservableValue<O> combineLatest(Map<K, ObservableValue<V>> sourcesMap, Function<Map<K, V>, O> mapper) {
        HashMap<K, ObservableValue<V>> sourcesMapCopy = new HashMap<>(sourcesMap);

        Map<Integer, K> keyIndexes = new HashMap<>();
        List<ObservableValue<V>> sources = new ArrayList<>();

        int keyIndex = 0;
        for (Map.Entry<K, ObservableValue<V>> entry : sourcesMapCopy.entrySet()) {
            sources.add(entry.getValue());

            keyIndexes.put(keyIndex, entry.getKey());
            keyIndex++;
        }

        return combineLatest(sources, (List<V> sourceValues) -> {
            Map<K, V> keyToSourceValues = new HashMap<>();
            for (int i = 0; i < sourceValues.size(); i++) {
                keyToSourceValues.put(keyIndexes.get(i), sourceValues.get(i));
            }
            return mapper.apply(keyToSourceValues);
        });
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

}
