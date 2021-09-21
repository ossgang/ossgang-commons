package org.ossgang.commons.observables.operators;

import static java.util.Collections.singletonMap;
import static org.ossgang.commons.monads.Maybe.attempt;
import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.ossgang.commons.observables.DispatchingObservableValue;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.Subscription;
import org.ossgang.commons.observables.SubscriptionOption;

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
    private final List<PossiblyWeakObserver<DerivedObservableValue<K, I, O>, I>> sourceObservers;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Subscription> sourceSubscriptions; /* just used to hold strong references upstream */

    private int subscriptionCount = 0;

    private DerivedObservableValue(Map<K, ? extends Observable<I>> sourceObservables,
            BiFunction<K, I, Optional<O>> mapper) {
        super(null);
        this.mapper = mapper;
        this.sourceObservers = new ArrayList<>();
        this.sourceSubscriptions = new ArrayList<>();
        sourceObservables.forEach((key, obs) -> {
            PossiblyWeakObserver<DerivedObservableValue<K, I, O>, I> observer = new PossiblyWeakObserver<>(this,
                    (self,item) -> self.deriveUpdate(key, item),
                    (self, exception) -> self.dispatchException(exception));
            this.sourceObservers.add(observer);
            this.sourceSubscriptions.add(obs.subscribe(observer, FIRST_UPDATE));
        });
    }

    @Override
    protected void subscriptionAdded(Observer<? super O> listener, Set<SubscriptionOption> options) {
        synchronized (sourceObservers) {
            if(subscriptionCount++ == 0) {
                sourceObservers.forEach(PossiblyWeakObserver::makeStrong);
            }
        }
    }

    @Override
    protected void subscriptionRemoved(Observer<? super O> listener) {
        synchronized (sourceObservers) {
            if (--subscriptionCount == 0) {
                sourceObservers.forEach(PossiblyWeakObserver::makeWeak);
            }
        }
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
                .ifException(this::dispatchException) //
                .optionalValue() //
                .orElseGet(Optional::empty) //
                .ifPresent(this::dispatchValue);
    }

    private static class PossiblyWeakObserver<C, T> implements Observer<T> {
        private final WeakReference<C> holderRef;
        private final AtomicReference<C> strongHolderRef;
        private final BiConsumer<? super C, T> valueConsumer;
        private final BiConsumer<? super C, Throwable> exceptionConsumer;

        PossiblyWeakObserver(C holder, BiConsumer<? super C, T> valueConsumer,
                BiConsumer<? super C, Throwable> exceptionConsumer) {
            this.holderRef = new WeakReference<>(holder);
            this.strongHolderRef = new AtomicReference<>();
            this.valueConsumer = valueConsumer;
            this.exceptionConsumer = exceptionConsumer;
        }

        public void makeStrong() {
            strongHolderRef.set(holderRef.get());
        }

        public void makeWeak() {
            strongHolderRef.set(null);
        }

        @Override
        public void onValue(T t) {
            dispatch(valueConsumer, t);
        }

        @Override
        public void onException(Throwable t) {
            dispatch(exceptionConsumer, t);
        }

        private <X> void dispatch(BiConsumer<? super C, X> consumer, X item) {
            Optional.ofNullable(holderRef.get()).ifPresent(ref -> consumer.accept(ref, item));
        }
    }

}
