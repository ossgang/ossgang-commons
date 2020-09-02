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

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.ossgang.commons.utils.Uncheckeds.asUnchecked;

/**
 * This utility class implements the concept of a "Maybe" or "Try" {@link Optional}. A Maybe&lt;T&gt; either carries a
 * T or an exception that occurred when producing it.
 *
 * @author michi
 * @param <T> the type to carry.
 */
public class Maybe<T> {
    private final T value;
    private final Throwable exception;

    private Maybe() {
        /* only allowed for successful Maybe<Void>! Verified Below! */
        this.value = null;
        this.exception = null;
    }

    private Maybe(T value) {
        this.value = requireNonNull(value);
        this.exception = null;
    }

    private Maybe(Throwable exception) {
        this.value = null;
        this.exception = requireNonNull(exception);
    }

    /**
     * Construct an "unsuccessful" {@link Maybe} containing an exception.
     *
     * @param <T> the type to carry.
     * @param exception the exception to wrap
     * @return the unsuccessful Maybe object
     * @throws NullPointerException if the exception is null
     */
    public static <T> Maybe<T> ofException(Throwable exception) {
        return new Maybe<>(exception);
    }

    /**
     * Construct a "successful" {@link Maybe} containing a value.
     *
     * @param <T> the type to carry.
     * @param value the value to wrap
     * @return the successful Maybe object
     * @throws NullPointerException if the value is null
     */
    public static <T> Maybe<T> ofValue(T value) {
        return new Maybe<>(value);
    }

    /**
     * Construct a "successful" {@link Maybe}&lt;Void&gt; containing a null value. This special Maybe objects represents
     * a
     * successful execution with no result (e.g. a void function). Note that ONLY Maybe&lt;Void&gt; is allowed to carry
     * a null
     * value.
     *
     * @return the successful Maybe&lt;Void&gt; object
     */
    public static Maybe<Void> ofVoid() {
        return new Maybe<>();
    }

    /**
     * Returns true, if this Maybe is in a successful state, i.e. it does not contain an exception. Usually this means,
     * the Maybe has a valid (non-null) value. So usually this is equivalent to {@code #hasValue()}. The only exception
     * to this is a "successful"
     * Maybe&lt;Void&gt; which does not have a valid value (and thus the {@link #value} would throw even if the Maybe is
     * in a successful state.
     * <p>
     * This is equivalent to {@code !hasException()}
     *
     * @return true if the Maybe is in "successful" state, false if not
     * @see #hasException()
     */
    public boolean isSuccessful() {
        return !hasException();
    }

    /**
     * Returns true if this Maybe is in an "unsuccessful" state, i.e. containing an exception.
     *
     * @return true if an exception is present, false otherwise
     */
    public boolean hasException() {
        return exception != null;
    }

    /**
     * Returns true, if this Maybe is in a successful state, if it has a valid non-null value. Usually, a Maybe in a
     * successful state always has a value. The only exception to this is a Maybe&lt;Void&lt; which even in
     * successful state does not have a value. So this method would return {@code false}. Use the method
     * {@link #isSuccessful()} in such a case.
     *
     * @return true if the the value is available, false if not
     * @see #hasException()
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Retrieve the value stored in this {@link Maybe}. If an exception is stored, this method will re-throw it wrapped
     * in a {@link RuntimeException}. If no value is contained, then this method will throw. Therefore, always check
     * before if a value is stored by using the {@link #hasValue()} method.
     *
     * @return the value
     * @throws RuntimeException if this Maybe objects contains an exception
     * @throws NoSuchElementException if the value is {@code null} (can only happen for a successful Maybe&lt;Void&gt;)
     * @see #hasValue()
     */
    public T value() {
        throwOnException();
        return optionalValue().get();
    }

    /**
     * Retrieves the exception stored in this Maybe. If no exception is stored, then this method will throw. Therefore,
     * always check before if an exception is there using the {@link #hasException()} method.
     *
     * @return the exception stored in this Maybe
     * @throws NoSuchElementException if the Maybe does not contain an exception
     * @see #hasException()
     */
    public Throwable exception() {
        return optionalException().get();
    }

