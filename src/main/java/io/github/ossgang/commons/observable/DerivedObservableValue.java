package io.github.ossgang.commons.observable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.github.ossgang.commons.monads.Maybe.attempt;
import static io.github.ossgang.commons.observable.Observable.ObservableSubscriptionOption.WEAK;
import static java.util.Collections.newSetFromMap;

/**
 * An {@link ObservableValue} which gets its data from a parent (upstream) {@link ObservableValue}, applying a
 * transformation. Transformations can include arbitrary mapping and/or filtering. If a transformation fails (the
 * mapping function throws), a warning is issued and the value is discarded.
 *
 * The subscription to the upstream observable is eager (as soon as this class is instantiated), even if there are no
 * subscribers.
 *
 * This class makes sure that it will not be garbage collected as long as there is at least one subscriber subscribed
 * to the observable. If there are no subscribers to a derived observable, it becomes garbage collectible (provided
 * that no other references to it exist).
 *
 * There is no guarantee that a call to get() will return the latest item of the upstream observable.
 *
 * @param <S> the type of the source observable
 * @param <D> the type of this observable
 */
public class DerivedObservableValue<S,D> extends DispatchingObservableValue<D> implements ObservableValue<D> {
    private final static Set<DerivedObservableValue<?,?>> GC_PROTECTION = newSetFromMap(new ConcurrentHashMap<>());
    private final Function<S, Optional<D>> mapper;

    DerivedObservableValue(ObservableValue<S> sourceObservable, Function<S, Optional<D>> mapper) {
        super(null);
        this.mapper = mapper;
        Optional.ofNullable(sourceObservable.get()).ifPresent(this::deriveUpdate);
        sourceObservable.subscribe(this::deriveUpdate, WEAK);
    }

    private void deriveUpdate(S item) {
        transform(item).ifPresent(this::update);
    }

    private Optional<D> transform(S value) {
        return attempt(() -> mapper.apply(value)) //
                .onException(ex -> transformationException(value, ex)) //
                .optionalValue() //
                .orElseGet(Optional::empty);
    }

    private void transformationException(S value, Throwable throwable) {
        System.err.println("Error in transformation - discarding value: "+value);
        throwable.printStackTrace();
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
