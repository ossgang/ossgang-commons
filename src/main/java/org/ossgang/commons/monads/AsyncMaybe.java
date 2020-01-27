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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.ossgang.commons.utils.Exceptions.unchecked;

/**
 * TODO andrea
 *
 * @param <T>
 */
public class AsyncMaybe<T> {

    private final CompletableFuture<T> future;
    private final Function<Supplier<T>, Maybe<T>> maybeGenerator;

    protected AsyncMaybe(CompletableFuture<T> future, Function<Supplier<T>, Maybe<T>> maybeGenerator) {
        this.future = future;
        this.maybeGenerator = maybeGenerator;
    }

    public static <T> AsyncMaybe<T> fromCompletableFuture(CompletableFuture<T> future) {
        return new AsyncMaybe<>(future, valueSupplier -> Maybe.attempt(valueSupplier::get));
    }

    public static AsyncMaybe<Void> fromVoidCompletableFuture(CompletableFuture<Void> future) {
        return new AsyncMaybe<>(future, valueSupplier -> {
            try {
                valueSupplier.get();
                return Maybe.ofVoid();
            } catch (Exception e) {
                return Maybe.ofException(e);
            }
        });
    }

    /**
     * TODO andrea
     *
     * @param runnable
     * @return
     */
    public static AsyncMaybe<Void> attemptAsync(ThrowingRunnable runnable) {
        return AsyncMaybe.fromVoidCompletableFuture(CompletableFuture.runAsync(unchecked(runnable)));
    }

    /**
     * TODO andrea
     *
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> AsyncMaybe<T> attemptAsync(ThrowingSupplier<T> supplier) {
        return AsyncMaybe.fromCompletableFuture(CompletableFuture.supplyAsync(unchecked(supplier)));
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
    public AsyncMaybe<T> whenCompleted(ThrowingConsumer<T> consumer) {
        return AsyncMaybe.fromCompletableFuture(future.whenComplete((value, exception) -> {
            if (exception == null) {
                unchecked(consumer).accept(value);
            }
        }));
    }

    /**
     * TODO andrea + test
     *
     * @param consumer
     * @return
     */
    public AsyncMaybe<T> whenCompleted(BiConsumer<T, Throwable> consumer) {
        return AsyncMaybe.fromCompletableFuture(future.whenComplete(consumer));
    }

    /**
     * TODO andrea + test
     *
     * @param consumer
     * @return
     */
    public AsyncMaybe<T> whenCompletedExceptionally(ThrowingConsumer<Throwable> consumer) {
        CompletableFuture<T> whenComplete = future.whenComplete((value, exception) -> {
            if (exception != null) {
                unchecked(consumer).accept(exception);
            }
        });
        return AsyncMaybe.fromCompletableFuture(whenComplete);
    }

    /**
     * Apply a transformation function if this {@link Maybe} is in a "successful" state. Pass through the exception in
     * case it is in an "unsuccessful" state. If the transformation function throws, the exception is returned wrapped
     * in an "unsuccessful" Maybe.
     *
     * @param function the function to apply
     * @return A successful Maybe the new value if the transformation succeeds. An unsuccessful Maybe otherwise.
     */
    public <R> AsyncMaybe<R> map(ThrowingFunction<T, R> function) {
        requireNonNull(function);
        return AsyncMaybe.fromCompletableFuture(this.future.thenApply(unchecked(function)));
    }

    /**
     * Apply a void function if this {@link Maybe} is in a "successful" state. Pass through the exception in case it is
     * in an "unsuccessful" state. If the transformation function throws, the exception is returned wrapped in an
     * "unsuccessful" Maybe. If it succeeds, return an empty Maybe&lt;Void&gt;.
     *
     * @param function the function to apply
     * @return A successful Maybe&lt;Void&gt; if the function is executed and succeeds. An unsuccessful Maybe otherwise.
     */
    public AsyncMaybe<Void> map(ThrowingConsumer<T> function) {
        requireNonNull(function);
        return AsyncMaybe.fromVoidCompletableFuture(future.thenAccept(unchecked(function)));
    }

    /**
     * Apply a function if this {@link Maybe} is in a "successful" state. Pass through the exception in case it is in an
     * "unsuccessful" state. The function does not get the value of the Maybe passed as parameter, which is useful e.g.
     * for chaining Maybe&lt;Void&gt;. If the function succeeds, its result is returned as a successful Maybe.
     *
     * @param supplier the function to apply
     * @return A successful Maybe wrapping the return value if the function is executed and succeeds. An unsuccessful
     * Maybe otherwise.
     */
    public <R> AsyncMaybe<R> then(ThrowingSupplier<R> supplier) {
        requireNonNull(supplier);
        return AsyncMaybe.fromCompletableFuture(future.thenApply(v -> unchecked(supplier).get()));
    }

    /**
     * Apply a void function if this {@link Maybe} is in a "successful" state. Pass through the exception in case it is
     * in an "unsuccessful" state. The function does not get the value of the Maybe passed as parameter, which is useful
     * e.g. for chaining Maybe&lt;Void&gt;. If the function succeeds, a successful empty Maybe&lt;Void&gt; is returned.
     *
     * @param runnable the function to apply
     * @return A successful Maybe&lt;Void&gt; it the function is executed and succeeds. An unsuccessful Maybe otherwise.
     */
    public AsyncMaybe<Void> then(ThrowingRunnable runnable) {
        requireNonNull(runnable);
        return AsyncMaybe.fromVoidCompletableFuture(future.thenRun(unchecked(runnable)));
    }

    /**
     * Apply a function if this {@link Maybe} is in a "unsuccessful" state. The function gets passed the exception
     * wrapped in this maybe. If it returns successfully, it's result is wrapped in a "successful" Maybe. If it
     * throws, the exception is returned wrapped in an "unsuccessful" Maybe.
     *
     * @param function the recovery function to apply
     * @return A successful Maybe the new value if the function returns. An unsuccessful Maybe if it throws.
     */
    public AsyncMaybe<T> recover(ThrowingFunction<Throwable, T> function) {
        requireNonNull(function);
        return AsyncMaybe.fromCompletableFuture(future.handle((value, exception) -> {
            if (exception != null) {
                return unchecked(function).apply(exception);
            }
            return value;
        }));
    }

    /**
     * Apply a function if this {@link Maybe} is in a "unsuccessful" state. The function gets passed the exception
     * wrapped in this maybe. If it returns successfully, a "successful" void Maybe is returned. If it
     * throws, the exception is returned wrapped in an "unsuccessful" Maybe.
     *
     * @param consumer the recovery handler to apply
     * @return A successful void Maybe the new value if the handler returns. An unsuccessful Maybe if it throws.
     */
    public AsyncMaybe<Void> recover(ThrowingConsumer<Throwable> consumer) {
        requireNonNull(consumer);
        return AsyncMaybe.fromVoidCompletableFuture(future.handle((value, exception) -> {
            if (exception != null) {
                unchecked(consumer).accept(exception);
            }
            return null;
        }));
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
     * @return
     */
    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

    /**
     * TODO andrea: decide which criteria to use with hashcode and equals! (+ test)
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsyncMaybe<?> that = (AsyncMaybe<?>) o;
        return Objects.equals(future, that.future);
    }

    @Override
    public int hashCode() {
        return Objects.hash(future);
    }
}
