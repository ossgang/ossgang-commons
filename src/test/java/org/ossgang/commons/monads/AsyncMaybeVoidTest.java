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

package org.ossgang.commons.monads;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncMaybeVoidTest {

    @Test
    public void testWhenCompleteVoid() {
        AsyncMaybe<Void> async = AsyncMaybe.attemptAsync(() -> {
        });
        async.whenComplete(maybe -> {
        });
        async.toMaybeBlocking().throwOnException();
    }

    @Test
    public void testAsyncMaybeVoid_afterWhenValue_shouldNotThrowNPE() {
        AsyncMaybe<Void> async1 = AsyncMaybe.attemptAsync(() -> {
        });
        AsyncMaybe<Void> async2 = async1.whenValue(v -> {
        });
        async2.toMaybeBlocking().throwOnException();
    }

    @Test
    public void testAsyncMaybeVoid_afterWhenException_shouldNotThrowNPE() {
        AsyncMaybe<Void> async1 = AsyncMaybe.attemptAsync(() -> {
            throw new RuntimeException("testAsyncMaybeVoid_afterWhenException_shouldNotThrowNPE");
        });
        AsyncMaybe<Void> async2 = async1.whenException(e -> {
        });
        Maybe<Void> maybe = async2.toMaybeBlocking();
        Assertions.assertThat(maybe.exception()).hasMessage("testAsyncMaybeVoid_afterWhenException_shouldNotThrowNPE");
    }

    @Test
    public void testAsyncMaybeVoid_afterRecover_shouldNotThrowNPE() {
        AsyncMaybe<Void> async1 = AsyncMaybe.attemptAsync(() -> {
            throw new RuntimeException();
        });
        AsyncMaybe<Void> async2 = async1.recover(e -> {
        });
        async2.toMaybeBlocking().throwOnException();
    }

    @Test
    public void testAsyncMaybeVoid_afterThenConsumer_shouldNotThrowNPE() {
        AsyncMaybe<Void> async1 = AsyncMaybe.attemptAsync(() -> {
        });
        AsyncMaybe<Void> async2 = async1.then(any -> {
        });
        async2.toMaybeBlocking().throwOnException();
    }

    @Test
    public void testAsyncMaybeVoid_afterThenRunnable_shouldNotThrowNPE() {
        AsyncMaybe<Void> async1 = AsyncMaybe.attemptAsync(() -> {
        });
        AsyncMaybe<Void> async2 = async1.then(() -> {
        });
        async2.toMaybeBlocking().throwOnException();
    }

    @Test
    public void testAsyncMaybeVoid_afterWhenCompleteConsumer_shouldNotThrowNPE() {
        AsyncMaybe<Void> async1 = AsyncMaybe.attemptAsync(() -> {
        });
        AsyncMaybe<Void> async2 = async1.whenComplete(maybe -> {
        });
        async2.toMaybeBlocking().throwOnException();
    }


    @Test
    public void testAsyncMaybeVoid_whenSuccessful_isCalledWithResolvedAsyncMaybe() {
        CompletableFuture<String> result = new CompletableFuture<>();
        AsyncMaybe.ofVoid().whenSuccessful(() -> result.complete("Success"));
        assertThat(result.join()).isEqualTo("Success");
    }

    @Test
    public void testAsyncMaybeVoid_whenSuccessful_isCalledAfterAsyncExecution() {
        CompletableFuture<String> result = new CompletableFuture<>();
        AsyncMaybe.attemptAsync(() -> {}).whenSuccessful(() -> result.complete("Success"));
        assertThat(result.join()).isEqualTo("Success");
    }

}
