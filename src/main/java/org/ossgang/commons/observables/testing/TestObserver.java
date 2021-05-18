package org.ossgang.commons.observables.testing;

import static org.ossgang.commons.awaitables.Await.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.Subscription;

/**
 * An {@link Observer} that is especially useful for testing {@link ObservableValue}s
 *
 * @param <T> the type of the observer
 */
public class TestObserver<T> implements Observer<T> {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final Object lock = new Object();
    private final List<T> values = new ArrayList<>();
    private final List<Throwable> exceptions = new ArrayList<>();
    private final List<ObserverEvent> observerEvents = new ArrayList<>();
    private boolean subscribed = false;

    @Override
    public void onValue(T value) {
        synchronized (lock) {
            observerEvents.add(ObserverEvent.ON_VALUE);
            if (subscribed) {
                values.add(value);
            }
        }
    }

    @Override
    public void onException(Throwable exception) {
        synchronized (lock) {
            observerEvents.add(ObserverEvent.ON_EXCEPTION);
            if (subscribed) {
                exceptions.add(exception);
            }
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized (lock) {
            observerEvents.add(ObserverEvent.ON_SUBSCRIBE);
            subscribed = true;
        }
    }

    @Override
    public void onUnsubscribe(Subscription subscription) {
        synchronized (lock) {
            observerEvents.add(ObserverEvent.ON_UNSUBSCRIBE);
            subscribed = false;
        }
    }

    public List<ObserverEvent> receivedEvents() {
        synchronized (lock) {
            return new ArrayList<>(observerEvents);
        }
    }

    public List<T> receivedValues() {
        synchronized (lock) {
            return new ArrayList<>(values);
        }
    }

    public List<Throwable> receivedExceptions() {
        synchronized (lock) {
            return new ArrayList<>(exceptions);
        }
    }

    public void awaitForEventCountsToBe(int count) {
        awaitForEventCountsToBe(count, DEFAULT_TIMEOUT);
    }

    public void awaitForEventCountsToBe(int count, Duration timeout) {
        awaitCondition(timeout, "Expected to receive " + count + " events", () -> {
            synchronized (lock) {
                return observerEvents.size() >= count;
            }
        });
    }

    public void awaitForExceptionCountToBe(int count) {
        awaitForExceptionCountToBe(count, DEFAULT_TIMEOUT);
    }

    public void awaitForExceptionCountToBe(int count, Duration timeout) {
        awaitCondition(timeout, "Expected to receive " + count + " exceptions", () -> {
            synchronized (lock) {
                return exceptions.size() >= count;
            }
        });
    }

    public void awaitForValueCountToBe(int expectedCount) {
        awaitForValueCountToBe(expectedCount, DEFAULT_TIMEOUT);
    }

    public void awaitForValueCountToBe(int expectedCount, Duration timeout) {
        awaitCondition(timeout, "Expected to receive " + expectedCount + " values", () -> {
            synchronized (lock) {
                return values.size() >= expectedCount;
            }
        });
    }

    public void awaitForPublishedValuesToContain(T targetValue) {
        awaitCondition(DEFAULT_TIMEOUT, "Received values do not contain " + targetValue, () -> {
            synchronized (lock) {
                return values.contains(targetValue);
            }
        });
    }

    private static void awaitCondition(Duration timeout, String errorMessage, Supplier<Boolean> condition) {
        if (condition.get()) {
            return;
        }
        await(condition).withErrorMessage(errorMessage).atMost(timeout);
    }

    public enum ObserverEvent {
        ON_VALUE, ON_EXCEPTION, ON_SUBSCRIBE, ON_UNSUBSCRIBE;
    }
}
