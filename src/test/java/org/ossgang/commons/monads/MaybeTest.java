// @formatter:off
/*******************************************************************************
 *
 * This file is part of ossgang-commons.
 *
 * Copyright (c) 2008-2018, CERN. All rights reserved.
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

import org.junit.Test;
import org.mockito.Mockito;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.ossgang.commons.monads.Maybe.attempt;

public class MaybeTest {

    private static class MaybeTestException1 extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private int throwException1(@SuppressWarnings("unused") Object value) {
        throw new MaybeTestException1();
    }

    private static class MaybeTestException2 extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private int throwException2(@SuppressWarnings("unused") Object value) {
        throw new MaybeTestException2();
    }

    @Test
    public void constructionSucceeds() {
        assertThat(Maybe.ofValue(42).hasException()).isFalse();
        assertThat(Maybe.ofVoid().hasException()).isFalse();
        assertThat(Maybe.ofException(new MaybeTestException1()).hasException()).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void constructionWithNullArgumentFails() {
        Maybe.ofValue(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructionWithNullExceptionFails() {
        Maybe.ofException(null);
    }

    @Test(expected = NullPointerException.class)
    public void mappingWithNullFunctionFails() {
        Maybe.ofValue("42").map((ThrowingFunction<String, String>) null);
    }

    @Test(expected = NullPointerException.class)
    public void attemptWithNullFunctionFails() {
        attempt((ThrowingSupplier<String>) null);
    }

    @Test(expected = NullPointerException.class)
    public void thenRunWithNullFunctionFails() {
        Maybe.ofValue("42").then(((ThrowingSupplier<String>) null));
    }

    @Test
    public void successfulChain() {
        assertThat(attempt(() -> 42).map(v -> 65 - v).value()).isEqualTo(23);
    }

    @Test
    public void successfulChainWithThen() {
        Maybe<String> chain = attempt(() -> Thread.sleep(0))//
                .then(() -> 42)//
                .map(v -> 21 - v)//
                .map((Integer v) -> assertThat(v).isEqualTo(-21))//
                .then(() -> "Success");
        assertThat(chain.value()).isEqualTo("Success");
    }

    @Test(expected = RuntimeException.class)
    public void failureMapping() {
        Maybe<Integer> maybe = attempt(() -> 42).map(this::throwException1).map(v -> 84 - v).map(this::throwException2);
        assertThat(maybe.hasException()).isTrue();
        assertThat(maybe.optionalException().get()).isInstanceOf(MaybeTestException1.class);
        maybe.value(); /* <- throws */
    }

    @Test
    public void transformToUncheckedException() {
        attempt(() -> Thread.sleep(0)).throwOnException();
    }

    @Test(expected = RuntimeException.class)
    public void failureTransformToUncheckedException() {
        attempt(() -> throwException1(42)).throwOnException();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void chainStopsOnceExceptionOccurs() throws Exception {
        ThrowingFunction<String, String> function1 = mock(ThrowingFunction.class);
        Mockito.when(function1.apply(anyString())).thenReturn("42");
        ThrowingFunction<Integer, String> function2 = mock(ThrowingFunction.class);
        Maybe<String> maybe = attempt(() -> "HelloWorld").map(function1).map(this::throwException2).map(function2);
        verify(function1, times(1)).apply("HelloWorld");
        verify(function2, never()).apply(anyInt());
        assertThat(maybe.hasException()).isTrue();
        assertThat(maybe.optionalException().get()).isInstanceOf(MaybeTestException2.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void recoverClearsException() throws Exception {
        ThrowingFunction<String, String> function1 = mock(ThrowingFunction.class);
        Mockito.when(function1.apply(anyString())).thenReturn("42");
        ThrowingFunction<Throwable, Integer> recoveryFunction = mock(ThrowingFunction.class);
        Mockito.when(recoveryFunction.apply(any(MaybeTestException2.class))).thenReturn(42);
        ThrowingFunction<Integer, String> function2 = mock(ThrowingFunction.class);
        Mockito.when(function2.apply(42)).thenReturn("OK");
        Maybe<String> maybe = attempt(() -> "HelloWorld").map(function1).map(this::throwException2)
                .recover(recoveryFunction).map(function2);
        verify(function1, times(1)).apply("HelloWorld");
        verify(recoveryFunction, times(1)).apply(any(MaybeTestException2.class));
        verify(function2, times(1)).apply(42);
        assertThat(maybe.hasException()).isFalse();
        assertThat(maybe.value()).isEqualTo("OK");
    }

    @Test
    public void checkHashCode() {
        assertThat(Maybe.ofValue("42").hashCode()).isEqualTo(Maybe.ofValue("42").hashCode());
        assertThat(Maybe.ofVoid().hashCode()).isEqualTo(Maybe.ofVoid().hashCode());
        IllegalArgumentException someException = new IllegalArgumentException();
        assertThat(Maybe.ofException(someException).hashCode()).isEqualTo(Maybe.ofException(someException).hashCode());
    }

    @Test
    public void checkEquals() {
        assertThat(Maybe.ofValue("42")).isEqualTo(Maybe.ofValue("42"));
        assertThat(Maybe.ofVoid()).isEqualTo(Maybe.ofVoid());
        IllegalArgumentException someException = new IllegalArgumentException();
        assertThat(Maybe.ofException(someException)).isEqualTo(Maybe.ofException(someException));

        assertThat(Maybe.ofValue("42")).isNotEqualTo(Maybe.ofVoid());
        assertThat(Maybe.ofVoid()).isNotEqualTo(Maybe.ofException(new MaybeTestException1()));
        assertThat(Maybe.ofException(new MaybeTestException1()))
                .isNotEqualTo(Maybe.ofException(new MaybeTestException2()));
    }

    @Test
    public void checkToString() {
        assertThat(Maybe.ofValue("HelloWorld").toString()).contains("HelloWorld");
        assertThat(Maybe.ofVoid().toString()).contains("null");
        assertThat(Maybe.ofException(new MaybeTestException1()).toString()).contains("MaybeTestException1");
    }

    @Test
    public void successfulVoidState() {
        Maybe<Void> m = Maybe.ofVoid();
        assertThat(m.isSuccessful()).isTrue();
        assertThat(m.hasValue()).isFalse();
        assertThat(m.hasException()).isFalse();
    }

    @Test(expected = NoSuchElementException.class)
    public void successfulVoidThrowsOnGettingException() {
        Maybe.ofVoid().exception();
    }

    @Test(expected = NoSuchElementException.class)
    public void successfulVoidThrowsOnGettingValue() {
        Maybe.ofVoid().value();
    }

    @Test
    public void successfulValueState() {
        Maybe<String> m = Maybe.ofValue("42");
        assertThat(m.hasException()).isFalse();
        assertThat(m.isSuccessful()).isTrue();
        assertThat(m.hasValue()).isTrue();
        assertThat(m.value()).isEqualTo("42");
    }

    @Test(expected = NoSuchElementException.class)
    public void successfulValueThrowsOnException() {
        Maybe.ofValue("42").exception();
    }

    @Test
    public void unsuccessfulCallsIfException() {
        Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
        Consumer<Void> valueConsumer = mock(Consumer.class);
        Runnable successful = mock(Runnable.class);

        Maybe<Void> m = Maybe.ofException(new NullPointerException());
        m.ifException(exceptionConsumer).ifValue(valueConsumer).ifSuccessful(successful);

        Mockito.verify(exceptionConsumer, times(1)).accept(any());
        Mockito.verify(valueConsumer, times(0)).accept(any());
        Mockito.verify(successful, times(0)).run();

    }

    @Test
    public void successfulVoidCallsNeiterValueNorException() {
        Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
        Consumer<Void> valueConsumer = mock(Consumer.class);
        Runnable successful = mock(Runnable.class);

        Maybe<Void> m = Maybe.ofVoid();
        m.ifException(exceptionConsumer).ifValue(valueConsumer).ifSuccessful(successful);

        Mockito.verify(exceptionConsumer, times(0)).accept(any());
        Mockito.verify(valueConsumer, times(0)).accept(any());
        Mockito.verify(successful, times(1)).run();
    }

    @Test
    public void successfulValueCallsValueButNotException() {
        Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
        Consumer<String> valueConsumer = mock(Consumer.class);
        Runnable successful = mock(Runnable.class);

        Maybe<String> m = Maybe.ofValue("42");
        m.ifException(exceptionConsumer).ifValue(valueConsumer).ifSuccessful(successful);

        Mockito.verify(exceptionConsumer, times(0)).accept(any());
        Mockito.verify(valueConsumer, times(1)).accept(any());
        Mockito.verify(successful, times(1)).run();
    }

}
