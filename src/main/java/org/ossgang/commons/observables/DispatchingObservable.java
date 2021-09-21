// @formatter:off
/*******************************************************************************
 *
 * This file is part of ossgang-commons.
 *
 * Copyright (c) 2008-2019, CERN. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
// @formatter:on

package org.ossgang.commons.observables;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.ossgang.commons.observables.ExceptionHandlers.dispatchToUncaughtExceptionHandler;
import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.ossgang.commons.observables.exceptions.UnhandledException;
import org.ossgang.commons.observables.exceptions.UpdateDeliveryException;

/**
 * A basic implementation of {@link Observable} managing a set of listeners, and dispatching updates to them.
 * <p>
 * This class makes sure that it will not be garbage collected as long as there is at least one subscriber subscribed
 * to the observable. If there are no subscribers to an observable, it becomes garbage collectible (provided
 * that no other references to it exist).
 *
 * @param <T> the type of the observable
 */
public class DispatchingObservable<T> implements Observable<T> {
    private static final ExecutorService DISPATCHER_POOL = newCachedThreadPool(
            daemonThreadFactoryWithPrefix("ossgang-Observable-dispatcher-"));
    private final Map<Observer<? super T>, ObservableSubscription<T>> listeners = new ConcurrentHashMap<>();

    protected DispatchingObservable() {
    }

    @Override
    public Subscription subscribe(Observer<? super T> listener, SubscriptionOption... options) {
        Set<SubscriptionOption> optionSet = new HashSet<>(Arrays.asList(options));
        ObservableSubscription subscription = addListener(listener, optionSet);
        listener.onSubscribe(subscription);
        return subscription;
    }

    private ObservableSubscription addListener(Observer<? super T> listener, Set<SubscriptionOption> options) {
        ObservableSubscription<T> subscription = new ObservableSubscription<>(this, listener, options);
        if(listeners.put(listener, subscription) == null) {
            subscriptionAdded(listener, options);
        }
        return subscription;
    }

    private void removeListener(Observer<? super T> listener) {
        if(listeners.remove(listener) != null) {
            subscriptionRemoved(listener);
        }
    }

    protected void subscriptionAdded(Observer<? super T> listener, Set<SubscriptionOption> options) {
        /* no op */
    }

    protected void subscriptionRemoved(Observer<? super T> listener) {
        /* no op */
    }

    protected void unsubscribeAllObservers() {
        Set<Subscription> allSubscriptions = new HashSet<>(listeners.values());
        allSubscriptions.forEach(Subscription::unsubscribe);
    }

    protected void dispatchValue(T newValue) {
        dispatchValue(newValue, any -> true);
    }

    protected void dispatchValue(T newValue, Predicate<Set<SubscriptionOption>> optionPredicate) {
        listeners.entrySet().stream() //
                .filter(entry -> optionPredicate.test(entry.getValue().options)) //
                .map(Map.Entry::getKey) //
                .forEach(l -> dispatch(l::onValue, newValue));
    }

    protected void dispatchException(Throwable exception) {
        dispatchException(exception, any -> true);
    }

    protected void dispatchException(Throwable exception, Predicate<Set<SubscriptionOption>> optionPredicate) {
        AtomicBoolean wasDispatched = new AtomicBoolean(false);
        listeners.entrySet().stream() //
                .filter(entry -> optionPredicate.test(entry.getValue().options)) //
                .map(Map.Entry::getKey) //
                .forEach(l -> {
                    dispatch(l::onException, exception);
                    wasDispatched.set(true);
                });
        if (!wasDispatched.get()) {
            dispatchToUncaughtExceptionHandler(new UnhandledException(exception));
        }
    }

    protected <X> Future<?> dispatch(Consumer<X> handler, X value) {
        return DISPATCHER_POOL.submit(() -> {
            try {
                handler.accept(value);
            } catch (UnhandledException e) {
                dispatchToUncaughtExceptionHandler(e);
            } catch (Exception e) {
                dispatchToUncaughtExceptionHandler(new UpdateDeliveryException(value, e));
            }
        });
    }

    private static class ObservableSubscription<T> implements Subscription {
        private final Observer<? super T> listener;
        private final Set<SubscriptionOption> options;
        private final WeakReference<DispatchingObservable<T>> observable;

        private ObservableSubscription(DispatchingObservable<T> observable, Observer<? super T> listener,
                Set<SubscriptionOption> options) {
            this.observable = new WeakReference<>(observable);
            this.listener = listener;
            this.options = options;
        }

        @Override
        public void unsubscribe() {
            Optional.ofNullable(observable.get()).ifPresent(obs -> obs.removeListener(listener));
            listener.onUnsubscribe(this);
        }
    }
}
