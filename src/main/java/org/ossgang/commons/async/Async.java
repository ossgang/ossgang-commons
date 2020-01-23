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

package org.ossgang.commons.async;

import org.ossgang.commons.monads.AsyncMaybe;
import org.ossgang.commons.monads.ThrowingRunnable;
import org.ossgang.commons.monads.ThrowingSupplier;

import java.util.function.Supplier;

/**
 *
 * TODO andrea
 */
public final class Async {

    /**
     *
     * TODO andrea
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> AsyncMaybe<T> supplyAsync(Supplier<T> supplier) {
        return AsyncMaybe.attemptAsync(supplier::get);
    }

    /**
     *
     * TODO andrea
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> AsyncMaybe<T> supplyAsync(ThrowingSupplier<T> supplier) {
        return AsyncMaybe.attemptAsync(supplier);
    }

    /**
     *
     * TODO andrea
     * @param runnable
     * @return
     */
    public static AsyncMaybe<Void> runAsync(ThrowingRunnable runnable) {
        return AsyncMaybe.attemptAsync(runnable);
    }

    /**
     *
     * TODO andrea
     * @param runnable
     * @return
     */
    public static AsyncMaybe<Void> runAsync(Runnable runnable) {
        return AsyncMaybe.attemptAsync(runnable::run);
    }

    private Async() {
        /* static only */
    }
}
