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

/**
 * A "property": an {@link ObservableValue} which can be set.
 *
 * @param <T> the type of the property
 */
public interface Property<T> extends ObservableValue<T> {

    /**
     * Set the property to the given value, notifying all observers.
     * The new value must not be null.
     *
     * @param value the new value.
     * @throws NullPointerException on a null value
     */
    void set(T value);

}
