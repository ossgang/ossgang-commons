package org.ossgang.commons.observable;

import org.ossgang.commons.property.Property;

import java.util.*;
import java.util.function.Function;

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
     * Merges any Observables of the same type into one. Updates whenever all sources have provided at least one item.
     */
    @SafeVarargs
    public static <K, V> ObservableValue<Map<K, V>> zip(Function<K, ObservableValue<V>> supplier, K... items) {
        return zip(supplier, Arrays.asList(items));
    }

    /**
     * Merges any Observables of the same type into one. Updates whenever all sources have provided at least one item.
     */
    public static <K, V> ObservableValue<Map<K, V>> zip(Function<K, ObservableValue<V>> supplier, Collection<K> items) {
        Property<Map<K, V>> combinedProperty = property();
        Map<K, V> values = new HashMap<>();
        Set<K> itemSet = new HashSet<>(items);
        for (K item : items) {
            supplier.apply(item).subscribe(value -> {
                synchronized (values) {
                    values.put(item, value);
                    if (values.keySet().containsAll(itemSet)) {
                        combinedProperty.set(new HashMap<>(values));
                        values.clear();
                    }
                }
            }, FIRST_UPDATE);
        }
        return combinedProperty;
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
    public static <I, O> ObservableValue<O> mergeLatest(List<ObservableValue<I>> sources,
                                                        Function<List<I>, O> combiner) {
        Property<O> mergedProperty = property();
        Map<ObservableValue<I>, I> latestValues = new HashMap<>();

        for (ObservableValue<I> source : sources) {
            source.subscribe(sourceValue -> {
                synchronized (latestValues) {
                    latestValues.put(source, sourceValue);

                    if (latestValues.keySet().containsAll(sources)) {
                        List<I> latestValueSnapshotInOrder = new ArrayList<>();
                        for (ObservableValue<I> s : sources) {
                            latestValueSnapshotInOrder.add(latestValues.get(s));
                        }

                        mergedProperty.set(combiner.apply(latestValueSnapshotInOrder));
                    }
                }
            }, FIRST_UPDATE);
        }

        return mergedProperty;
    }

}
