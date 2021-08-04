package org.ossgang.commons.observables.weak;

import org.junit.Test;
import org.ossgang.commons.awaitables.Await;
import org.ossgang.commons.observables.Dispatcher;
import org.ossgang.commons.observables.Observables;
import org.ossgang.commons.observables.Subscription;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observables.weak.WeakMethodReferenceObserver.DEFAULT_CLEANUP_PERIOD_SEC;

public class WeakObserverTest {

    public static final BiConsumer<Object, Object> NOOP_ON_VALUE = (h, v) -> {
    };
    public static final BiConsumer<Object, Throwable> NOOP_ON_EXCEPTION = (h, e) -> {
    };
    public static final BiConsumer<Object, Integer> NOOP_SUBSCRIBER_COUNTING = (h, i) -> {
    };

    @Test
    public void weakObserver_whenHolderIsCollected_shouldUnsubscribe() throws Exception {
        Object holder = new Object();
        CountDownLatch unSubscribed = new CountDownLatch(1);

        Dispatcher<Object> source = Observables.dispatcher();
        source.subscribe(newWeakObserver(holder, s -> unSubscribed.countDown()));

        WeakReference<?> weakHolder = new WeakReference<>(holder);
        holder = null;

        Await.await(() -> wasGarbageCollected(weakHolder))
                .withRetryInterval(ofSeconds(1))
                .withErrorMessage("Holder should be collected since WeakObserver should not keep a reference to it!")
                .atMost(ofSeconds(DEFAULT_CLEANUP_PERIOD_SEC * 2));

        assertThat(unSubscribed.await(DEFAULT_CLEANUP_PERIOD_SEC * 2, TimeUnit.SECONDS))
                .as("Subscriber should have been unsubscribed by now..")
                .isTrue();
    }

    @Test
    public void weakObserver_whenHolderIsStronglyReferenced_shouldNotUnsubscribe() throws InterruptedException {
        Object holder = new Object();

        CountDownLatch unSubscribed = new CountDownLatch(1);

        Dispatcher<Object> source = Observables.dispatcher();
        source.subscribe(newWeakObserver(holder, s -> unSubscribed.countDown()));

        assertThat(unSubscribed.await(DEFAULT_CLEANUP_PERIOD_SEC * 2, TimeUnit.SECONDS))
                .as("Subscriber should NOT have been unsubscribed because there is a strong reference to the holder!")
                .isFalse();
    }

    private static WeakMethodReferenceObserver<Object, Object> newWeakObserver(Object holder, Consumer<Subscription> onUnsubscribe) {
        return new WeakMethodReferenceObserver<Object, Object>(holder, NOOP_ON_VALUE, NOOP_ON_EXCEPTION, NOOP_SUBSCRIBER_COUNTING) {
            @Override
            public void onUnsubscribe(Subscription subscription) {
                super.onUnsubscribe(subscription);
                onUnsubscribe.accept(subscription);
            }
        };
    }

    private static Boolean wasGarbageCollected(WeakReference<?> weakHolder) {
        System.gc();
        return weakHolder.get() == null;
    }

}
