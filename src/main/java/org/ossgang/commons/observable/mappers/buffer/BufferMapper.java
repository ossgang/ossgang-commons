package org.ossgang.commons.observable.mappers.buffer;

import static org.ossgang.commons.observable.SubscriptionOptions.ON_CHANGE;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.ossgang.commons.collections.ConcurrentCircularBuffer;
import org.ossgang.commons.observable.Observable;
import org.ossgang.commons.observable.Observer;
import org.ossgang.commons.property.Properties;
import org.ossgang.commons.property.Property;

/**
 * Represents a buffer that can be used as a mapping function for {@link Observable#derive(Function)} method.
 * <p>
 * This buffer has properties himself, by which its behavior can be controlled during runtime:
 * <ul>
 * <li>{@link #size()}: Defines the size of the buffer</li>
 * <li>{@link #minEmitSize()}: Defines the size at which the downstream observable starts emitting. The default is
 * {@value #DEFAULT_MIN_EMIT_SIZE}.
 * </ul>
 * Additionally, the buffer exposes an {@link Observer}, {@link #clearTrigger()} that can be attached to another
 * observable, to trigger clearing of this buffer.
 */
public class BufferMapper<T> implements Function<T, Optional<List<T>>> {

    private static final int DEFAULT_MIN_EMIT_SIZE = 2;
    private final ConcurrentCircularBuffer<T> buffer;
    private final Property<Integer> size;
    private final Property<Integer> minEmitSize;
    private final Observer<?> clearTrigger;

    BufferMapper(int initialBufferSize) {
        this.buffer = new ConcurrentCircularBuffer<>();
        this.buffer.setLength(initialBufferSize);
        size = Properties.property(initialBufferSize);
        minEmitSize = Properties.property(Integer.min(initialBufferSize, DEFAULT_MIN_EMIT_SIZE));
        size.subscribe(buffer::setLength, ON_CHANGE);
        this.clearTrigger = (v) -> buffer.clear();
    }

    @Override
    public Optional<List<T>> apply(T t) {
        buffer.add(t);
        List<T> buffered = buffer.toList();
        if (buffered.size() > minEmitSize.get()) {
            return Optional.of(buffered);
        }
        return Optional.empty();
    }

    /**
     * An observer that can be attached to another observable in order to trigger clearing of the buffer. The buffer is
     * cleared on every value emitted.
     */
    public Observer<?> clearTrigger() {
        return this.clearTrigger;
    }

    /**
     * @return a property to control the minimum size of the buffer, before the downstream observable starts emitting.
     */
    public Property<Integer> minEmitSize() {
        return this.minEmitSize;
    }

    /**
     * @return the property to control the size of the buffer.
     */
    public Property<Integer> size() {
        return this.size;
    }

    public void clear() {
        buffer.clear();
    }

}
