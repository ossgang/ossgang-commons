package io.github.ossgang.commons.observable;

public class Properties {
    private Properties() {
        throw new UnsupportedOperationException("static only");
    }

    public static <T> Property<T> property(T initialValue) {
        return new SimpleProperty<>(initialValue);
    }

    public static <T> Property<T> property() {
        return new SimpleProperty<>(null);
    }

    public static PropertyMaker propertyMaker() { return new PropertyMaker(); }
}
