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

import java.util.function.Consumer;

/**
 * A generic "observable" value of type T. An Observable&lt;T&gt; contains a value of type T (which can be retrieved
 * at any time), but also allows interested parties to observe the value for future changes.
 * @param <T> the wrapped data type
 */
public interface Observable<T> {
    /**
     * Get the wrapped latest value.
     * @return The latest value, or null if no value exists.
     */
    T value();

    /**
     * Add a listener to observe this value for future values.
     * If a value is present when the listener is registered, it will be called with that value upon registration.
     * However, there is no guarantee at which time or in which order listeners are notified.
     *
     * Implementations are expected to hold strong references to the listeners added. In order to remove a listener,
     * call unsubscribe().
     * @param listener the listener to add
     */
    void subscribe(Consumer<T> listener);

    /**
     * Remove a previously registered listener
     * @param listener the listener
     * @throws IllegalArgumentException if the listener has not been registered with this observable
     */
    void unsubscribe(Consumer<T> listener);

}
