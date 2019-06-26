package io.github.ossgang.commons.observable;

public class SimpleProperty<T> extends DispatchingObservableValue<T> implements Property<T> {
    SimpleProperty(T initial) {
        super(initial);
    }

    @Override
    public void set(T value) {
        update(value);
    }
}