    /**
     * Retrieve the value stored in this {@link Maybe} as an {@link Optional}. If an exception is stored in this Maybe,
     * the returned optional will be empty. For a "successful" {@link Maybe}&lt;Void&gt;, it will also be an empty
     * optional.
     *
     * @return the value as an optional (empty in case of a successful void or an unsuccessful Maybe)
     */
    public Optional<T> optionalValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns an optional containing the exception, if any. If no exception is contained, then an empty optional
     * is returned.
     *
     * @return the contained exception, or Optional.empty() if none occurred.
     */
    public Optional<Throwable> optionalException() {
        return Optional.ofNullable(exception);
    }

    /**
     * Throw the contained exception, wrapped in a {@link RuntimeException}, if any. If called on a "successful" Maybe,
     * this method does nothing.
     */
    public void throwOnException() {
        if (exception != null) {
            throw asUnchecked(exception);
        }
    }

    /**
     * Construct a {@link Maybe} from the execution of a function.
     *
     * @param supplier the function to run.
     * @return A {@link Maybe} object containing either the return value of the function, or an exception of one
     *         occurred.
     */
    public static <T> Maybe<T> attempt(ThrowingSupplier<T> supplier) {
        requireNonNull(supplier);
        try {
            return Maybe.ofValue(supplier.get());
        } catch (Exception e) {
            return Maybe.ofException(e);
        }
    }

    /**
     * Construct a {@link Maybe}&lt;Void&gt; from the execution of a void function.
     *
     * @param runnable the function to run.
     * @return A successful {@link Maybe}&lt;Void&gt; if the function run successfully, or an exception of one occurred.
     */
    public static Maybe<Void> attempt(ThrowingRunnable runnable) {
        requireNonNull(runnable);
        try {
            runnable.run();
            return Maybe.ofVoid();
        } catch (Exception e) {
            return Maybe.ofException(e);
        }
    }

    /**
     * Apply a transformation function if this {@link Maybe} is in a "successful" state. Pass through the exception in
     * case it is in an "unsuccessful" state. If the transformation function throws, the exception is returned wrapped
     * in an "unsuccessful" Maybe.
     *
     * @param function the function to apply
     * @return A successful Maybe the new value if the transformation succeeds. An unsuccessful Maybe otherwise.
     */
    public <R> Maybe<R> map(ThrowingFunction<T, R> function) {
        requireNonNull(function);
        if (exception != null) {
            return Maybe.ofException(exception);
        }
        return attempt(() -> function.apply(value));
    }

    /**
     * Apply a transformation function if this {@link Maybe} is in a "successful" state. Pass through the exception in
     * case it is in an "unsuccessful" state. The transformation function should return a {@link Maybe}, which will be
     * returned. If the transformation function throws, the exception is returned wrapped in an "unsuccessful" Maybe.
     *
     * @param function the function to apply
     * @return A successful Maybe the new value if the transformation succeeds. An unsuccessful Maybe otherwise.
     */
    public <R> Maybe<R> flatMap(ThrowingFunction<T, Maybe<R>> function) {
        requireNonNull(function);
        if (exception != null) {
            return Maybe.ofException(exception);
        }
        try {
            return function.apply(value);
        } catch (Exception e) {
            return Maybe.ofException(exception);
        }
    }

    /**
     * Apply a void function if this {@link Maybe} is in a "successful" state. Pass through the exception in case it is
     * in an "unsuccessful" state. If the transformation function throws, the exception is returned wrapped in an
     * "unsuccessful" Maybe. If it succeeds, return an empty Maybe&lt;Void&gt;.
     *
     * @param function the function to apply
     * @return A successful Maybe&lt;Void&gt; if the function is executed and succeeds. An unsuccessful Maybe otherwise.
     */
    public Maybe<Void> then(ThrowingConsumer<T> function) {
        requireNonNull(function);
        if (exception != null) {
            return Maybe.ofException(exception);
        }
        return attempt(() -> function.accept(value));
    }

    /**
     * Apply a function if this {@link Maybe} is in a "successful" state. Pass through the exception in case it is in an
     * "unsuccessful" state. The function does not get the value of the Maybe passed as parameter, which is useful e.g.
     * for chaining Maybe&lt;Void&gt;. If the function succeeds, its result is returned as a successful Maybe.
     *
     * @param supplier the function to apply
     * @return A successful Maybe wrapping the return value if the function is executed and succeeds. An unsuccessful
     *         Maybe otherwise.
     */
    public <R> Maybe<R> then(ThrowingSupplier<R> supplier) {
        requireNonNull(supplier);
        if (exception != null) {
            return Maybe.ofException(exception);
        }
        return attempt(() -> supplier.get());
    }

