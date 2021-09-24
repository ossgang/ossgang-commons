package org.ossgang.commons.observables;

import org.ossgang.commons.monads.Maybe;
import org.ossgang.commons.observables.exceptions.UnhandledException;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility class to create {@link Observer} instances.
 */
public class Observers {
    private Observers() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Create an {@link Observer} for values and exceptions
     *
     * @param valueConsumer     for values
     * @param exceptionConsumer for exceptions
     * @param <T>               the type of event
     * @return the {@link Observer}
     */
    public static <T> Observer<T> withErrorHandling(Consumer<T> valueConsumer, Consumer<Throwable> exceptionConsumer) {
        return new Observer<T>() {
            public void onValue(T value) {
                valueConsumer.accept(value);
            }

            public void onException(Throwable exception) {
                exceptionConsumer.accept(exception);
            }
        };
    }

    /**
     * Create an {@link Observer} for {@link Maybe}s
     *
     * @param maybeConsumer for maybe
     * @param <T>           the type of event
     * @return the {@link Observer}
     */
    public static <T> Observer<T> forMaybes(Consumer<Maybe<T>> maybeConsumer) {
        return new Observer<T>() {

            public void onValue(T value) {
                maybeConsumer.accept(Maybe.ofValue(value));
            }

            public void onException(Throwable exception) {
                maybeConsumer.accept(Maybe.ofException(exception));
            }

        };
    }

    /**
     * Create an {@link Observer} for exceptions
     *
     * @param exceptionConsumer for the exceptions
     * @param <T>               the tupe of event
     * @return the {@link Observer}
     */
    public static <T> Observer<T> forExceptions(Consumer<Throwable> exceptionConsumer) {
        return new Observer<T>() {
            public void onValue(T value) {
            }

            public void onException(Throwable exception) {
                exceptionConsumer.accept(exception);
            }
        };
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
        return new WeakMethodReferenceObserver<>(holder, valueConsumer);
    }

    /**
     * @see #weak(Object, BiConsumer)
     */
    public static <C, T> Observer<T> weakWithErrorHandling(C holder, BiConsumer<? super C, T> valueConsumer,
                                                           BiConsumer<? super C, Throwable> exceptionConsumer) {
        return new WeakMethodReferenceObserver<>(holder, valueConsumer, exceptionConsumer);
    }
}