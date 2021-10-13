package org.ossgang.commons.utils;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.ossgang.commons.monads.ThrowingConsumer;
import org.ossgang.commons.monads.ThrowingFunction;
import org.ossgang.commons.monads.ThrowingPredicate;
import org.ossgang.commons.monads.ThrowingRunnable;
import org.ossgang.commons.monads.ThrowingSupplier;

/**
 * Utility class to transform Runnables, Suppliers, Consumers and Functions which can throw checked exceptions to their
 * standard java counterparts, effectively wrapping checked exceptions into unchecked ones.
 */
public final class Uncheckeds {
    private Uncheckeds() {
        throw new UnsupportedOperationException("static only");
    }

    public static <T> Supplier<T> uncheckedSupplier(ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw asUnchecked(e);
            }
        };
    }

    public static Runnable uncheckedRunnable(ThrowingRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw asUnchecked(e);
            }
        };
    }

    public static <I, R> Function<I, R> uncheckedFunction(ThrowingFunction<I, R> function) {
        return i -> {
            try {
                return function.apply(i);
            } catch (Exception e) {
                throw asUnchecked(e);
            }
        };
    }

    public static <T> Consumer<T> uncheckedConsumer(ThrowingConsumer<T> consumer) {
        return c -> {
            try {
                consumer.accept(c);
            } catch (Exception e) {
                throw asUnchecked(e);
            }
        };
    }


    public static <T> Predicate<T> uncheckedPredicate(ThrowingPredicate<T> predicate) {
        return c -> {
            try {
                return predicate.test(c);
            } catch (Exception e) {
                throw asUnchecked(e);
            }
        };
    }

    /**
     * Returns an unchecked ({@link RuntimeException}) version of the given exception. It avoids wrapping if the
     * exception is already an unchecked one.
     *
     * @param exception the exception to convert to unchecked
     * @return an unchecked version of the given expression
     */
    public static RuntimeException asUnchecked(Throwable exception) {
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        }
        return new RuntimeException(exception);
    }

    /**
     * An unchecked sleep. Converts the duration to milliseconds and invokes Thread.sleep().
     * In case of an InterruptedException, it re-interrupts the threads and re-throws the exception as a
     * RuntimeException.
     *
     * @param duration the duration to sleep.
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
