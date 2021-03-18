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

import org.ossgang.commons.observables.exceptions.UnhandledException;
import org.ossgang.commons.observables.exceptions.UpdateDeliveryException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.ossgang.commons.observables.ExceptionHandlers.dispatchToUncaughtExceptionHandler;
import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;

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
    private static final Set<Observable<?>> GC_PROTECTION = newSetFromMap(new ConcurrentHashMap<>());
    private static final ExecutorService DISPATCHER_POOL =
            newCachedThreadPool(daemonThreadFactoryWithPrefix("ossgang-Observable-dispatcher"));
    private final Map<Observer<? super T>, ObservableSubscription> listeners = new ConcurrentHashMap<>();
    private final AtomicInteger listenerCount = new AtomicInteger(0);

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
        ObservableSubscription subscription = new ObservableSubscription(listener, options);
        listeners.put(listener, subscription);
        if (listenerCount.getAndIncrement() == 0) {
            GC_PROTECTION.add(this);
        }
        return subscription;
    }

    private void removeListener(Observer<? super T> listener) {
        if (listeners.remove(listener) != null) {
            if (listenerCount.decrementAndGet() == 0) {
                GC_PROTECTION.remove(this);
            }
        }
    }

    protected void unsubscribeAllObservers() {
        Set<Subscription> allSubscriptions = new HashSet<>(listeners.values());
        allSubscriptions.forEach(Subscription::unsubscribe);
    }

    protected void dispatchValue(T newValue) {
        listeners.keySet().forEach(l -> dispatch(l::onValue, newValue));
    }

    protected void dispatchValue(T newValue, Predicate<Set<SubscriptionOption>> optionPredicate) {
        listeners.entrySet().stream() //
                .filter(entry -> optionPredicate.test(entry.getValue().options)) //
                .map(Map.Entry::getKey) //
                .forEach(l -> dispatch(l::onValue, newValue));
    }

    protected void dispatchException(Throwable exception) {
        AtomicBoolean wasDispatched = new AtomicBoolean(false);
        listeners.keySet().forEach(l -> {
            dispatch(l::onException, exception);
            wasDispatched.set(true);
        });
        if (!wasDispatched.get()) {
            dispatchToUncaughtExceptionHandler(new UnhandledException(exception));
        }
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


    private class ObservableSubscription implements Subscription {
        private final Observer<? super T> listener;
        private final Set<SubscriptionOption> options;

        private ObservableSubscription(Observer<? super T> listener, Set<SubscriptionOption> options) {
            this.listener = listener;
            this.options = options;
        }

        @Override
        public void unsubscribe() {
            removeListener(listener);
            listener.onUnsubscribe(this);
        }
    }
}
