/*
 * @formatter:off
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
 * @formatter:on
 */

package org.ossgang.commons.observables;

/**
 * * Basic {@link ValueCombinationPolicy}.
 * <br>
 * Implementation note: at the moment this class is an enum, future versions will be able to migrate to class for
 * more advanced options without breaking the compatibility.
 */
public enum ValueCombinationPolicies implements ValueCombinationPolicy {
    COMBINE_LATEST
}
