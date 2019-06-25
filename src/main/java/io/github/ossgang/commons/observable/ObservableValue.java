package io.github.ossgang.commons.observable;

public interface ObservableValue<T> extends Observable<T> {
    // delivered before listener is registered on the subscribing thread
    static SubscriptionOption FIRST_UPDATE = new SubscriptionOption();
    static SubscriptionOption ON_CHANGE = new SubscriptionOption();

    T get();
}
