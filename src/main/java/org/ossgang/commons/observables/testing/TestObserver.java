package org.ossgang.commons.observables.testing;

import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.ossgang.commons.utils.AwaitUtils.awaitFor;

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

    @Override
    public void onValue(T value) {
        synchronized (lock) {
            values.add(value);
        }
    }

    @Override
    public void onException(Throwable exception) {
        synchronized (lock) {
            exceptions.add(exception);
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
        awaitFor(condition).withErrorMessage(errorMessage).atMost(timeout);
    }

}
