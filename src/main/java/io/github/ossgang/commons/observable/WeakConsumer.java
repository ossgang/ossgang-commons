package io.github.ossgang.commons.observable;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public class WeakConsumer<T> implements Consumer<T> {

    private final WeakReference<Consumer<? super T>> consumer;
    private final Consumer<WeakConsumer<T>> cleanUp;

    WeakConsumer(Consumer<? super T> parent, Consumer<WeakConsumer<T>> cleanUp) {
        this.consumer = new WeakReference<>(parent);
        this.cleanUp = cleanUp;
    }

    @Override
    public void accept(T t) {
        Consumer<? super T> parent = consumer.get();
        if (parent != null) {
            parent.accept(t);
        } else {
            cleanUp.accept(this);
        }
    }
}
