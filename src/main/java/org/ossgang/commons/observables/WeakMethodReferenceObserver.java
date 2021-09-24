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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * An observer based on a weak reference to an object, and class method references to consumers for values (and,
 * optionally, exceptions).
 * Release the reference to the subscriber as soon as the (weak-referenced) holder object is GC'd. A common use
 * case for this is e.g. working with method references within a particular object:
 * <pre>
 * class Test {
 *     public void doSubscribe(Observable&lt;String&gt; obs) {
 *         obs.subscribe(weak(this, Test::handle));
 *     }
 *     private void handle(String update) ...
 * }
 * </pre>
 * This will allow the "Test" instance to be GC'd, terminating the subscription; but for as long as it lives, the
 * subscription will be kept alive.
 */
public class WeakMethodReferenceObserver<C, T> extends WeakReference<C> implements Observer<T> {
    private static final ReferenceQueue<Object> STALE_WEAK_OBSERVERS = new ReferenceQueue<>();
    private BiConsumer<? super C, T> valueConsumer;
    private BiConsumer<? super C, Throwable> exceptionConsumer;
    private Set<Subscription> subscriptions;

    protected WeakMethodReferenceObserver(C holder, BiConsumer<? super C, T> valueConsumer,
            BiConsumer<? super C, Throwable> exceptionConsumer) {
        super(holder, STALE_WEAK_OBSERVERS);
        this.subscriptions = new HashSet<>();
        this.valueConsumer = valueConsumer;
        this.exceptionConsumer = exceptionConsumer;
    }

    protected WeakMethodReferenceObserver(C holder, BiConsumer<? super C, T> valueConsumer) {
        this(holder, valueConsumer, (r, e) -> {
            throw new UnhandledException(e);
        });
    }

    @Override
    public void onValue(T t) {
        dispatch(valueConsumer, t);
    }

    @Override
    public void onException(Throwable t) {
        dispatch(exceptionConsumer, t);
    }

    @Override
    public synchronized void onSubscribe(Subscription subscription) {
        if (subscriptions == null) {
            subscription.unsubscribe();
            throw new IllegalStateException("Weak observer has been garbage collected and can not be re-used!");
        }
        subscriptions.add(subscription);
    }

    @Override
    public synchronized void onUnsubscribe(Subscription subscription) {
        if (subscriptions == null) {
            return;
        }
        subscriptions.remove(subscription);
    }

    private synchronized void cleanUp() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions = null;
        valueConsumer = null;
        exceptionConsumer = null;
    }

    private <X> void dispatch(BiConsumer<? super C, X> consumer, X item) {
        Optional.ofNullable(get()).ifPresent(ref -> consumer.accept(ref, item));
    }

    static {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Reference<?> ref = STALE_WEAK_OBSERVERS.remove();
                    WeakMethodReferenceObserver<?, ?> observer = (WeakMethodReferenceObserver<?, ?>) ref;
                    observer.cleanUp();
                } catch (Exception e) {
                    ExceptionHandlers.dispatchToUncaughtExceptionHandler(
                            new IllegalStateException("Error in WeakMethodReferenceObserver finalizer", e));
                }
            }
        });
        cleanupThread.setName("ossgang-commons-weakobserver-finalizer");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
}
