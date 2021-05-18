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

package org.ossgang.commons.properties;

import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.Subscription;
import org.ossgang.commons.observables.SubscriptionOption;

import java.util.function.Consumer;

/**
 * A {@link Property} that will delegate its {@link Property#set(Object)}, {@link Property#get()} and {@link #subscribe(Observer, SubscriptionOption...)}
 * methods to the provided {@link ObservableValue} and {@link Consumer}.
 *
 * @param <T> the type of the property
 */
public class WrapperProperty<T> implements Property<T>, ObservableValue<T> {
    private final ObservableValue<T> observableValue;
    private final Consumer<? super T> setConsumer;

    WrapperProperty(ObservableValue<T> updateProvider, Consumer<? super T> setConsumer) {
        this.observableValue = updateProvider;
        this.setConsumer = setConsumer;
    }

    @Override
    public void set(T value) {
        setConsumer.accept(value);
    }

    @Override
    public T get() {
        return observableValue.get();
    }

    @Override
    public Subscription subscribe(Observer<? super T> listener, SubscriptionOption... options) {
        return observableValue.subscribe(listener, options);
    }
}
