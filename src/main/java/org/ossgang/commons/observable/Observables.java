package org.ossgang.commons.observable;

import org.ossgang.commons.observable.connectors.ConnectorObservableValue;
import org.ossgang.commons.observable.connectors.ConnectorObservables;
import org.ossgang.commons.observable.connectors.ConnectorState;
import org.ossgang.commons.observable.connectors.DynamicConnectorObservableValue;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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

    public static <T> ConnectorObservableValue<T> connectWhen(ObservableValue<T> upstream, ObservableValue<ConnectorState> connectProvider) {
        return ConnectorObservables.connectWhen(upstream, connectProvider);
    }

    public static <T> ConnectorObservableValue<T> connectWhen(Supplier<ObservableValue<T>> upstreamSupplier, ObservableValue<ConnectorState> connectProvider) {
        return ConnectorObservables.connectWhen(upstreamSupplier, connectProvider);
    }

    public static <T> ConnectorObservableValue<T> connectorObservableValue(Supplier<ObservableValue<T>> upstreamSupplier) {
        return ConnectorObservables.connectorObservableValue(upstreamSupplier);
    }

    public static <T> ConnectorObservableValue<T> connectorTo(ObservableValue<T> upstream) {
        return ConnectorObservables.connectorTo(upstream);
    }

    public static <T> DynamicConnectorObservableValue<T> dynamicConnectorObservableValue() {
        return ConnectorObservables.dynamicConnectorObservableValue();
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
     * @param <I>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper {@link Function}.
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
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources  the input {@link ObservableValue}s
     * @param combiner the combining function that will produce the result
     * @param <I>      the input type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     */
    public static <I, O> ObservableValue<O> zip(List<? extends Observable<I>> sources, Function<List<I>, O> combiner) {
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
     * @param <I>        the input type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map} parameter provides the indexes of the
     * resulting {@link ObservableValue} values.
     */
    public static <K, I> ObservableValue<Map<K, I>> zip(Map<K, ? extends Observable<I>> sourcesMap) {
        return zip(sourcesMap, Function.identity());
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources the input {@link ObservableValue}s
     * @param <I>     the input type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting {@link ObservableValue}.
     */
    public static <I> ObservableValue<List<I>> zip(List<? extends Observable<I>> sources) {
        return zip(sources, Function.identity());
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <I>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
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
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The order of the input values of the combiner is the same as the order of the provided source {@link ObservableValue}s
     *
     * @param sources  the input {@link ObservableValue}s
     * @param combiner the combining function that will produce the result
     * @param <I>      the input type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source publishes the result of the
     * combiner applied with the latest values of the other inputs.
     */
    public static <I, O> ObservableValue<O> combineLatest(List<? extends Observable<I>> sources,
                                                          Function<List<I>, O> combiner) {
        return combineLatest(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <I>        the input type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     */
    public static <K, I> ObservableValue<Map<K, I>> combineLatest(Map<K, ? extends Observable<I>> sourcesMap) {
        return combineLatest(sourcesMap, Function.identity());
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a list containing the values of each {@link ObservableValue}
     *
     * @param sources the input {@link ObservableValue}s
     * @param <I>     the input type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a list containing the values of each {@link ObservableValue}
     */
    public static <I> ObservableValue<List<I>> combineLatest(List<? extends Observable<I>> sources) {
        return combineLatest(sources, Function.identity());
    }

    /**
     * Sets a static, framework-wide uncaught exception handler. It is called in the following cases:
     * <ul>
     *     <li>If an observer onNext/onException throws, with an {@link UpdateDeliveryException}</li>
     *     <li>If an observer does not implement onException and an exception would be delivered, by
     *     an {@link UnhandledException}</li>
     * </ul>
     * In either case, getCause() can be used to obtain the original exception.
     *
     * @see UpdateDeliveryException
     * @see UnhandledException
     * @param handler the exception handler to be called
     */
    public static void setUncaughtExceptionHandler(Consumer<Exception> handler) {
        DispatchingObservable.setUncaughtExceptionHandler(handler);
    }

    private static <V> Map<Integer, V> toIndexMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(toMap(Function.identity(), list::get));
    }

    private static <V> List<V> fromIndexMap(Map<Integer, V> map) {
        return IntStream.range(0, map.size()).boxed().map(map::get).collect(toList());
    }

}
