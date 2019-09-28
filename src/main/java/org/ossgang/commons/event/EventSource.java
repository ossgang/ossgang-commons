package org.ossgang.commons.event;

import org.ossgang.commons.observable.Observable;

/**
 * An "event source": an Observable-backed dispatcher for events of type T.
 * @param <T>
 */
public interface EventSource<T> extends Observable<T> {
    void send(T event);
}
