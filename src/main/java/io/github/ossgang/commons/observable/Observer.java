package io.github.ossgang.commons.observable;

public interface Observer<T> {

    void onValue(T value);

    default void onException(Throwable exception) {};
}
