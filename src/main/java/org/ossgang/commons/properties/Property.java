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

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Transition;

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

    /**
     * Sets the property to the given value, notifying all observers and returning the previous value. The new value
     * must not be null.
     * 
     * @param value the new value
     * @throws NullPointerException on a null value
     */
    default T getAndSet(T value) {
        requireNonNull(value, "new value must not be null.");
        return update(old -> value).oldValue();
    }

    /**
     * Atomically updates the current value with the results of applying the given function, returning the updated
     * value. The updated value is dispatched to all observers. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     * 
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @throws NullPointerException if the updateFunction itself the result of it is {@code null}.
     */
    default T updateAndGet(UnaryOperator<T> updateFunction) {
        return update(updateFunction).newValue();
    }

    /**
     * Atomically updates the current value with the results of applying the given function, returning the previous
     * value. The updated value is dispatched to all observers. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     * 
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @throws NullPointerException if the updateFunction itself the result of it is {@code null}.
     */
    default T getAndUpdate(UnaryOperator<T> updateFunction) {
        return update(updateFunction).oldValue();
    }

    /**
     * Atomically updates the current value with the results of applying the given function, returning both, the
     * previous and the updated value. The updated value is dispatched to all observers. The function should be
     * side-effect-free, since it may be re-applied when attempted updates fail due to contention among threads.
     * 
     * @param updateFunction a side-effect-free function
     * @return a transition object, containing both, the previous and the updated value
     * @throws NullPointerException if the updateFunction itself the result of it is {@code null}.
     */
    default Transition<T> update(UnaryOperator<T> updateFunction) {
        requireNonNull(updateFunction, "updateFunction must not be null.");
        return accumulate(null, (old, update) -> updateFunction.apply(old));
    }

    /**
     * Atomically updates the current value with the results of applying the given function to the current and given
     * values, returning the updated value. The updated value is dispatched to all observers. The function should be
     * side-effect-free, since it may be re-applied when
     * attempted updates fail due to contention among threads. The function is applied with the current value as its
     * first argument, and the given update as the second argument.
     * 
     * @param the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @throws NullPointerException if the accumulatorFunction itself the result of it is {@code null}.
     */
    default T accumulateAndGet(T x, BinaryOperator<T> accumulatorFunction) {
        return accumulate(x, accumulatorFunction).newValue();
    }

    /**
     * Atomically updates the current value with the results of applying the given function to the current and given
     * values, returning the previous value. The updated value is dispatched to all observers. The function should be
     * side-effect-free, since it may be re-applied when attempted updates fail due to contention among threads. The
     * function is applied with the current value as its first argument, and the given update as the second argument.
     * 
     * @param the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @throws NullPointerException if the accumulatorFunction itself the result of it is {@code null}.
     */
    default T getAndAccumulate(T x, BinaryOperator<T> accumulatorFunction) {
        return accumulate(x, accumulatorFunction).oldValue();
    }

    /**
     * Atomically updates the current value with the results of applying the given function to the current and given
     * values, returning both, the previous and the updated value. The updated value is dispatched to all observers. The
     * function should be side-effect-free, since it may be re-applied when attempted updates fail due to contention
     * among threads. The
     * function is applied with the current value as its first argument, and the given update as the second argument.
     * 
     * @param the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return a transition object, containing both, the previous and the updated value
     * @throws NullPointerException if the accumulatorFunction itself the result of it is {@code null}.
     */
    Transition<T> accumulate(T x, BinaryOperator<T> accumulatorFunction);

}