    /**
     * Apply a void function if this {@link Maybe} is in a "successful" state. Pass through the exception in case it is
     * in an "unsuccessful" state. The function does not get the value of the Maybe passed as parameter, which is useful
     * e.g. for chaining Maybe&lt;Void&gt;. If the function succeeds, a successful empty Maybe&lt;Void&gt; is returned.
     *
     * @param runnable the function to apply
     * @return A successful Maybe&lt;Void&gt; it the function is executed and succeeds. An unsuccessful Maybe otherwise.
     */
    public Maybe<Void> then(ThrowingRunnable runnable) {
        requireNonNull(runnable);
        if (exception != null) {
            return Maybe.ofException(exception);
        }
        return attempt(() -> runnable.run());
    }

    /**
     * Apply a function if this {@link Maybe} is in a "unsuccessful" state. The function gets passed the exception
     * wrapped in this maybe. If it returns successfully, it's result is wrapped in a "successful" Maybe. If it
     * throws, the exception is returned wrapped in an "unsuccessful" Maybe.
     *
     * @param function the recovery function to apply
     * @return A successful Maybe the new value if the function returns. An unsuccessful Maybe if it throws.
     */
    public Maybe<T> recover(ThrowingFunction<Throwable, T> function) {
        requireNonNull(function);
        if (exception == null) {
            return Maybe.ofValue(value);
        }
        return attempt(() -> function.apply(exception));
    }

    /**
     * Apply a function if this {@link Maybe} is in a "unsuccessful" state. The function gets passed the exception
     * wrapped in this maybe. If it returns successfully, a "successful" void Maybe is returned. If it
     * throws, the exception is returned wrapped in an "unsuccessful" Maybe.
     *
     * @param consumer the recovery handler to apply
     * @return A successful void Maybe the new value if the handler returns. An unsuccessful Maybe if it throws.
     */
    public Maybe<Void> recover(ThrowingConsumer<Throwable> consumer) {
        requireNonNull(consumer);
        if (exception == null) {
            return Maybe.ofVoid();
        }
        return attempt(() -> consumer.accept(exception));
    }

    /**
     * Pass the exception to the given consumer if this {@link Maybe} is in a "unsuccessful" state. If the consumer
     * throws when called, then the resulting exception is
     * escalated.
     *
     * @param consumer the exception handler to apply
     * @return this
     * @throws NullPointerException in case the consumer is {@code null}.
     * @throws RuntimeException in case the consumer throws any exception
     */
    public Maybe<T> ifException(Consumer<Throwable> consumer) {
        requireNonNull(consumer, "The consumer must not be null.");
        if (hasException()) {
            consumer.accept(exception);
        }
        return this;
    }

    /**
     * Pass the value to the given consumer if this {@link Maybe} is in a "successful" state. If the consumer
     * throws when called, then the resulting exception is escalated.
     *
     * @param consumer the handler to which to pass on the value.
     * @return this
     * @throws NullPointerException in case the consumer is {@code null}.
     * @throws RuntimeException in case the consumer throws any exception
     */
    public Maybe<T> ifValue(Consumer<T> consumer) {
        requireNonNull(consumer, "The consumer must not be null.");
        if (hasValue()) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Call the given runnable if the Maybe is in a successful state. No value is passed here, as e.g. a successful
     * Maybe&lt;Void&gt; does not have a value. If you rely on the value, then consider using the {@link #ifValue(Consumer)}
     * callback. If the given runnable throws when called, then the resulting exception is escalated.
     *
     * @param runnable the handler to run if this monad is in a successful state
     * @return this
     * @throws NullPointerException in case the runnable is {@code null}.
     * @throws RuntimeException in case the runnable throws any exception
     */
    public Maybe<T> ifSuccessful(Runnable runnable) {
        requireNonNull(runnable, "The runnable must not be null.");
        if (isSuccessful()) {
            runnable.run();
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((exception == null) ? 0 : exception.hashCode());
        result = (prime * result) + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Maybe)) {
            return false;
        }
        Maybe<?> other = (Maybe<?>) obj;
        if (exception == null) {
            if (other.exception != null) {
                return false;
            }
        } else if (!exception.equals(other.exception)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (exception != null) {
            return "Maybe [exception=" + exception.getClass().getSimpleName() + "]\n" + exception;
        } else {
            return "Maybe [value=" + value + "]";
        }
    }

}
