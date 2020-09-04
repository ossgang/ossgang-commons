/*
 * @formatter:off
 * Copyright (c) 2008-2020, CERN. All rights reserved.
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
 * @formatter:on
 */

package org.ossgang.commons.monads;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Utility methods for the {@link AsyncMaybe}s
 */
public final class AsyncMaybes {

    /**
     * Creates a {@link CompletableFuture} from the provided {@link AsyncMaybe}.
     *
     * @param asyncMaybe to convert to {@link CompletableFuture}
     * @param <T>        the data value type
     * @return the {@link CompletableFuture} from the provided {@link AsyncMaybe}
     */
    public static <T> CompletableFuture<T> toCompletableFuture(AsyncMaybe<T> asyncMaybe) {
        return CompletableFuture.supplyAsync(() -> asyncMaybe.toMaybeBlocking().value());
    }

    /**
     * Creates a {@link CompletableFuture} from the provided {@link AsyncMaybe}.
     *
     * @param asyncMaybe to convert to {@link CompletableFuture}
     * @param executor   the executor to use for the {@link CompletableFuture} async execution.
     * @param <T>        the data value type
     * @return the {@link CompletableFuture} created from the provided {@link AsyncMaybe}
     */
    public static <T> CompletableFuture<T> toCompletableFuture(AsyncMaybe<T> asyncMaybe, Executor executor) {
        return CompletableFuture.supplyAsync(() -> asyncMaybe.toMaybeBlocking().value(), executor);
    }

    /**
     * Creates an {@link AsyncMaybe} from the provided {@link CompletableFuture}.
     *
     * @param future to convert to {@link AsyncMaybe}
     * @param <T>    the data value type
     * @return the {@link AsyncMaybe} created from the provided {@link CompletableFuture}
     */
    public static <T> AsyncMaybe<T> toAsyncMaybe(CompletableFuture<T> future) {
        return AsyncMaybe.attemptAsync((ThrowingSupplier<T>) future::get);
    }

    private AsyncMaybes() {
        throw new UnsupportedOperationException("static only");
    }

}
