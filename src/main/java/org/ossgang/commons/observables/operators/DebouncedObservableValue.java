/*
 * @formatter:off
 * Copyright (c) 2008-2020, CERN. All rights reserved.
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
 * @formatter:on
 */

package org.ossgang.commons.observables.operators;

import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observables.weak.WeakObservers.weakWithErrorAndSubscriptionCountHandling;
import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.ossgang.commons.observables.DispatchingObservableValue;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;

/**
 * An {@link ObservableValue} that only dispatch those items dispatched by the source {@link ObservableValue} that are not
 * followed by another item within the specified time window.
 * NOTE: if the source {@link ObservableValue} always dispatch items with a shorter rate then the time window, then
 * this debounced {@link ObservableValue} will never dispatch !
 *
 * @param <T> the type of the {@link ObservableValue}
 */
public class DebouncedObservableValue<T> extends DispatchingObservableValue<T> {

    private final ScheduledExecutorService debouncerExecutor;

    private final AtomicReference<ScheduledFuture<?>> callback;
    private final long debouncePeriodMs;

    public DebouncedObservableValue(Observable<T> source, Duration debouncePeriod) {
        super(null);
        this.debouncePeriodMs = debouncePeriod.toMillis();
        this.callback = new AtomicReference<>();
        this.debouncerExecutor = Executors.newSingleThreadScheduledExecutor(daemonThreadFactoryWithPrefix("ossgang-Observable-debounce-" + System.identityHashCode(this) + "-"));
        source.subscribe(weakWithErrorAndSubscriptionCountHandling(this,
                DebouncedObservableValue::debounceValue,
                DebouncedObservableValue::dispatchException,
                DebouncedObservableValue::upstreamObserverSubscriptionCountChanged), FIRST_UPDATE);
    }

    private void debounceValue(T s) {
        callback.updateAndGet(scheduledCallback -> {
            if (scheduledCallback != null) {
                scheduledCallback.cancel(false);
            }
            return debouncerExecutor.schedule(() -> dispatchValue(s), debouncePeriodMs, TimeUnit.MILLISECONDS);
        });
    }

    private void upstreamObserverSubscriptionCountChanged(int refCount) {
        if (refCount == 0) {
            /* the upstream subscription was terminated. terminate downstream subscriptions, eventually
               allowing GC'ing this derived value. */
            unsubscribeAllObservers();
        }
    }
}
