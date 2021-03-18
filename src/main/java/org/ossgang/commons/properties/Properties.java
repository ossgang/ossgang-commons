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

import java.util.function.Consumer;

import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.SubscriptionOption;

/**
 * Static entry point to create properties.
 */
public class Properties {
    private Properties() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Create a {@link Property} with an initial value.
     *
     * @param initialValue the initial value
     * @param <T>          the type of the {@link Property}
     * @return the new property
     */
    public static <T> Property<T> property(T initialValue) {
        return new SimpleProperty<>(initialValue);
    }

    /**
     * Create a {@link Property} with NO initial value.
     *
     * @param <T> the type of the {@link Property}
     * @return the new property
     */
    public static <T> Property<T> property() {
        return new SimpleProperty<>(null);
    }

    /**
     * Create a {@link Property} that will bind the {@link Property#get()} and {@link Property#subscribe(Observer, SubscriptionOption...)}
     * to the specified {@link ObservableValue} and the {@link Property#set(Object)} to the specified {@link Consumer}
     *
     * @param updateProvider the {@link ObservableValue} for the {@link Property#get()} and {@link Property#subscribe(Observer, SubscriptionOption...)}
     * @param setConsumer    the {@link Consumer} for the {@link Property#set(Object)}
     * @param <T>            the type of the {@link Property}
     * @return the new wrapper {@link Property}
     */
    public static <T> Property<T> wrapperProperty(ObservableValue<T> updateProvider, Consumer<? super T> setConsumer) {
        return new WrapperProperty<>(updateProvider, setConsumer);
    }
}
