package org.ossgang.commons.observables.operators;

import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.singletonMap;
import static org.ossgang.commons.monads.Maybe.attempt;

/**
 * An {@link ObservableValue} which gets its data from a parent (upstream) {@link ObservableValue} or {@link Observable},
 * applying a transformation. Transformations can include arbitrary mapping and/or filtering. If a transformation fails
 * (the mapping function throws), the exception is propagated downstream.
 *
 * @param <K> the indexing type
 * @param <I> the type of the source observable
 * @param <O> the type of this observable
 */
public class DerivedObservableValue<K, I, O> extends AbstractOperatorObservableValue<K, I, O> {

    private static final Object SINGLE = new Object();
    private final BiFunction<K, I, Optional<O>> mapper;

    private DerivedObservableValue(Map<K, ? extends Observable<I>> sourceObservables,
                                   BiFunction<K, I, Optional<O>> mapper) {
        this.mapper = mapper;
        super.subscribeUpstreamWithFirstUpdate(sourceObservables);
    }

    public static <K, I, O> ObservableValue<O> derive(Map<K, ? extends Observable<I>> sourceObservables,
                                                      BiFunction<K, I, Optional<O>> mapper) {
        return new DerivedObservableValue<>(sourceObservables, mapper);
    }

    public static <I, O> ObservableValue<O> derive(Observable<I> source, Function<I, Optional<O>> mapper) {
        return new DerivedObservableValue<>(singletonMap(SINGLE, source), (k, v) -> mapper.apply(v));
    }

    @Override
    protected void applyOperation(K key, I item) {
        attempt(() -> mapper.apply(key, item)) //
                .ifException(this::dispatchException) //
                .optionalValue() //
                .orElseGet(Optional::empty) //
                .ifPresent(this::dispatchValue);
    }

}
