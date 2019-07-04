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

/**
 * An option which can be passed to {@link Observable#subscribe(Observer, SubscriptionOption...)}. The available options
 * are implementation specific and should be exposed in the concrete interface of the implementation.
 *
 * Implementations are expected to ignore options they don't support, and pass the set of options through to a
 * parent class, if any.
 */
public interface SubscriptionOption {
    String name();
}
