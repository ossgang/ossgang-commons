package org.ossgang.commons.observables;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;

/**
 * Creates an observable value that emits periodically at the given rate. The value emitted is the current time (as
 * instant). Emitting is done on a single thread.
 */
public class PeriodicObservableValue extends DispatchingObservableValue<Instant> {

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = newScheduledThreadPool(1,
            daemonThreadFactoryWithPrefix("ossgang-commons-PeriodicObservable-"));

    PeriodicObservableValue(long period, TimeUnit unit) {
        super(Instant.now());
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> dispatchValue(Instant.now()), 0, period, unit);
    }

}
