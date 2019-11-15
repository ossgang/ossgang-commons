package org.ossgang.commons.observables.operators.buffer;

import org.ossgang.commons.collections.ConcurrentCircularBuffer;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.SubscriptionOptions;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a buffer that can be used as a mapping function for {@link Observable#derive(Function)} method.
 * <p>
 * This buffer has properties himself, by which its behavior can be controlled during runtime:
 * <ul>
 * <li>{@link #maxSize()}: Defines the size of the buffer</li>
 * <li>{@link #minEmitSize()}: Defines the size at which the downstream observable starts emitting. The default is
 * {@value #DEFAULT_MIN_EMIT_SIZE}.
 * </ul>
 * Additionally, the buffer exposes an {@link Observer}, {@link #clearTrigger()} that can be attached to another
 * observable, to trigger clearing of this buffer.
 */
public class BufferMapper<T> implements Function<T, Optional<List<T>>> {

    private static final int DEFAULT_MIN_EMIT_SIZE = 2;
    private final ConcurrentCircularBuffer<T> buffer;
    private final Property<Integer> maxSize;
    private final Property<Integer> minEmitSize;
    private final Observer<?> clearTrigger;

    BufferMapper(int initialMaxSize) {
        this.buffer = new ConcurrentCircularBuffer<>();
        this.buffer.setMaxSize(initialMaxSize);
        maxSize = Properties.property(initialMaxSize);
        minEmitSize = Properties.property(Integer.min(initialMaxSize, DEFAULT_MIN_EMIT_SIZE));
        maxSize.subscribe(buffer::setMaxSize, SubscriptionOptions.ON_CHANGE);
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
     * @return the property to control the maximum size of the buffer.
     */
    public Property<Integer> maxSize() {
        return this.maxSize;
    }

    /**
     * Clears the content of the buffer.
     */
    public void clear() {
        buffer.clear();
    }

}
