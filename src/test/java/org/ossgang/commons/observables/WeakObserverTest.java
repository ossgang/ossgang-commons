package org.ossgang.commons.observables;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.ossgang.commons.GcTests.wasGarbageCollected;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;
import org.ossgang.commons.awaitables.Await;

public class WeakObserverTest {

    private static final BiConsumer<Object, Object> NOOP_ON_VALUE = (h, v) -> {
    };
    private static final BiConsumer<Object, Throwable> NOOP_ON_EXCEPTION = (h, e) -> {
    };

    @Test
    public void weakObserver_whenHolderIsCollected_shouldUnsubscribe() throws Exception {
        Object holder = new Object();
        CountDownLatch unSubscribed = new CountDownLatch(1);

        Dispatcher<Object> source = Observables.dispatcher();
        source.subscribe(newWeakObserver(holder, s -> unSubscribed.countDown()));

        WeakReference<?> weakHolder = new WeakReference<>(holder);
        holder = null;

        Await.await(() -> wasGarbageCollected(weakHolder)).withRetryInterval(ofMillis(10))
                .withErrorMessage("Holder should be collected since WeakObserver should not keep a reference to it!")
                .atMost(ofSeconds(5));

        assertThat(unSubscribed.await(5, TimeUnit.SECONDS))
                .as("Subscriber should have been unsubscribed by now because the holder was collected").isTrue();
    }

    @Test
    public void weakObserver_whenHolderIsStronglyReferenced_shouldNotUnsubscribe() throws InterruptedException {
        Object holder = new Object();

        CountDownLatch unSubscribed = new CountDownLatch(1);

        Dispatcher<Object> source = Observables.dispatcher();
        source.subscribe(newWeakObserver(holder, s -> unSubscribed.countDown()));

        assertThat(unSubscribed.await(1, TimeUnit.SECONDS))
                .as("Subscriber should NOT have been unsubscribed because there is a strong reference to the holder!")
                .isFalse();
    }

    @Test
    public void weakObserver_whenHolderIsCollectedAndReuseIsAttempted_shouldThrowOnSubscribe() throws Exception {
        Object holder = new Object();
        CountDownLatch unSubscribed = new CountDownLatch(1);

        Dispatcher<Object> source = Observables.dispatcher();
        AtomicReference<WeakMethodReferenceObserver<Object, Object>> observerRef =
                new AtomicReference<>(newWeakObserver(holder, s -> unSubscribed.countDown()));

        WeakReference<?> weakHolder = new WeakReference<>(holder);
        holder = null;

        Await.await(() -> wasGarbageCollected(weakHolder)).withRetryInterval(ofMillis(10))
                .withErrorMessage("Holder should be collected since WeakObserver should not keep a reference to it!")
                .atMost(ofSeconds(5));

        Thread.sleep(100); /* allow the WeakObserver cleanup to run */

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> source.subscribe(observerRef.get()))
                .withMessageContaining("Weak observer has been garbage collected");

        WeakReference<WeakMethodReferenceObserver<?,?>> weakObserverRef = new WeakReference<>(observerRef.get());
        observerRef.set(null);

        Await.await(() -> wasGarbageCollected(weakObserverRef)).withRetryInterval(ofMillis(10))
                .withErrorMessage("WeakObserver itself should be collected!")
                .atMost(ofSeconds(5));
    }

    private static WeakMethodReferenceObserver<Object, Object> newWeakObserver(Object holder,
            Consumer<Subscription> onUnsubscribe) {
        return new WeakMethodReferenceObserver<Object, Object>(holder, NOOP_ON_VALUE, NOOP_ON_EXCEPTION) {
            @Override
            public void onUnsubscribe(Subscription subscription) {
                super.onUnsubscribe(subscription);
                onUnsubscribe.accept(subscription);
            }
        };
    }

}
