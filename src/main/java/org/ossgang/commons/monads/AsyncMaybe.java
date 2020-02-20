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

package org.ossgang.commons.monads;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.ossgang.commons.utils.Exceptions.unchecked;

/**
 * This utility class implements the concept of a "Maybe" or "Try" {@link Optional} that can be resolved at some point
 * in the future. It allows to run code asynchronously and then fetch back or react on the result.
 * In order to fetch the result, it is possible to create a {@link Maybe} in a blocking way. Otherwise, reacting on the
 * completion can be achieved via, e.g., {@link #whenValue(ThrowingConsumer)} or {@link #map(ThrowingFunction)}.
 *
 * A Maybe&lt;T&gt; either carries a T or an exception that occurred when producing it.
 *
 * @param <T>
 */
public class AsyncMaybe<T> {

    /* TODO add here the named thread factory created in PR #38 */
    private static final ExecutorService ASYNC_MAYBE_POOL = Executors.newCachedThreadPool();

    private final CompletableFuture<T> future;
    private final Function<ThrowingSupplier<T>, Maybe<T>> maybeGenerator;

    protected AsyncMaybe(CompletableFuture<T> future, Function<ThrowingSupplier<T>, Maybe<T>> maybeGenerator) {
        this.future = future;
        this.maybeGenerator = maybeGenerator;
    }

    public static <T> AsyncMaybe<T> fromCompletableFuture(CompletableFuture<T> future) {
        return new AsyncMaybe<>(future, Maybe::attempt);
    }

    public static AsyncMaybe<Void> fromVoidCompletableFuture(CompletableFuture<Void> future) {
        return new AsyncMaybe<>(future, valueSupplier -> Maybe.attempt((ThrowingRunnable) future::get));
    }

    /**
     * TODO andrea
     *
     * @param runnable
     * @return
     */
    public static AsyncMaybe<Void> attemptAsync(ThrowingRunnable runnable) {
        return AsyncMaybe.fromVoidCompletableFuture(CompletableFuture.runAsync(unchecked(runnable), ASYNC_MAYBE_POOL));
    }

    /**
     * TODO andrea
     *
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> AsyncMaybe<T> attemptAsync(ThrowingSupplier<T> supplier) {
        return AsyncMaybe.fromCompletableFuture(CompletableFuture.supplyAsync(unchecked(supplier), ASYNC_MAYBE_POOL));
    }

    /**
     * TODO andrea
     *
     * @param value
     * @param <T>
     * @return
     */
    public static <T> AsyncMaybe<T> ofValue(T value) {
        return AsyncMaybe.fromCompletableFuture(CompletableFuture.completedFuture(value));
    }

