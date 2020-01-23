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

package org.ossgang.commons.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.time.Duration.ZERO;

/**
 * Utility functions to use when it is needed to wait for a certain "event" to be resolved.
 */
public final class AwaitUtils {

    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(100);

    private AwaitUtils() {
        /* static methods */
    }

    public static AwaitBuilder awaitFor(Supplier<Boolean> predicate) {
        return new AwaitBuilder(predicate);
    }

    private static void await(AwaitBuilder builder) {
        boolean timeoutEnabled = !builder.timeout.equals(ZERO);
        int count = 1;
        Instant beforeWaiting = Instant.now();
        while (!builder.condition.get()) {
            if (timeoutEnabled && timeoutExceeded(beforeWaiting, builder.timeout)) {
                throw new AwaitTimeoutException("Timeout exceeded " + builder.timeout + ": " + builder.message);
            }
            if (count > builder.retryCount) {
                throw new AwaitRetryCountException("Retry count exceeded " + count + ": " + builder.message);
            }
            try {
                if (builder.retryInterval.equals(ZERO)) {
                    Thread.yield();
                } else {
                    TimeUnit.MILLISECONDS.sleep(builder.retryInterval.toMillis());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            count++;
        }
    }

    private static boolean timeoutExceeded(Instant beforeWaiting, Duration timeout) {
        return Duration.between(beforeWaiting, Instant.now()).compareTo(timeout) > 0;
    }

    public static final class AwaitBuilder {

        private String message;
        private Duration retryInterval;
        private int retryCount;
        private Duration timeout;
        private Supplier<Boolean> condition;

        private AwaitBuilder(Supplier<Boolean> condition) {
            this.condition = condition;
            message = "";
            retryInterval = DEFAULT_RETRY_INTERVAL;
            retryCount = Integer.MAX_VALUE;
        }

        public AwaitBuilder withErrorMessage(String errorMessage) {
            this.message = errorMessage;
            return this;
        }

        public AwaitBuilder withRetryCount(int numberOfRetry) {
            if (numberOfRetry < 0) {
                throw new IllegalArgumentException("Retry count cannot be negative");
            }
            this.retryCount = numberOfRetry;
            return this;

        }

        public AwaitBuilder withRetryInterval(Duration interval) {
            if (interval.isNegative()) {
                throw new IllegalArgumentException("Retry interval cannot be negative");
            }
            this.retryInterval = interval;
            return this;
        }

        public void indefinitely() {
            this.timeout = ZERO;
            await(this);
        }

        public void atMost(Duration aTimeout) {
            if (aTimeout.equals(ZERO)) {
                throw new IllegalArgumentException("If you want a ZERO timeout, use indefinitely()");
            }
            if (aTimeout.isNegative()) {
                throw new IllegalArgumentException("Timeout cannot be negative");
            }
            this.timeout = aTimeout;
            await(this);
        }

    }

    public static class AwaitTimeoutException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public AwaitTimeoutException(String message) {
            super(message);
        }
    }

    public static class AwaitRetryCountException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public AwaitRetryCountException(String message) {
            super(message);
        }
    }
}

