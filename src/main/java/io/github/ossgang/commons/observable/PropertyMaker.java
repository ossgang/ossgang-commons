package io.github.ossgang.commons.observable;

import java.util.function.Consumer;

public class PropertyMaker {
    public PropertyMaker dispatchOn(Consumer<Runnable> dispatcher) {
        return this;
    }

    public PropertyMaker allowNulls() {
        return this;
    }

    public PropertyMaker ignoreNulls() {
        return this;
    }

    public PropertyMaker onException(Consumer<Throwable> exceptionConsumer) {
        return this;
    }

    public <T> Property<T> makeProperty() {
        return new SimpleProperty<T>(null);
    }

    public <T> Property<T> makeProperty(T initialValue) {
        return new SimpleProperty<T>(initialValue);
    }
}
