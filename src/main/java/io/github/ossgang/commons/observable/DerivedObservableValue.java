package io.github.ossgang.commons.observable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.github.ossgang.commons.monads.Maybe.attempt;
import static java.util.Collections.newSetFromMap;

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
