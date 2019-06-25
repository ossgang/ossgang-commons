package io.github.ossgang.commons.observable;

public interface Property<T> extends ObservableValue<T> {
    void set(T value);
}
