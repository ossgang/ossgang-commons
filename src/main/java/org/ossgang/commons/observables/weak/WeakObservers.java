package org.ossgang.commons.observables.weak;

import java.util.function.BiConsumer;

import org.ossgang.commons.observables.Observer;

public class WeakObservers {
    private WeakObservers() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Create an observer based on a weak reference to an object, and class method references to consumers for values
     * (and, optionally, exceptions).
     * Release the reference to the subscriber as soon as the (weak-referenced) holder object is GC'd. A common use
     * case for this is e.g. working with method references within a particular object:
     * <pre>
     * class Test {
     *     public void doSubscribe(Observable&lt;String&gt; obs) {
     *         obs.subscribe(weak(this, Test::handle));
     *     }
     *     private void handle(String update) ...
     * }
     * </pre>
     * This will allow the "Test" instance to be GC'd, terminating the subscription; but for as long as it lives, the
     * subscription will be kept alive.
     */
    public static <C, T> Observer<T> weak(C holder, BiConsumer<C, T> valueConsumer) {
        return new WeakMethodReferenceObserver<>(holder, valueConsumer, (a, b) -> {}, (a, b) -> {});
    }

    /**
     * @see #weak(Object, BiConsumer)
     */
    public static <C, T> Observer<T> weakWithErrorHandling(C holder, BiConsumer<? super C, T> valueConsumer,
                                                           BiConsumer<? super C, Throwable> exceptionConsumer) {
        return new WeakMethodReferenceObserver<>(holder, valueConsumer, exceptionConsumer, (a, b) -> {});
    }

    /**
     * @see #weak(Object, BiConsumer)
     */
    public static <C, T> Observer<T> weakWithErrorAndSubscriptionCountHandling(C holder,
                                                                               BiConsumer<? super C, T> valueConsumer,
                                                                               BiConsumer<? super C, Throwable> exceptionConsumer,
                                                                               BiConsumer<? super C, Integer> subscriptionCountChanged) {
        return new WeakMethodReferenceObserver<>(holder, valueConsumer, exceptionConsumer, subscriptionCountChanged);
    }
}
