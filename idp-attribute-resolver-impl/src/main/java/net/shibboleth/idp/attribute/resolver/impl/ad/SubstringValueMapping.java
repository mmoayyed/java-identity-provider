/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.impl.ad;

import net.shibboleth.idp.attribute.resolver.impl.ad.mapped.ValueMapping;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Optional;

/**
 * A {@link ValueMapping} function that checks if an input contains a particular substring and, if so, returns a static
 * value.
 */
public class SubstringValueMapping implements ValueMapping {

    /** The string that a value must contain. */
    private final String target;

    /** Whether to perform a case-insensitive comparison between the target and the input. */
    private boolean ignoreCase;

    /** The value returned if the input value contains the target. */
    private final Optional<String> result;

    /**
     * Constructor.
     * 
     * @param targetValue string that an input value must contain
     * @param caseInsensitive whether to perform a case-insensitive comparison between the target and the input
     * @param returnValue value returned if the input value contains the target
     */
    public SubstringValueMapping(final String targetValue, boolean caseInsensitive, String returnValue) {
        ignoreCase = caseInsensitive;

        String trimmedTarget =
                Constraint.isNotNull(StringSupport.trimOrNull(targetValue), "Target value can not be null or empty");
        if (ignoreCase) {
            target = trimmedTarget.toUpperCase();
        } else {
            target = trimmedTarget;
        }

        result =
                Optional.of(Constraint.isNotNull(StringSupport.trimOrNull(returnValue),
                        "Return value can not be null or empty"));
    }

    /** {@inheritDoc} */
    public Optional<String> apply(String input) {
        String source = input;
        if (ignoreCase) {
            source = source.toUpperCase();
        }

        if (input.contains(target)) {
            return result;
        }

        return Optional.absent();
    }
}