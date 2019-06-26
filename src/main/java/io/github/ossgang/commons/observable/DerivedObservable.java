package io.github.ossgang.commons.observable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Collections.newSetFromMap;

public class DerivedObservable<S,D> extends DispatchingObservable<D> implements Observable<D> {
    private final static Set<DerivedObservable<?,?>> GC_PROTECTION = newSetFromMap(new ConcurrentHashMap<>());
    private final Function<S, Optional<D>> mapper;

    DerivedObservable(Observable<S> sourceObservable, Function<S, Optional<D>> mapper) {
        this.mapper = mapper;
        sourceObservable.subscribe(this::deriveUpdate, WEAK);
    }

    private void deriveUpdate(S item) {
        mapper.apply(item).ifPresent(this::update);
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
