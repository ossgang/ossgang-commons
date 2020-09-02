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
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;
import static org.ossgang.commons.utils.Uncheckeds.*;

/**
 * This utility class implements the concept of a "Maybe" or "Try" {@link Optional} that can be resolved at some point
 * in the future. It allows to run code asynchronously and then fetch back or react on the result.
 * In order to fetch the result, it is possible to create a {@link Maybe} in a blocking way. Otherwise, reacting on the
 * completion can be achieved via, e.g., {@link #whenValue(ThrowingConsumer)} or {@link #map(ThrowingFunction)}.
 * *
 *
 * @param <T> the type to carry
 */
public class AsyncMaybe<T> {

    private static final ExecutorService ASYNC_MAYBE_POOL = newCachedThreadPool(daemonThreadFactoryWithPrefix("ossgang-AsyncMaybe-executor"));
    private static final String NULL_VALUE_MSG = "AsyncMaybe cannot contain a null value";

    private final CompletableFuture<T> future;
    private final boolean isVoid;
    private final Function<ThrowingSupplier<T>, Maybe<T>> maybeGenerator;

    private AsyncMaybe(CompletableFuture<T> future, boolean isVoid, Function<ThrowingSupplier<T>, Maybe<T>> maybeGenerator) {
        this.future = future;
        this.isVoid = isVoid;
        this.maybeGenerator = maybeGenerator;
    }

    public static <T> AsyncMaybe<T> fromCompletableFuture(CompletableFuture<T> future) {
        return new AsyncMaybe<>(future, false, Maybe::attempt);
    }

    public static AsyncMaybe<Void> fromVoidCompletableFuture(CompletableFuture<Void> future) {
        return new AsyncMaybe<>(future, true, valueSupplier -> Maybe.attempt((ThrowingRunnable) future::get));
    }

    /**
     * Construct an {@link AsyncMaybe} from the execution of a runnable. The type of the {@link AsyncMaybe} will be Void.
     *
     * @param runnable the runnable to run
     * @return An {@link AsyncMaybe} wrapping the execution of the provided runnable
     */
    public static AsyncMaybe<Void> attemptAsync(ThrowingRunnable runnable) {
        return AsyncMaybe.fromVoidCompletableFuture(CompletableFuture.runAsync(uncheckedRunnable(runnable), ASYNC_MAYBE_POOL));
    }

    /**
     * Construct an {@link AsyncMaybe} from the execution of a supplier.
     *
     * @param supplier the supplier to use
     * @param <T>      the type of the {@link AsyncMaybe}
     * @return An {@link AsyncMaybe} wrapping the execution of the provided supplier
     */
    public static <T> AsyncMaybe<T> attemptAsync(ThrowingSupplier<T> supplier) {
        return AsyncMaybe.fromCompletableFuture(CompletableFuture.supplyAsync(() -> requireNonNull(uncheckedSupplier(supplier).get(), NULL_VALUE_MSG), ASYNC_MAYBE_POOL));
    }

    /**
     * Construct an already "resolved" (without asynchronous calls) "successful" {@link AsyncMaybe} containing a value.
     *
     * @param <T>   the type to carry
     * @param value the value to wrap
     * @return the successful {@link AsyncMaybe} object
     * @throws NullPointerException if the value is null
     */
    public static <T> AsyncMaybe<T> ofValue(T value) {
        return AsyncMaybe.fromCompletableFuture(CompletableFuture.completedFuture(requireNonNull(value, NULL_VALUE_MSG)));
    }

