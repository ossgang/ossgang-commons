package org.ossgang.commons.event;

import org.ossgang.commons.observable.DispatchingObservable;

public class SimpleEventSource<T> extends DispatchingObservable<T> implements EventSource<T>  {
    SimpleEventSource() {}

    @Override
    public void send(T event) {
        dispatchValue(event);
    }
}
