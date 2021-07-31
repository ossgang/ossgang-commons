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

package org.ossgang.commons.observables.weak;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.Subscription;

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
class WeakMethodReferenceObserver<C, T> implements Observer<T> {
    private final WeakReference<C> holder;
    private final BiConsumer<? super C, T> valueConsumer;
    private final BiConsumer<? super C, Throwable> exceptionConsumer;
    private final BiConsumer<? super C, Integer> subscriptionCountUpdated;
    private final Set<Subscription> subscriptions = new HashSet<>();

    WeakMethodReferenceObserver(C holder, BiConsumer<? super C, T> valueConsumer,
                                BiConsumer<? super C, Throwable> exceptionConsumer,
                                BiConsumer<? super C, Integer> subscriptionCountUpdated) {
        this.holder = new WeakReference<>(holder);
        this.valueConsumer = valueConsumer;
        this.exceptionConsumer = exceptionConsumer;
        this.subscriptionCountUpdated = subscriptionCountUpdated;
    }

    @Override
    public void onException(Throwable t) {
        dispatch(exceptionConsumer, t);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized (subscriptions) {
            subscriptions.add(subscription);
            Optional.ofNullable(holderRef.get()).ifPresent(h -> subscriptionCountUpdated.accept(h, subscriptions.size()));
        }
    }

    @Override
    public void onUnsubscribe(Subscription subscription) {
        synchronized (subscriptions) {
            subscriptions.remove(subscription);
            Optional.ofNullable(holderRef.get()).ifPresent(h -> subscriptionCountUpdated.accept(h, subscriptions.size()));
        }
    }

    private <X> void dispatch(BiConsumer<? super C, X> consumer, X item) {
        C holderInstance = holderRef.get();
        if (holderInstance != null) {
            consumer.accept(holderInstance, item);
        } else {
            unsubscribeAll();
        }
    }

    private static class ObserverPhantomReference<C> extends PhantomReference<C> {

        private final Runnable finalizeAction;
        private final ReferenceQueue<? super C> referenceQueue;

        public ObserverPhantomReference(C holder, Runnable finalizeAction, ReferenceQueue<? super C> referenceQueue) {
            super(holder, referenceQueue);
            this.finalizeAction = finalizeAction;
            this.referenceQueue = referenceQueue;
        }

        public boolean attemptFinalize() {
            Reference<?> ref = referenceQueue.poll();
            if (ref != null) {
                finalizeAction.run();
                return true;
            }
            return false;
        }

    }

}
