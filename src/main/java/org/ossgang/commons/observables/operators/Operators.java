/*
 *
 * This file is part of ossgang-commons.
 *
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
 */

package org.ossgang.commons.observables.operators;

import static org.ossgang.commons.observables.operators.DerivedObservableValue.derive;

import java.util.Optional;

import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;

public final class Operators {

    private Operators() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * @see org.ossgang.commons.observables.Observables#observableValueOf(Observable)
     */
    public static <T> ObservableValue<T> observableValueOf(Observable<T> observable) {
        if (observable instanceof ObservableValue) {
            return (ObservableValue<T>) observable;
        }
        return derive(observable, Optional::of);
    }

}
