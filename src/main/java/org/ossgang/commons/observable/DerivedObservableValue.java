package org.ossgang.commons.observable;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.singletonMap;
import static org.ossgang.commons.monads.Maybe.attempt;
import static org.ossgang.commons.observable.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observable.WeakObservers.weakWithErrorAndSubscriptionCountHandling;

/**
 * An {@link ObservableValue} which gets its data from a parent (upstream) {@link ObservableValue} or {@link Observable},
 * applying a transformation. Transformations can include arbitrary mapping and/or filtering. If a transformation fails
 * (the mapping function throws), the exception is propagated downstream.
 * <p>
 * The subscription to the upstream observable is eager (as soon as this class is instantiated), even if there are no
 * subscribers.
 * <p>
 * There is no guarantee that a call to get() will return the latest item of the upstream observable.
 *
 * @param <K> the indexing type
 * @param <I> the type of the source observable
 * @param <O> the type of this observable
 */
public class DerivedObservableValue<K, I, O> extends DispatchingObservableValue<O> implements ObservableValue<O> {
    private final BiFunction<K, I, Optional<O>> mapper;
    private static final Object SINGLE = new Object();

    private DerivedObservableValue(Map<K, ? extends Observable<I>> sourceObservables, BiFunction<K, I, Optional<O>> mapper) {
        super(null);
        this.mapper = mapper;
        sourceObservables.forEach((key, obs) -> obs.subscribe(weakWithErrorAndSubscriptionCountHandling(this,
                (self, item) -> self.deriveUpdate(key, item),
                DerivedObservableValue::dispatchException,
                DerivedObservableValue::upstreamObserverSubscriptionCountChanged), FIRST_UPDATE));
    }

    public static <K, I, O> ObservableValue<O> derive(Map<K, ? extends Observable<I>> sourceObservables,
                                                      BiFunction<K, I, Optional<O>> mapper) {
        return new DerivedObservableValue<>(sourceObservables, mapper);
    }

    public static <I, O> ObservableValue<O> derive(Observable<I> source, Function<I, Optional<O>> mapper) {
        return new DerivedObservableValue<>(singletonMap(SINGLE, source), (k, v) -> mapper.apply(v));
    }

    private void deriveUpdate(K key, I item) {
        attempt(() -> mapper.apply(key, item)) //
                .onException(this::dispatchException) //
                .optionalValue() //
                .orElseGet(Optional::empty) //
                .ifPresent(this::dispatchValue);
    }

    private void upstreamObserverSubscriptionCountChanged(int refCount) {
        if (refCount == 0) {
            /* the upstream subscription was terminated. terminate downstream subscriptions, eventually
               allowing GC'ing this derived value. */
            unsubscribeAllObservers();
        }
    }
}
