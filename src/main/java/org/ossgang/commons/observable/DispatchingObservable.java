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

package org.ossgang.commons.observable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A basic implementation of {@link Observable} managing a set of listeners, and dispatching updates to them.
 *
 * @param <T> the type of the observable
 */
public class DispatchingObservable<T> implements Observable<T> {
    private final Map<Observer<? super T>, Set<SubscriptionOption>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService dispatcher = Executors.newCachedThreadPool();
    private final AtomicInteger listenerCount = new AtomicInteger(0);

    protected DispatchingObservable() {
    }

    @Override
    public Subscription subscribe(Observer<? super T> listener, SubscriptionOption... options) {
        Set<SubscriptionOption> optionSet = new HashSet<>(Arrays.asList(options));
        addListener(listener, optionSet);
        ObservableSubscription observableSubscription = new ObservableSubscription(listener);
        listener.onSubscribe(observableSubscription);
        return observableSubscription;
    }

    private void addListener(Observer<? super T> listener, Set<SubscriptionOption> subscriptionOptions) {
        listeners.put(listener, subscriptionOptions);
        if (listenerCount.getAndIncrement() == 0) {
            firstListenerAdded();
        }
    }

    protected void firstListenerAdded() { /* no op */ }

    private void removeListener(Observer<? super T> listener) {
        if (listeners.remove(listener) != null) {
            if (listenerCount.decrementAndGet() == 0) {
                lastListenerRemoved();
            }
        }
    }

    protected void lastListenerRemoved() { /* no op */ }

    protected void dispatchValue(T newValue) {
        listeners.keySet().forEach(l -> dispatch(l::onValue, newValue));
    }

    protected void dispatchValue(T newValue, Predicate<Set<SubscriptionOption>> optionPredicate) {
        listeners.entrySet().stream() //
                .filter(entry -> optionPredicate.test(entry.getValue())) //
                .map(Map.Entry::getKey) //
                .forEach(l -> dispatch(l::onValue, newValue));
    }

    protected void dispatchException(Throwable newValue) {
        listeners.keySet().forEach(l -> dispatch(l::onException, newValue));
    }

    protected void dispatchException(Throwable newValue, Predicate<Set<SubscriptionOption>> optionPredicate) {
        listeners.entrySet().stream() //
                .filter(entry -> optionPredicate.test(entry.getValue())) //
                .map(Map.Entry::getKey) //
                .forEach(l -> dispatch(l::onException, newValue));
    }

    private <X> void dispatch(Consumer<X> handler, X value) {
        dispatcher.submit(() -> {
            try {
                handler.accept(value);
            } catch (Exception e) {
                System.err.println("Error in event handler\nvalue: " + value);
                e.printStackTrace();
            }
        });
    }

    private class ObservableSubscription implements Subscription {
        private final Observer<? super T> listener;

        private ObservableSubscription(Observer<? super T> listener) {
            this.listener = listener;
        }

        @Override
        public void unsubscribe() {
            removeListener(listener);
            listener.onUnsubscribe(this);
        }
    }
}
