package org.ossgang.commons.observable;

import org.ossgang.commons.property.Properties;
import org.ossgang.commons.property.Property;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestObserver<T> implements Observer<T> {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final Property<Integer> valuesCount = Properties.property(0);
    private final List<T> values = new ArrayList<>();
    private final List<Throwable> exceptions = new ArrayList<>();

    @Override
    public void onValue(T value) {
        synchronized (values) {
            values.add(value);
            valuesCount.set(valuesCount.get() + 1);
        }
    }

    @Override
    public void onException(Throwable exception) {
        synchronized (exceptions) {
            exceptions.add(exception);
        }
    }

    public List<T> receivedValues() {
        synchronized (values) {
            return new ArrayList<>(values);
        }
    }

    public List<Throwable> receivedExceptions() {
        synchronized (exceptions) {
            return new ArrayList<>(exceptions);
        }
    }

    public void awaitForValueCountToBe(int expectedCount) {
        awaitForValueCountToBe(expectedCount, DEFAULT_TIMEOUT);
    }

    public void awaitForValueCountToBe(int expectedCount, Duration timeout) {
        CountDownLatch latch = new CountDownLatch(1);
        valuesCount.subscribe(i -> {
            if (i >= expectedCount) {
                latch.countDown();
            }
        });
        if (valuesCount.get() >= expectedCount) {
            return;
        }
        try {
            if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timeout occurred - timeout was " + timeout);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
