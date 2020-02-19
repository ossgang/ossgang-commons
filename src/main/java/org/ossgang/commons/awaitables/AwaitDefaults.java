package org.ossgang.commons.awaitables;

import java.time.Duration;

final class AwaitDefaults {
    private AwaitDefaults() {
        throw new UnsupportedOperationException("static only");
    }

    static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(100);
    static final int DEFAULT_RETRY_COUNT = Integer.MAX_VALUE;
}
