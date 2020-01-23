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

import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AsyncMaybeTest {

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
        assertThat(AsyncMaybe.ofValue(42).toMaybeBlocking().hasException()).isFalse();
        assertThat(AsyncMaybe.ofVoid().toMaybeBlocking().hasException()).isFalse();
        assertThat(AsyncMaybe.ofException(new RuntimeException()).toMaybeBlocking().hasException()).isTrue();
    }

    @Test
    public void successfulChain() {
        Maybe<Integer> integerMaybe = AsyncMaybe.attemptAsync(() -> 42).map(v -> 65 - v).toMaybeBlocking();
        assertThat(integerMaybe.value()).isEqualTo(23);
    }

    @Test
    public void successfulVoidChain() {
        Maybe<Void> integerMaybe = AsyncMaybe.attemptAsync(() -> 42).then(() -> {
        }).toMaybeBlocking();
        assertThat(integerMaybe.optionalValue()).isEmpty();
    }

    @Test
    public void successfulChainWithThen() {
        Maybe<String> chain = AsyncMaybe.attemptAsync(() -> Thread.sleep(0))//
                .then(() -> 42)//
                .map(v -> 21 - v)//
                .map((Integer v) -> assertThat(v).isEqualTo(-21))//
                .then(() -> "Success")
                .toMaybeBlocking();
        assertThat(chain.value()).isEqualTo("Success");
    }

    @Test(expected = RuntimeException.class)
    public void failureMapping() {
        Maybe<Integer> maybe = AsyncMaybe.attemptAsync(() -> 42)
                .map(this::throwException1)
                .map(v -> 84 - v)
                .map(this::throwException2)
                .toMaybeBlocking();
        assertThat(maybe.hasException()).isTrue();
        assertThat(maybe.exception()).hasCauseInstanceOf(MaybeTestException1.class);
        maybe.value(); /* <- throws */
    }

    @Test
    @SuppressWarnings("unchecked")
    public void chainStopsOnceExceptionOccurs() throws Exception {
        ThrowingFunction<String, String> function1 = mock(ThrowingFunction.class);
        Mockito.when(function1.apply(anyString())).thenReturn("42");
        ThrowingFunction<Integer, String> function2 = mock(ThrowingFunction.class);
        Maybe<String> maybe = AsyncMaybe.attemptAsync(() -> "HelloWorld")
                .map(function1)
                .map(this::throwException2)
                .map(function2)
                .toMaybeBlocking();
        verify(function1, times(1)).apply("HelloWorld");
        verify(function2, never()).apply(anyInt());
        assertThat(maybe.hasException()).isTrue();
        assertThat(maybe.exception()).hasCauseInstanceOf(MaybeTestException2.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void recoverClearsException() throws Exception {
        ThrowingFunction<String, String> function1 = mock(ThrowingFunction.class);
        Mockito.when(function1.apply(anyString())).thenReturn("42");

        ThrowingFunction<Throwable, Integer> recoveryFunction = mock(ThrowingFunction.class);
        Mockito.when(recoveryFunction.apply(anyExceptionWithCause(MaybeTestException2.class))).thenReturn(42);

        ThrowingFunction<Integer, String> function2 = mock(ThrowingFunction.class);
        Mockito.when(function2.apply(42)).thenReturn("OK");

        Maybe<String> maybe = AsyncMaybe.attemptAsync(() -> "HelloWorld")
                .map(function1)
                .map(this::throwException2)
                .recover(recoveryFunction)
                .map(function2)
                .toMaybeBlocking();
        verify(function1, times(1)).apply("HelloWorld");
        verify(recoveryFunction, times(1)).apply(any(CompletionException.class)); /* <-- MaybeTestException2 wrapped by completable future*/
        verify(function2, times(1)).apply(42);
        assertThat(maybe.hasException()).isFalse();
        assertThat(maybe.value()).isEqualTo("OK");
    }

    @Test
    public void successfulVoidState() {
        Maybe<Void> m = AsyncMaybe.ofVoid().toMaybeBlocking();
        assertThat(m.isSuccessful()).isTrue();
        assertThat(m.hasValue()).isFalse();
        assertThat(m.hasException()).isFalse();
    }

    @Test(expected = NoSuchElementException.class)
    public void successfulVoidThrowsOnGettingException() {
        AsyncMaybe.ofVoid().toMaybeBlocking().exception();
    }

    @Test(expected = NoSuchElementException.class)
    public void successfulVoidThrowsOnGettingValue() {
        AsyncMaybe.ofVoid().toMaybeBlocking().value();
    }

    @Test
    public void successfulValueState() {
        Maybe<String> m = AsyncMaybe.ofValue("42").toMaybeBlocking();
        assertThat(m.hasException()).isFalse();
        assertThat(m.isSuccessful()).isTrue();
        assertThat(m.hasValue()).isTrue();
        assertThat(m.value()).isEqualTo("42");
    }

    @Test(expected = NoSuchElementException.class)
    public void successfulValueThrowsOnException() {
        AsyncMaybe.ofValue("42").toMaybeBlocking().exception();
    }

    @Test
    public void unsuccessfulCallsIfException() {
        Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
        Consumer<Void> valueConsumer = mock(Consumer.class);
        Runnable successful = mock(Runnable.class);

        Maybe<Void> m = AsyncMaybe.<Void>ofException(new NullPointerException()).toMaybeBlocking();
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

        Maybe<Void> m = AsyncMaybe.ofVoid().toMaybeBlocking();
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

        Maybe<String> m = AsyncMaybe.ofValue("42").toMaybeBlocking();
        m.ifException(exceptionConsumer).ifValue(valueConsumer).ifSuccessful(successful);

        Mockito.verify(exceptionConsumer, times(0)).accept(any());
        Mockito.verify(valueConsumer, times(1)).accept(any());
        Mockito.verify(successful, times(1)).run();
    }

    private static Throwable anyExceptionWithCause(Class<? extends Throwable> throwableClass) {
        return ArgumentMatchers.argThat(t -> throwableClass.isAssignableFrom(t.getCause().getClass()));
    }

}