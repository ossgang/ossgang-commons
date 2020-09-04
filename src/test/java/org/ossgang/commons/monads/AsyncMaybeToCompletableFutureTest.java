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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class AsyncMaybeToCompletableFutureTest {

    @Test
    public void testCompletableFuture_canBeCreatedFromSuccessfulAsyncMaybe() {
        AsyncMaybe<String> asyncMaybe = AsyncMaybe.attemptAsync(() -> "OK");
        CompletableFuture<String> completableFuture = AsyncMaybes.toCompletableFuture(asyncMaybe);

        Assertions.assertThat(completableFuture.join()).isEqualTo("OK");
    }

    @Test
    public void testCompletableFuture_canBeCreatedFromUnsuccessfulAsyncMaybe() {
        AsyncMaybe<Object> asyncMaybe = AsyncMaybe.attemptAsync(() -> {
            throw new RuntimeException("NOT OK");
        });
        CompletableFuture<Object> completableFuture = AsyncMaybes.toCompletableFuture(asyncMaybe);

        Assertions.assertThatThrownBy(completableFuture::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class)
                .matches(t -> t.getCause().getMessage().equals("NOT OK"), "Root exception message not correct");
    }

    @Test
    public void testAsyncMaybe_canBeCreatedFromSuccessfulCompletableFuture() {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "OK");
        AsyncMaybe<String> asyncMaybe = AsyncMaybes.toAsyncMaybe(completableFuture);

        Assertions.assertThat(asyncMaybe.toMaybeBlocking().value()).isEqualTo("OK");
    }

    @Test
    public void testAsyncMaybe_canBeCreatedFromUnsuccessfulCompletableFuture() {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("NOT OK");
        });
        AsyncMaybe<String> asyncMaybe = AsyncMaybes.toAsyncMaybe(completableFuture);

        Assertions.assertThat(asyncMaybe.toMaybeBlocking().exception())
                .isInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(ExecutionException.class)
                .matches(t -> t.getCause().getCause() instanceof RuntimeException, "Root exception should be RuntimeException")
                .matches(t -> t.getCause().getCause().getMessage().equals("NOT OK"), "Root exception message not correct");
    }

}
