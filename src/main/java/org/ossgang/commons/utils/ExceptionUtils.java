// @formatter:off
/*******************************************************************************
 *
 * This file is part of ossgang-commons.
 *
 * Copyright (c) 2008-2019, CERN. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
// @formatter:on

package org.ossgang.commons.utils;

import org.ossgang.commons.monads.ThrowingConsumer;
import org.ossgang.commons.monads.ThrowingFunction;
import org.ossgang.commons.monads.ThrowingRunnable;
import org.ossgang.commons.monads.ThrowingSupplier;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * TODO andrea
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
        /* static methods */
    }

    /**
     * TODO andrea
     *
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> Supplier<T> unchecked(ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * TODO andrea
     *
     * @param runnable
     * @return
     */
    public static Runnable unchecked(ThrowingRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * TODO andrea
     *
     * @param consumer
     * @param <T>
     * @return
     */
    public static <T> Consumer<T> unchecked(ThrowingConsumer<T> consumer) {
        return value -> {
            try {
                consumer.accept(value);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * TODO andrea
     *
     * @param function
     * @param <I>
     * @param <O>
     * @return
     */
    public static <I, O> Function<I, O> unchecked(ThrowingFunction<I, O> function) {
        return value -> {
            try {
                return function.apply(value);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
