package io.github.ossgang.commons.observable;

import java.util.function.Consumer;

public interface ObservableValue<T> {
    void subscribe(Consumer<T> listener);

    T value();
}
