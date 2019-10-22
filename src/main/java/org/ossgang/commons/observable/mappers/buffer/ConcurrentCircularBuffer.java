package org.ossgang.commons.observable.mappers.buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A circular buffer of a given size. The exact length is not fully guaranteed. Cleanup is done on each addition of a
 * value. The length can be changed during runtime, however, the new length only applies as soon as a new element is
 * added.
 */
public class ConcurrentCircularBuffer<T> {

    private final AtomicLong nextIndex = new AtomicLong(0);
    private final AtomicLong firstIndex = new AtomicLong(0);
    private final AtomicInteger length = new AtomicInteger(1);

    private final ConcurrentHashMap<Long, T> elements = new ConcurrentHashMap<>();

    /**
     * Adds a value to the buffer. If the length of the buffer is exceede, old elements are removed.
     */
    public void add(T value) {
        long index = nextIndex.getAndIncrement();
        elements.put(index, value);
        cleanup();
    }

    /**
     * Clears the buffer fully.
     */
    public void clear() {
        cleanUpTo(nextIndex.get());
    }

    private void cleanup() {
        long newFirstIndex = nextIndex.get() - length.get();
        cleanUpTo(newFirstIndex);
    }

    private void cleanUpTo(long newFirstIndex) {
        long oldFirstIndex = firstIndex.getAndSet(newFirstIndex);
        for (long i = oldFirstIndex; i < newFirstIndex; i++) {
            elements.remove(i);
        }
    }

    /**
     * Retrieves the current content of the buffer as a list.
     */
    public List<T> toList() {
        List<T> list = new ArrayList<>();
        for (long i = firstIndex.get(), next = nextIndex.get(); i < next; i++) {
            T element = elements.get(i);
            /*
             * we have to check for null here, because a concurrent modification could have removed it in the meantime
             */
            if (element != null) {
                list.add(element);
            }
        }
        return list;
    }

    /**
     * Changes the length of the buffer to the given value.
     *
     * @throws IllegalArgumentException if the new length is less than 0
     */
    public void setLength(int newLength) {
        if (newLength < 0) {
            throw new IllegalArgumentException("buffer length must be >= 0 but was set to " + newLength);
        }
        length.set(newLength);
    }
}
