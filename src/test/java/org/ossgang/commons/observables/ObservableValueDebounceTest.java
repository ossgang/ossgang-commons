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

package org.ossgang.commons.observables;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;
import org.ossgang.commons.monads.Maybe;
import org.ossgang.commons.observables.testing.TestObserver;

public class ObservableValueDebounceTest {

    @Test
    public void testDebounce_dispatchIsSkipped_ifInsideTimeWindow() {
        Dispatcher<Object> dispatcher = Observables.dispatcher();

        TestObserver<Object> observer = new TestObserver<>();
        Observables.debounce(dispatcher, Duration.ofSeconds(1)).subscribe(observer);

        Thread.yield();
        dispatcher.dispatchValue("a");
        Maybe.attempt(() -> Thread.sleep(1500));
        dispatcher.dispatchValue("b");
        Maybe.attempt(() -> Thread.sleep(20));
        dispatcher.dispatchValue("c");
        Maybe.attempt(() -> Thread.sleep(1500));
        dispatcher.dispatchValue("d");

        observer.awaitForValueCountToBe(3, Duration.ofSeconds(3));
        assertThat(observer.receivedValues()).containsExactly("a", "c", "d");
    }

    @Test
    public void testDebounce_dispatchException_worksRegardlessOfTimeWindow() {
        Dispatcher<Object> dispatcher = Observables.dispatcher();

        TestObserver<Object> observer = new TestObserver<>();
        Observables.debounce(dispatcher, Duration.ofSeconds(1)).subscribe(observer);

        RuntimeException exceptionA = new RuntimeException("a");
        RuntimeException exceptionB = new RuntimeException("b");
        RuntimeException exceptionC = new RuntimeException("c");

        Thread.yield();
        dispatcher.dispatchException(exceptionA);
        Maybe.attempt(() -> Thread.sleep(20));
        dispatcher.dispatchException(exceptionB);
        Maybe.attempt(() -> Thread.sleep(20));
        dispatcher.dispatchException(exceptionC);

        observer.awaitForExceptionCountToBe(3, Duration.ofSeconds(3));

        assertThat(observer.receivedValues()).isEmpty();
        assertThat(observer.receivedExceptions()).containsExactly(exceptionA, exceptionB, exceptionC);
    }

}
