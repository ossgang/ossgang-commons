package org.ossgang.commons.observable.mappers.buffer;

import org.ossgang.commons.observable.Observable;

/**
 * Contains factor methods for functions that can be used in {@link Observable#map(java.util.function.Function)}
 * methods, to buffer values of the incoming stream.
 */
public final class Buffers {

    private Buffers() {
        throw new UnsupportedOperationException("Only static methods.");
    }

    /**
     * Creates a buffer of the given length.
     *
     * @see BufferMapper
     */
    public static <T> BufferMapper<T> buffer(int initialSize) {
        return new BufferMapper<>(initialSize);
    }

}
