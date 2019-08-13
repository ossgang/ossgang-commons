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

package org.ossgang.commons.property;

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
     * @param <T> the type
     * @return the new property
     */
    public static <T> Property<T> property(T initialValue) {
        return new SimpleProperty<>(initialValue);
    }

    /**
     * Create a {@link Property} with NO initial value.
     *
     * @param <T> the type
     * @return the new property
     */
    public static <T> Property<T> property() {
        return new SimpleProperty<>(null);
    }

}