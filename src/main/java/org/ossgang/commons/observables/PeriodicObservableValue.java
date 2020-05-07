package org.ossgang.commons.observables;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Creates an observable value that emits periodically at the given rate. The value emitted is the current time (as
 * instant). Emitting is done on a single thread.
 */
public class PeriodicObservableValue extends DispatchingObservableValue<Instant> {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    PeriodicObservableValue(long period, TimeUnit unit) {
        super(Instant.now());
        executor.scheduleAtFixedRate(() -> dispatchValue(Instant.now()), 0, period, unit);
    }

}
