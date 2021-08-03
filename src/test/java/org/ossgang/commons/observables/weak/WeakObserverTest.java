package org.ossgang.commons.observables.weak;

import org.junit.*;
import org.ossgang.commons.awaitables.Await;
import org.ossgang.commons.observables.Dispatcher;
import org.ossgang.commons.observables.Observables;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

public class WeakObserverTest {

    @Test
    public void weakObserver_shouldGetGCd() {
        Object holder = new Object();
        AtomicReference<Object> lastValue = new AtomicReference<>();

        Dispatcher<Object> source = Observables.dispatcher();
        source.subscribe(WeakObservers.weak(holder, (h, v) -> lastValue.set(v)));
        source.dispatchValue("one");

        Await.await(() -> Objects.equals(lastValue.get(), "one"))
                .withRetryInterval(ofSeconds(1))
                .withErrorMessage("Value event should have arrived !")
                .atMost(ofSeconds(10));

        WeakReference<Dispatcher<Object>> weakSource = new WeakReference<>(source);
        WeakReference<?> weakHolder = new WeakReference<>(holder);
        source = null;
        holder = null;

        Await.await(() -> wasGarbageCollected(weakHolder))
                .withRetryInterval(ofSeconds(1))
                .withErrorMessage("Holder should be collected since WeakObserver should not keep a reference to it!")
                .atMost(ofSeconds(WeakMethodReferenceObserver.DEFAULT_CLEANUP_PERIOD * 2));

        Await.await(() -> wasGarbageCollected(weakSource))
                .withRetryInterval(ofSeconds(1))
                .withErrorMessage("Source should be collected since there should be 0 subscribers!")
                .atMost(ofSeconds(WeakMethodReferenceObserver.DEFAULT_CLEANUP_PERIOD * 2));

        assertThat(lastValue.get())
                .as("No other value event should be dispatched")
                .isEqualTo("one");
    }

    private static Boolean wasGarbageCollected(WeakReference<?> weakHolder) {
        System.gc();
        return weakHolder.get() == null;
    }

}