    /**
     * TODO andrea
     *
     * @return
     */
    public static AsyncMaybe<Void> ofVoid() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return AsyncMaybe.fromVoidCompletableFuture(future);
    }

    /**
     * TODO andrea
     *
     * @param throwable
     * @param <T>
     * @return
     */
    public static <T> AsyncMaybe<T> ofException(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return AsyncMaybe.fromCompletableFuture(future);
    }

    /**
     * TODO andrea + test
     *
     * @param consumer
     * @return
     */
    public AsyncMaybe<T> whenValue(ThrowingConsumer<T> consumer) {
        return AsyncMaybe.fromCompletableFuture(future.whenCompleteAsync((value, exception) -> {
            if (exception == null) {
                unchecked(consumer).accept(value);
            }
        }, ASYNC_MAYBE_POOL));
    }

    /**
     * TODO andrea + test
     *
     * @param consumer
     * @return
     */
    public AsyncMaybe<T> whenComplete(ThrowingConsumer<Maybe<T>> consumer) {
        return AsyncMaybe.fromCompletableFuture(future.whenCompleteAsync((value, exception) -> {
            if (exception != null) {
                unchecked(consumer).accept(Maybe.ofException(exception));
            } else {
                unchecked(consumer).accept(maybeGenerator.apply(() -> value));
            }
        }, ASYNC_MAYBE_POOL));
    }

    /**
     * TODO andrea + test
     *
     * @param consumer
     * @return
     */
    public AsyncMaybe<T> whenComplete(BiConsumer<T, Throwable> consumer) {
        return AsyncMaybe.fromCompletableFuture(future.whenCompleteAsync(consumer, ASYNC_MAYBE_POOL));
    }

    /**
     * TODO andrea + test
     *
     * @param consumer
     * @return
     */
    public AsyncMaybe<T> whenException(ThrowingConsumer<Throwable> consumer) {
        CompletableFuture<T> whenComplete = future.whenCompleteAsync((value, exception) -> {
            if (exception != null) {
                unchecked(consumer).accept(exception);
            }
        }, ASYNC_MAYBE_POOL);
        return AsyncMaybe.fromCompletableFuture(whenComplete);
    }

    /**
     * Apply a transformation function if this {@link AsyncMaybe} is in a "successful" state. Pass through the exception in
     * case it is in an "unsuccessful" state. If the transformation function throws, the exception is returned wrapped
     * in an "unsuccessful" AsyncMaybe.
     *
     * @param function the function to apply
     * @return A successful AsyncMaybe the new value if the transformation succeeds. An unsuccessful AsyncMaybe otherwise.
     */
    public <R> AsyncMaybe<R> map(ThrowingFunction<T, R> function) {
        requireNonNull(function);
        return AsyncMaybe.fromCompletableFuture(future.thenApplyAsync(unchecked(function), ASYNC_MAYBE_POOL));
    }

    /**
     * Apply a void function if this {@link AsyncMaybe} is in a "successful" state. Pass through the exception in case it is
     * in an "unsuccessful" state. If the transformation function throws, the exception is returned wrapped in an
     * "unsuccessful" AsyncMaybe. If it succeeds, return an empty AsyncMaybe&lt;Void&gt;.
     *
     * @param function the function to apply
     * @return A successful AsyncMaybe&lt;Void&gt; if the function is executed and succeeds. An unsuccessful AsyncMaybe otherwise.
     */
    public AsyncMaybe<Void> then(ThrowingConsumer<T> function) {
        requireNonNull(function);
        return AsyncMaybe.fromVoidCompletableFuture(future.thenAcceptAsync(unchecked(function), ASYNC_MAYBE_POOL));
    }

    /**
     * Apply a function if this {@link AsyncMaybe} is in a "successful" state. Pass through the exception in case it is in an
     * "unsuccessful" state. The function does not get the value of the AsyncMaybe passed as parameter, which is useful e.g.
     * for chaining AsyncMaybe&lt;Void&gt;. If the function succeeds, its result is returned as a successful AsyncMaybe.
     *
     * @param supplier the function to apply
     * @return A successful AsyncMaybe wrapping the return value if the function is executed and succeeds. An unsuccessful
     * AsyncMaybe otherwise.
     */
    public <R> AsyncMaybe<R> then(ThrowingSupplier<R> supplier) {
        requireNonNull(supplier);
        return AsyncMaybe.fromCompletableFuture(future.thenApplyAsync(v -> unchecked(supplier).get(), ASYNC_MAYBE_POOL));
    }

    /**
     * Apply a void function if this {@link AsyncMaybe} is in a "successful" state. Pass through the exception in case it is
     * in an "unsuccessful" state. The function does not get the value of the AsyncMaybe passed as parameter, which is useful
     * e.g. for chaining AsyncMaybe&lt;Void&gt;. If the function succeeds, a successful empty AsyncMaybe&lt;Void&gt; is returned.
     *
     * @param runnable the function to apply
     * @return A successful AsyncMaybe&lt;Void&gt; it the function is executed and succeeds. An unsuccessful AsyncMaybe otherwise.
     */
    public AsyncMaybe<Void> then(ThrowingRunnable runnable) {
        requireNonNull(runnable);
        return AsyncMaybe.fromVoidCompletableFuture(future.thenRunAsync(unchecked(runnable), ASYNC_MAYBE_POOL));
    }

    /**
     * Apply a function if this {@link AsyncMaybe} is in a "unsuccessful" state. The function gets passed the exception
     * wrapped in this maybe. If it returns successfully, it's result is wrapped in a "successful" AsyncMaybe. If it
     * throws, the exception is returned wrapped in an "unsuccessful" AsyncMaybe.
     *
     * @param function the recovery function to apply
     * @return A successful AsyncMaybe the new value if the function returns. An unsuccessful AsyncMaybe if it throws.
     */
    public AsyncMaybe<T> recover(ThrowingFunction<Throwable, T> function) {
        requireNonNull(function);
        return AsyncMaybe.fromCompletableFuture(future.handleAsync((value, exception) -> {
            if (exception != null) {
                return unchecked(function).apply(exception);
            }
            return value;
        }, ASYNC_MAYBE_POOL));
    }

    /**
     * Apply a function if this {@link AsyncMaybe} is in a "unsuccessful" state. The function gets passed the exception
     * wrapped in this AsyncMaybe. If it returns successfully, a "successful" void AsyncMaybe is returned. If it
     * throws, the exception is returned wrapped in an "unsuccessful" AsyncMaybe.
     *
     * @param consumer the recovery handler to apply
     * @return A successful void AsyncMaybe the new value if the handler returns. An unsuccessful AsyncMaybe if it throws.
     */
    public AsyncMaybe<Void> recover(ThrowingConsumer<Throwable> consumer) {
        requireNonNull(consumer);
        return AsyncMaybe.fromVoidCompletableFuture(future.handleAsync((value, exception) -> {
            if (exception != null) {
                unchecked(consumer).accept(exception);
            }
            return null;
        }, ASYNC_MAYBE_POOL));
    }

    /**
     * TODO andrea
     *
     * @return
     */
    public Maybe<T> toMaybeBlocking() {
        return maybeGenerator.apply(future::join);
    }

    /**
     * TODO andrea
     *
     * @param timeout
     * @return
     */
    public Maybe<T> toMaybeBlocking(Duration timeout) {
        return maybeGenerator.apply(() -> future.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * TODO andrea
     *
     * @return
     */
    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

}
