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

/**
 * An {@link Observable} which can be updated explicitly with new values.
 *
 * @param <T> The value type.
 */
public class Updateable<T> extends AbstractDispatchingObservable<T> implements Observable<T> {
    private Updateable(T initial) {
        super(initial);
    }

    public static <T> Updateable<T> withInitialValue(T initialValue) {
        return new Updateable<>(initialValue);
    }

    public static <T> Updateable<T> empty() {
        return new Updateable<>(null);
    }

    /**
     * Update the value (notifying all registered listeners)
     * @param newValue the new value
     */
    public void update(T newValue) {
        dispatch(newValue);
    }
}
