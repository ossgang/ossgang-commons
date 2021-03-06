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

package org.ossgang.commons.observables;


import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.ossgang.commons.monads.Maybe;
import org.ossgang.commons.observables.operators.BlockingOperators;
import org.ossgang.commons.observables.operators.DerivedObservableValue;

/**
 * An stream of objects of type T, which can be subscribed to by interested consumers.
 *
 * @param <T> the type of objects
 */
public interface Observable<T> {
    /**
     * Subscribe for future updates of this observable. By default, the Observable will hold a strong reference
     * to the provided Observer (this can be overridden by the WEAK option).
     * The provided {@link SubscriptionOption}s are specific to the sub-type of observable. The available options are
     * typically exposed in the interface of the concrete sub-type, or any of its parent interfaces.
     * The returned {@link Subscription} object can be used to terminate the subscription at a later point by calling
     * {@link Subscription#unsubscribe()}. If the subscription will live for the lifetime of the application, it is
     * safe to discard this object.
     *
     * @param listener the consumer of updates
     * @param options the list of options
     * @return a Subscription object, which can be used to unsubscribe at a later point
     */
    Subscription subscribe(Observer<? super T> listener, SubscriptionOption... options);

    /**
     * Create a derived observable applying a mapping function to each value.
     *
     * @param mapper the mapper to apply
     * @param <D> the destination type
     * @return the derived observable
     */
    default <D> Observable<D> map(Function<T, D> mapper) {
        return derive(mapper.andThen(Optional::of));
    }

    /**
     * Create a derived observable applying a filtering function to the updates of this one. Values which do not
     * match the provided predicate are discarded.
     *
     * @param filter the filter to apply
     * @return the derived observable
     */
    default Observable<T> filter(Predicate<T> filter) {
        return derive(v -> Optional.of(v).filter(filter));
    }

    /**
     * Creates a derived observable, using the given mapper. If the mapper returns an optional containing a value, then
     * values are emitted downstream, if the returned optional is empty, values will be filtered out.
     *
     * @param mapper to be used
     * @return the derived observable
     */
    default <D> Observable<D> derive(Function<T, Optional<D>> mapper) {
        return DerivedObservableValue.derive(this, mapper);
    }

    /**
     * Blocks and awaits the next update on this observable (which can be either a value or an exception), and returns
     * it as a Maybe&lt;T&gt;. Note that while an exception-update on this observable will be wrapped in a Maybe, this
     * method throws an AwaitTimeoutException in case the timeout exceeds.
     *
     * @param timeout the timeout for the wait
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException if the timeout is exceeded
     * @return a Maybe&lt;T&gt; containing either the value or the exception from the next update on this observable
     */
    default Maybe<T> awaitNext(Duration timeout) {
        return BlockingOperators.awaitNext(this, timeout);
    }

    /**
     * Blocks and awaits the next value-update on this observable. Any exception-updates are ignored. If no value is
     * received until the timeout passes, this method throws an AwaitTimeoutException.
     *
     * @param timeout the timeout for the wait
     * @throws org.ossgang.commons.awaitables.exceptions.AwaitTimeoutException if the timeout is exceeded
     * @return the next value on this observable
     */
    default T awaitNextValue(Duration timeout) {
        return BlockingOperators.awaitNextValue(this, timeout);
    }
}
