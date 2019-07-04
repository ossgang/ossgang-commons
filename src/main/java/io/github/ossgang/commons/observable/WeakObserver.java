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

package io.github.ossgang.commons.observable;

import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A wrapper around a {@link Observer} with a weak reference. On every update, it is checked if the wrapped consumer
 * still exists; if so, the update is forwarded. If not, a cleanup method is called.
 * @param <T> the type of the wrapped consumer
 */
public class WeakObserver<T> implements Observer<T> {

    private final WeakReference<Observer<? super T>> consumer;
    private final Consumer<WeakObserver<T>> cleanUp;

    WeakObserver(Observer<? super T> parent, Consumer<WeakObserver<T>> cleanUp) {
        this.consumer = new WeakReference<>(parent);
        this.cleanUp = cleanUp;
    }

    @Override
    public void onValue(T t) {
        dispatch(Observer::onValue, t);
    }

    @Override
    public void onException(Throwable t) {
        dispatch(Observer::onException, t);
    }

    private <X> void dispatch(BiConsumer<Observer<? super T>, X> handler, X item) {
        Observer<? super T> parent = consumer.get();
        if (parent != null) {
            handler.accept(parent, item);
        } else {
            cleanUp.accept(this);
        }
    }
}
