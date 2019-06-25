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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public abstract class AbstractDispatchingObservableValue<T> extends AbstractDispatchingObservable<T> implements ObservableValue<T> {
    private final AtomicReference<T> lastValue;

    protected AbstractDispatchingObservableValue(T initial) {
        lastValue = new AtomicReference<>(initial);
    }

    @Override
    public Subscription subscribe(Consumer<? super T> listener, SubscriptionOption... options) {
        Set<SubscriptionOption> optionSet = new HashSet<>(Arrays.asList(options));
        if (optionSet.contains(FIRST_UPDATE)) {
            Optional.ofNullable(lastValue.get()).ifPresent(listener);
        }
        return super.subscribe(listener, options);
    }

    @Override
    protected void update(T newValue) {
        requireNonNull(newValue, "null value not allowed");
        if (Objects.equals(lastValue.getAndSet(newValue), newValue)) {
            super.update(newValue, s -> !s.contains(ON_CHANGE));
        } else {
            super.update(newValue);
        }
    }

    @Override
    public T get() {
        return lastValue.get();
    }
}