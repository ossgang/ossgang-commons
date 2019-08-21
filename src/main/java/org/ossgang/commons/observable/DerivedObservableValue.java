package org.ossgang.commons.observable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Collections.newSetFromMap;
import static org.ossgang.commons.monads.Maybe.attempt;
import static org.ossgang.commons.observable.ObservableValue.ObservableValueSubscriptionOption.FIRST_UPDATE;
import static org.ossgang.commons.observable.WeakObservers.weakWithErrorAndSubscriptionCountHandling;

/**
 * An {@link ObservableValue} which gets its data from a parent (upstream) {@link ObservableValue} or {@link Observable},
 * applying a transformation. Transformations can include arbitrary mapping and/or filtering. If a transformation fails
 * (the mapping function throws), a warning is issued and the value is discarded.
 * <p>
 * The subscription to the upstream observable is eager (as soon as this class is instantiated), even if there are no
 * subscribers.
 * <p>
 * This class makes sure that it will not be garbage collected as long as there is at least one subscriber subscribed
 * to the observable. If there are no subscribers to a derived observable, it becomes garbage collectible (provided
 * that no other references to it exist).
 * <p>
 * There is no guarantee that a call to get() will return the latest item of the upstream observable.
 *
 * @param <S> the type of the source observable
 * @param <D> the type of this observable
 */
public class DerivedObservableValue<S, D> extends DispatchingObservableValue<D> implements ObservableValue<D> {
    private final static Set<DerivedObservableValue<?, ?>> GC_PROTECTION = newSetFromMap(new ConcurrentHashMap<>());
    private final Function<S, Optional<D>> mapper;

    DerivedObservableValue(Observable<S> sourceObservable, Function<S, Optional<D>> mapper) {
        super(null);
        this.mapper = mapper;
        sourceObservable.subscribe(weakWithErrorAndSubscriptionCountHandling(this,
                DerivedObservableValue::deriveUpdate,
                DerivedObservableValue::dispatchException,
                DerivedObservableValue::upstreamObserverSubscriptionCountChanged), FIRST_UPDATE);
    }

    private void deriveUpdate(S item) {
        transform(item).ifPresent(this::dispatchValue);
    }

    private Optional<D> transform(S value) {
        return attempt(() -> mapper.apply(value)) //
                .onException(this::dispatchException) //
                .optionalValue() //
                .orElseGet(Optional::empty);
    }

    private void upstreamObserverSubscriptionCountChanged(int refCount) {
        if (refCount == 0) {
            /* the upstream subscription was terminated. terminate downstream subscriptions, eventually
               allowing GC'ing this derived value. */
            unsubscribeAllObservers();
        }
    }

    @Override
    protected void firstListenerAdded() {
        GC_PROTECTION.add(this);
    }

    @Override
    protected void lastListenerRemoved() {
        GC_PROTECTION.remove(this);
    }
}
