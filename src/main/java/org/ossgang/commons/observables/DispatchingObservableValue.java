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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observables.SubscriptionOptions.ON_CHANGE;
import static org.ossgang.commons.utils.Uncheckeds.uncheckedConsumer;

/**
 * A basic implementation of {@link ObservableValue}, based on {@link DispatchingObservable} to handle the update
 * dispatching, and keeping track of the latest value in a thread safe way.
 * <p>
 * The value is only allowed to be null for an "uninitialized" {@link ObservableValue}. Trying to dispatch an update
 * with a null value will raise an exception.
 *
 * @param <T> the type of the observable
 */
public class DispatchingObservableValue<T> extends DispatchingObservable<T> implements ObservableValue<T> {
    private final AtomicReference<T> lastValue;

    protected DispatchingObservableValue(T initial) {
        lastValue = new AtomicReference<>(initial);
    }

    @Override
    public Subscription subscribe(Observer<? super T> listener, SubscriptionOption... options) {
        Set<SubscriptionOption> optionSet = new HashSet<>(Arrays.asList(options));
        Subscription subscription = super.subscribe(listener, options);
        if (optionSet.contains(FIRST_UPDATE)) {
            Optional.ofNullable(lastValue.get())
                    .ifPresent(uncheckedConsumer(value -> dispatch(listener::onValue, value).get()));
        }
        return subscription;
    }

    @Override
    protected void dispatchValue(T newValue) {
        requireNonNull(newValue, "null value not allowed");
        update(ar -> ValueTransition.fromTo(ar.getAndSet(newValue), newValue));
    }

    /**
     * applies the given function onto the internal atomic reference and publishes the resulting new value downstream.
     * This is exposed in order to be used by subclasses to perform atomic operations other than simple set.
     * NB: the client of this method has to take care that the operation itself is atomic!
     */
    protected ValueTransition<T> update(Function<AtomicReference<T>, ValueTransition<T>> operation) {
        ValueTransition<T> transition = operation.apply(lastValue);
        if (Objects.equals(transition.oldValue(), transition.newValue())) {
            super.dispatchValue(transition.newValue(), s -> !s.contains(ON_CHANGE));
        } else {
            super.dispatchValue(transition.newValue());
        }
        return transition;
    }

    @Override
    public T get() {
        return lastValue.get();
    }
}