    /**
     * Construct an already "resolved" (without asynchronous calls) "successful" {@link AsyncMaybe} of Void containing
     * a null value. This special {@link AsyncMaybe} objects represent a successful execution with no result (e.g. a void function).
     * Note that ONLY {@link AsyncMaybe} of Void is allowed to carry a null value.
     *
     * @return the successful {@link AsyncMaybe} of Void object
     */
    public static AsyncMaybe<Void> ofVoid() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return AsyncMaybe.fromVoidCompletableFuture(future);
    }


    /**
     * Construct an already "resolved" (without asynchronous calls) "unsuccessful" {@link Maybe} containing an exception.
     *
     * @param <T>       the type to carry
     * @param exception the exception to wrap
     * @return the unsuccessful {@link AsyncMaybe} object
     * @throws NullPointerException if the exception is null
     */
    public static <T> AsyncMaybe<T> ofException(Throwable exception) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(requireNonNull(exception, "AsyncMaybe cannot have a null exception"));
        return AsyncMaybe.fromCompletableFuture(future);
    }

    /**
     * Handles the case where the {@link CompletableFuture} can be of Void or T depending on the {@link #isVoid} flag.
     *
     * @param future to be transformed
     * @return the corresponding {@link AsyncMaybe}
     */
    private AsyncMaybe<T> asyncMaybeFromPotentiallyVoid(CompletableFuture<T> future) {
        if (isVoid) {
            try {
                @SuppressWarnings("unchecked")
                AsyncMaybe<T> nextMaybe = (AsyncMaybe<T>) AsyncMaybe.fromVoidCompletableFuture((CompletableFuture<Void>) future);
                return nextMaybe;
            } catch (Exception e) {
                throw new IllegalStateException("Cannot make CompletableFuture Void. isVoid flag inconsistent !!");
            }
        }
        return AsyncMaybe.fromCompletableFuture(future);
    }

    /**
     * Apply the consumer with the value that is contained in the, "successful", "resolved" state of this {@link AsyncMaybe}.
     * The consumer will be called only when the asynchronous calculations of the result of this {@link AsyncMaybe} are done.
     * A new {@link AsyncMaybe} is returned with the same result of this {@link AsyncMaybe} (be it an exception or a value).
     * If the provided consumer throws an exception, the returned {@link AsyncMaybe} will contain the newly thrown exception.
     *
     * @param consumer the consumer to run
     * @return a new {@link AsyncMaybe} with the same result as this {@link AsyncMaybe} or with an exception if one is thrown in the provided consumer
     */
    public AsyncMaybe<T> whenValue(ThrowingConsumer<T> consumer) {
        return asyncMaybeFromPotentiallyVoid(future.whenCompleteAsync((value, exception) -> {
            if (exception == null) {
                uncheckedConsumer(consumer).accept(value);
            }
        }, ASYNC_MAYBE_POOL));
    }

    /**
     * Apply the consumer on a {@link Maybe} representing the "resolved" state of this {@link AsyncMaybe}. The consumer
     * will be called only when the asynchronous calculations of the result of this {@link AsyncMaybe} are done.
     * A new {@link AsyncMaybe} is returned with the same result of this {@link AsyncMaybe} (be it an exception or a value).
     * If the provided consumer throws an exception, the returned {@link AsyncMaybe} will contain the newly thrown exception.
     *
     * @param consumer the consumer to run
     * @return a new {@link AsyncMaybe} with the same result as this {@link AsyncMaybe} or with an exception if one thrown in the provided consumer
     */
    public AsyncMaybe<T> whenComplete(ThrowingConsumer<Maybe<T>> consumer) {
        return asyncMaybeFromPotentiallyVoid(future.whenCompleteAsync((value, exception) -> {
            if (exception != null) {
                uncheckedConsumer(consumer).accept(Maybe.ofException(exception));
            } else {
                uncheckedConsumer(consumer).accept(maybeGenerator.apply(() -> value));
            }
        }, ASYNC_MAYBE_POOL));
    }

    /**
     * Apply the consumer with the value or exception that is contained in the "resolved" state of this {@link AsyncMaybe}.
     * The consumer will be called only when the asynchronous calculations of the result of this {@link AsyncMaybe} are done.
     * A new {@link AsyncMaybe} is returned with the same result of this {@link AsyncMaybe} (be it an exception or a value).
     * If the provided consumer throws an exception, the returned {@link AsyncMaybe} will contain the newly thrown exception.
     *
     * @param consumer the consumer to run
     * @return a new {@link AsyncMaybe} with the same result as this {@link AsyncMaybe} or with an exception if one is thrown in the provided consumer
     */
    public AsyncMaybe<T> whenComplete(BiConsumer<T, Throwable> consumer) {
        return asyncMaybeFromPotentiallyVoid(future.whenCompleteAsync(consumer, ASYNC_MAYBE_POOL));
    }

    /**
     * Apply the consumer with the exception that is contained in the, "unsuccessful",  "resolved" state of this {@link AsyncMaybe}.
     * The consumer will be called only when the asynchronous calculations of the result of this {@link AsyncMaybe} are done.
     * A new {@link AsyncMaybe} is returned with the same result of this {@link AsyncMaybe} (be it an exception or a value).
     * If the provided consumer throws an exception, the returned {@link AsyncMaybe} will contain the newly thrown exception.
     *
     * @param consumer the consumer to run
     * @return a new {@link AsyncMaybe} with the same result as this {@link AsyncMaybe} or with an exception if one is thrown in the provided consumer
     */
    public AsyncMaybe<T> whenException(ThrowingConsumer<Throwable> consumer) {
        return asyncMaybeFromPotentiallyVoid(future.whenCompleteAsync((value, exception) -> {
            if (exception != null) {
                uncheckedConsumer(consumer).accept(exception);
            }
        }, ASYNC_MAYBE_POOL));
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
        return AsyncMaybe.fromCompletableFuture(future.thenApplyAsync(v -> requireNonNull(uncheckedFunction(function).apply(v), NULL_VALUE_MSG), ASYNC_MAYBE_POOL));
    }

    /**
     * Apply a void function if this {@link AsyncMaybe} is in a "successful" state. Pass through the exception in case it is
     * in an "unsuccessful" state. If the transformation function throws, the exception is returned wrapped in an
     * "unsuccessful" AsyncMaybe. If it succeeds, return an empty AsyncMaybe&lt;Void&gt;.
     *
     * @param consumer the function to apply
     * @return A successful AsyncMaybe&lt;Void&gt; if the function is executed and succeeds. An unsuccessful AsyncMaybe otherwise.
     */
    public AsyncMaybe<Void> then(ThrowingConsumer<T> consumer) {
        requireNonNull(consumer);
        return AsyncMaybe.fromVoidCompletableFuture(future.thenAcceptAsync(uncheckedConsumer(consumer), ASYNC_MAYBE_POOL));
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
        return AsyncMaybe.fromCompletableFuture(future.thenApplyAsync(v -> requireNonNull(uncheckedSupplier(supplier).get(), NULL_VALUE_MSG), ASYNC_MAYBE_POOL));
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
        return AsyncMaybe.fromVoidCompletableFuture(future.thenRunAsync(uncheckedRunnable(runnable), ASYNC_MAYBE_POOL));
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
                return requireNonNull(uncheckedFunction(function).apply(exception), NULL_VALUE_MSG);
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
                uncheckedConsumer(consumer).accept(exception);
            }
            return null;
        }, ASYNC_MAYBE_POOL));
    }

    /**
     * Transform this {@link AsyncMaybe} into a {@link Maybe} in a blocking way, being the {@link Maybe} a container for
     * an already resolved value. This method will block the caller thread until this {@link AsyncMaybe} is completed.
     * Any exception produced during the waiting (e.g. {@link InterruptedException}) will result in an "unsuccessful"
     * {@link Maybe}.
     *
     * @return the {@link Maybe} representing the completed state of this {@link AsyncMaybe}
     */
    public Maybe<T> toMaybeBlocking() {
        return maybeGenerator.apply(future::join);
    }

    /**
     * Transform this {@link AsyncMaybe} into a {@link Maybe} in a blocking way, being the {@link Maybe} a container for
     * an already resolved value. This method will block the caller thread until this {@link AsyncMaybe} is completed.
     * Any exception produced during the waiting (e.g. {@link InterruptedException}) will result in an "unsuccessful"
     * {@link Maybe}.
     * The specified timeout is used to control for how long this method will block the caller thread. A {@link java.util.concurrent.TimeoutException}
     * will be thrown in case the timeout is reached and this {@link AsyncMaybe} not completed yet. This exception will
     * be in the returned, "unsuccessful", {@link Maybe}.
     * This means that this method can be called more then once if you know the value can be present at some point in the
     * future.
     *
     * @param timeout the maximum time this method will block the caller thread
     * @return the {@link Maybe} representing the completed state of this {@link AsyncMaybe}
     */
    public Maybe<T> toMaybeBlocking(Duration timeout) {
        return maybeGenerator.apply(() -> future.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * Return the underlying {@link CompletableFuture} that this {@link AsyncMaybe} abstracts.
     *
     * @return the underlying {@link CompletableFuture} that this {@link AsyncMaybe} abstracts
     */
    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

}
