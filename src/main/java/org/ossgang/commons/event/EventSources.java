package org.ossgang.commons.event;

public class EventSources {
    private EventSources() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Create a {@link EventSource}.
     *
     * @param <T> the type
     * @return the new event source
     */
    public static <T> EventSource<T> eventSource() {
        return new SimpleEventSource<>();
    }
}
