package org.ossgang.commons.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.time.Duration.ZERO;

/**
 * Utility functions to use when it is needed to wait for a certain "event" to be resolved.
 */
public final class Awaits {

    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(100);

    private Awaits() {
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
                throw new AwaitTimeoutException("Timeout exceeded " + builder.timeout + ": " + builder.message.get());
            }
            if (count > builder.retryCount) {
                throw new AwaitRetryCountException("Retry count exceeded " + count + ": " + builder.message.get());
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

        private Supplier<String> message;
        private Duration retryInterval;
        private int retryCount;
        private Duration timeout;
        private Supplier<Boolean> condition;

        private AwaitBuilder(Supplier<Boolean> condition) {
            this.condition = condition;
            message = () -> "";
            retryInterval = DEFAULT_RETRY_INTERVAL;
            retryCount = Integer.MAX_VALUE;
        }

        public AwaitBuilder withErrorMessage(String errorMessage) {
            this.message = () -> errorMessage;
            return this;
        }

        public AwaitBuilder withErrorMessage(Supplier<String> errorMessage) {
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

