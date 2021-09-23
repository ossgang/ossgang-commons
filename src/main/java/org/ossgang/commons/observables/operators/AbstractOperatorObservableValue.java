package org.ossgang.commons.observables.operators;

import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.ossgang.commons.observables.DispatchingObservableValue;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.Subscription;
import org.ossgang.commons.observables.SubscriptionOption;
import org.ossgang.commons.observables.WeakMethodReferenceObserver;

/**
 * A base {@link ObservableValue} which gets its data from parents (upstream) {@link ObservableValue} or {@link Observable},
 * applying an operation. Operations can be arbitrary but must be side-effect free, e.g. mapping or filtering.
 * If an operation fails (e.g. a mapping function throws), the exception is propagated downstream.
 * <p>
 * The subscription to the upstream observable is eager (as soon as this class is instantiated), even if there are no
 * subscribers. Subscription upstream are "possibly weak". If there are no observers downstream, the upstream observer
 * reference "this" {@link ObservableValue} weakly. As soon as a downstream observer subscribes, the upstream observer
 * reference to "this" {@link ObservableValue} becomes strong. When all downstream observers unsubscribe, the upstream
 * observer reference to "this" {@link ObservableValue} becomes weak again, allowing "this" to be garbage collected.
 * <p>
 * There is no guarantee that a call to get() will return the latest item of the upstream observable.
 *
 * @param <K> the indexing type
 * @param <I> the type of the source observable
 * @param <O> the type of this observable
 */
public abstract class AbstractOperatorObservableValue<K, I, O> extends DispatchingObservableValue<O>
        implements ObservableValue<O> {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Subscription> sourceSubscriptions; /* just used to hold strong references upstream */
    private final List<PossiblyWeakObserver<AbstractOperatorObservableValue<K, I, O>, I>> sourceObservers;
    private int subscriptionCount = 0;

    protected AbstractOperatorObservableValue() {
        super(null);
        this.sourceObservers = new ArrayList<>();
        this.sourceSubscriptions = new ArrayList<>();
    }

    protected void subscribeUpstreamWithFirstUpdate(Map<K, ? extends Observable<I>> sourceObservables) {
        sourceObservables.forEach((key, source) -> {
            PossiblyWeakObserver<AbstractOperatorObservableValue<K, I, O>, I> observer = new PossiblyWeakObserver<>(
                    this, (self, item) -> self.applyOperation(key, item),
                    (self, exception) -> self.dispatchException(exception));
            this.sourceObservers.add(observer);
            this.sourceSubscriptions.add(source.subscribe(observer, FIRST_UPDATE));
        });
    }

    @Override
    protected void subscriptionAdded(Observer<? super O> listener, Set<SubscriptionOption> options) {
        synchronized (sourceObservers) {
            if (subscriptionCount++ == 0) {
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

    /**
     * Apply the operation to item delivered by upstream {@link ObservableValue} identified by the key.
     *
     * @param key  corresponding to the source {@link ObservableValue} that delivered the value
     * @param item of the source {@link ObservableValue} identified by the key
     */
    abstract protected void applyOperation(K key, I item);

    private static class PossiblyWeakObserver<C, T> extends WeakMethodReferenceObserver<C, T> {
        private final AtomicReference<C> strongHolderRef;

        PossiblyWeakObserver(C holder, BiConsumer<? super C, T> valueConsumer,
                BiConsumer<? super C, Throwable> exceptionConsumer) {
            super(holder, valueConsumer, exceptionConsumer);
            this.strongHolderRef = new AtomicReference<>();
        }

        void makeStrong() {
            strongHolderRef.set(get());
        }

        void makeWeak() {
            strongHolderRef.set(null);
        }
    }

}
