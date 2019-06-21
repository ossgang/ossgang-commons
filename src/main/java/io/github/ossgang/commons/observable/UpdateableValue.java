package io.github.ossgang.commons.observable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNull;

public class UpdateableValue<T> implements ObservableValue<T> {
    private final Set<Consumer<T>> listeners = newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicReference<T> lastValue = new AtomicReference<>();
    private final ExecutorService dispatcher = Executors.newCachedThreadPool();

    private UpdateableValue(T initial) {
        lastValue.set(initial);
    }

    private UpdateableValue() {

    }

    public static <T> UpdateableValue<T> withInitialValue(T initialValue) {
        return new UpdateableValue<>(initialValue);
    }

    public static <T> UpdateableValue<T> empty() {
        return new UpdateableValue<>();
    }

    public void subscribe(Consumer<T> listener) {
        listeners.add(listener);
        T value = lastValue.get();
        if (value != null) {
            dispatch(listener, value);
        }
    }

    public void update(T newValue) {
        requireNonNull(newValue, "null value not allowed");
        if (!Objects.equals(lastValue.getAndSet(newValue), newValue)) {
            listeners.forEach(l -> dispatch(l, newValue));
        }
    }

    public T value() {
        return lastValue.get();
    }

    private void dispatch(Consumer<T> listener, T value) {
        dispatcher.submit(() -> {
            try {
                listener.accept(value);
            } catch (Exception e) {
                System.err.println("Error in event handler\nvalue: "+value);
                e.printStackTrace();
            }
        });
    }
}
