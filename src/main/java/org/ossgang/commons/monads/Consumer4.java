/*
 *
 * This file is part of ossgang-commons.
 *
 * Copyright (c) 2008-2020, CERN. All rights reserved.
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
 */

package org.ossgang.commons.monads;

/**
 * Represents a consumer that accepts 4 arguments.
 * This is a specialization of standard Java {@link java.util.function.Consumer}.
 *
 * @param <I1> the first argument type
 * @param <I2> the second argument type
 * @param <I3> the third argument type
 * @param <I4> the fourth argument type
 */
public interface Consumer4<I1, I2, I3, I4> {

    /**
     * Apply this consumer to the given arguments.
     *
     * @param input1 the first argument
     * @param input2 the second argument
     * @param input3 the third argument
     * @param input4 the fourth argument
     */
    void accept(I1 input1, I2 input2, I3 input3, I4 input4);
}
