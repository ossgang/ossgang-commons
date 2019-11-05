package org.ossgang.commons.observable;

import org.ossgang.commons.property.Properties;
import org.ossgang.commons.property.Property;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.ossgang.commons.observable.SubscriptionOptions.FIRST_UPDATE;


public class TestObserver<T> implements Observer<T> {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final Object lock = new Object();
    private final Property<List<T>> values = Properties.property(new ArrayList<>());
    private final Property<List<Throwable>> exceptions = Properties.property(new ArrayList<>());

    @Override
    public void onValue(T value) {
        synchronized (lock) {
            List<T> newValues = new ArrayList<>(values.get());
            newValues.add(value);
            values.set(newValues);
        }
    }

    @Override
    public void onException(Throwable exception) {
        synchronized (lock) {
            List<Throwable> newExceptions = new ArrayList<>(exceptions.get());
            newExceptions.add(exception);
            exceptions.set(newExceptions);
        }
    }

    public List<T> receivedValues() {
        synchronized (lock) {
            return new ArrayList<>(values.get());
        }
    }

    public List<Throwable> receivedExceptions() {
        synchronized (lock) {
            return new ArrayList<>(exceptions.get());
        }
    }

    public void awaitForValueCountToBe(int expectedCount) {
        awaitForValueCountToBe(expectedCount, DEFAULT_TIMEOUT);
    }

    public void awaitForValueCountToBe(int expectedCount, Duration timeout) {
        if (values.get().size() >= expectedCount) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Subscription subscription = values.subscribe(v -> {
            if (v.size() >= expectedCount) {
                latch.countDown();
            }
        }, FIRST_UPDATE);

        try {
            if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timeout while waiting for count " + expectedCount + " - timeout was " + timeout);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            subscription.unsubscribe();
        }
    }

    public void awaitForValue(T targetValue) {
        CountDownLatch latch = new CountDownLatch(1);
//        int currentSize;
        Subscription subscription;
        synchronized (lock) {
            if (values.get().contains(targetValue)) {
                return;
            }

//            currentSize = values.get().size();
            subscription = values.subscribe(v -> {
//                List<T> newValues = new ArrayList<>(v).subList(currentSize, v.size());
                if (v.contains(targetValue)) {
                    latch.countDown();
                }
            }, FIRST_UPDATE);
        }

        try {
            if (!latch.await(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timeout while waiting for value " + targetValue + " - timeout was " + DEFAULT_TIMEOUT);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (subscription != null) {
                subscription.unsubscribe();
            }
        }
    }
}
