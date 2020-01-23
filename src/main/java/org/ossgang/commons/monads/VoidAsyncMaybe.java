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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class VoidAsyncMaybe extends AsyncMaybe<Void> {

    protected VoidAsyncMaybe(CompletableFuture<Void> future) {
        super(future);
    }

    @Override
    protected Maybe<Void> toMaybeBlocking(CompletableFuture<Void> future) {
        try {
            future.get();
            return Maybe.ofVoid();
        } catch (InterruptedException | ExecutionException e) {
            return Maybe.ofException(e);
        }
    }
}
