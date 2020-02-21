package org.ossgang.commons.utils;

import org.ossgang.commons.monads.*;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class to transform Runnables, Suppliers, Consumers and Functions which can throw checked exceptions to their
 * standard java counterparts, effectively wrapping checked exceptions into unchecked ones.
 */
public final class Uncheckeds {
    private Uncheckeds() {
        throw new UnsupportedOperationException("static only");
    }

    public static <T> Supplier<T> uncheckedSupplier(ThrowingSupplier<T> supplier) {
        return () -> Maybe.attempt(supplier).value();
    }

    public static <I, R> Function<I, R> uncheckedFunction(ThrowingFunction<I, R> function) {
        return i -> Maybe.ofValue(i).map(function).value();
    }

    public static Runnable uncheckedRunnable(ThrowingRunnable runnable) {
        return () -> Maybe.attempt(runnable).throwOnException();
    }

    public static <T> Consumer<T> uncheckedConsumer(ThrowingConsumer<T> consumer) {
        return c -> Maybe.ofValue(c).then(consumer).throwOnException();
    }
}
