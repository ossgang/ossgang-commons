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

package org.ossgang.commons.observable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An observable of type T which has an actual value.
 *
 * @param <T> the type of the observable
 */
public interface ObservableValue<T> extends Observable<T> {

    /**
     * Retrieve the actual value of this ObservableValue.
     * If not such value exists (this ObservableValue is in an uninitialized state), null is returned.
     *
     * @return the actual value.
     */
    T get();

    /**
     * Create a derived observable value applying a mapping function to each value.
     *
     * @param mapper the mapper to apply
     * @param <D> the destination type
     * @return the derived observable
     */
    @Override
    default <D> ObservableValue<D> map(Function<T, D> mapper) {
        return derive(mapper.andThen(Optional::of));
    }

    /**
     * Create a derived observable value applying a filtering function to the updates of this one. Values which do not
     * match the provided predicate are discarded.
     *
     * @param filter the filter to apply
     * @return the derived observable
     */
    @Override
    default ObservableValue<T> filter(Predicate<T> filter) {
        return derive(v -> Optional.of(v).filter(filter));
    }

    /**
     * Creates a derived observable value, using the given mapper. If the mapper returns an optional containing a value,
     * then values are emitted downstream, if the returned optional is empty, values will be filtered out.
     *
     * @param the mapper to be used
     * @return the derived observable
     */
    @Override
    default <D> ObservableValue<D> derive(Function<T, Optional<D>> mapper) {
        return DerivedObservableValue.derive(this, mapper);
    }

}
