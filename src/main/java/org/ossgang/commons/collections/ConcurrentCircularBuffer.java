package org.ossgang.commons.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A lock-free implementation circular buffer of variable (maximum) size. The exact length is not fully guaranteed.
 * Cleanup is done on each addition of a value. The length can be changed during runtime.
 */
public class ConcurrentCircularBuffer<T> {

    private final AtomicLong nextIndex = new AtomicLong(0);
    private final AtomicLong firstIndex = new AtomicLong(0);
    private final AtomicInteger maxSize = new AtomicInteger(1);

    private final ConcurrentHashMap<Long, T> elements = new ConcurrentHashMap<>();

    /**
     * Adds a value to the buffer. If the maxLength of the buffer is exceeded, old elements are removed.
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
        long newFirstIndex = nextIndex.get() - maxSize.get();
        cleanUpTo(newFirstIndex);
    }

    private void cleanUpTo(long newFirstIndex) {
        long oldFirstIndex = firstIndex.getAndSet(newFirstIndex);
        for (long i = oldFirstIndex; i < newFirstIndex; i++) {
            elements.remove(i);
        }
    }

    /**
     * Retrieves the current content of the buffer as a list. Note: The list will have *roughly* the same length as the
     * buffer or less. There is no guarantee on the exact length, because of the lock-free nature of the buffer, it can
     * happen that items were removed or added while the retrieving loop is running.
     *
     * @return the content of the buffer as a list.
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
     * Changes the maximum length of the buffer to the given value. As soon as the maximum length is reached, old
     * entries will be cleaned up.
     *
     * @throws IllegalArgumentException if the new length is less than 0
     */
    public void setMaxSize(int newMaxSize) {
        if (newMaxSize < 0) {
            throw new IllegalArgumentException("buffer maxSize must be >= 0 but was set to " + newMaxSize);
        }
        maxSize.set(newMaxSize);
        cleanup();
    }
}
