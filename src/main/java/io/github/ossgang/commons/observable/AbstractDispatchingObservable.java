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

package io.github.ossgang.commons.observable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNull;

/**
 * An abstract implementation of {@link Observable} with a dispatcher-listener pattern. This class keeps track of a
 * list of added listeners and the latest value in a thread-safe way.
 *
 * Implementations should call the (protected) dispatch(T) method to dispatch updates to the value.
 * @param <T> The data type
 */
public abstract class AbstractDispatchingObservable<T> implements Observable<T> {
    private final Set<Consumer<T>> listeners = newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicReference<T> lastValue = new AtomicReference<>();
    private final ExecutorService dispatcher = Executors.newCachedThreadPool();

    protected AbstractDispatchingObservable(T initial) {
        lastValue.set(initial);
    }

    public void unsubscribe(Consumer<T> listener) {
        if (!listeners.remove(listener)) {
            throw new IllegalArgumentException("The listener " + listener + " is not known to this observable.");
        }
    }

    public void subscribe(Consumer<T> listener) {
        listeners.add(listener);
        T value = lastValue.get();
        if (value != null) {
            dispatch(listener, value);
        }
    }

    protected void dispatch(T newValue) {
        requireNonNull(newValue, "null value not allowed");
        if (!Objects.equals(lastValue.getAndSet(newValue), newValue)) {
            listeners.forEach(l -> dispatch(l, newValue));
        }
    }

    private void dispatch(Consumer<T> listener, T value) {
        dispatcher.submit(() -> {
            try {
                listener.accept(value);
            } catch (Exception e) {
                System.err.println("Error in event handler\nvalue: " + value);
                e.printStackTrace();
            }
        });
    }

    public T value() {
        return lastValue.get();
    }
}
