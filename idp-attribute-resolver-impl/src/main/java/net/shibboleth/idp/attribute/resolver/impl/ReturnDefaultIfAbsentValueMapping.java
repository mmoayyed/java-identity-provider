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

package net.shibboleth.idp.attribute.resolver.impl;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Optional;

/**
 * A {@Link ValueMapping} that returns a default value if the wrapped {@link ValueMapping} returns
 * {@link Optional#absent()}.
 */
public class ReturnDefaultIfAbsentValueMapping implements ValueMapping {

    /** Value mapping function composed with this function. */
    private final ValueMapping composedFunction;

    /** The result returned if the composed function returns an {@link Optional#absent()}. */
    private final Optional<String> result;

    /**
     * Constructor.
     * 
     * @param function value mapping function composed with this function
     * @param returnValue result returned if the composed function returns an {@link Optional#absent()}
     */
    public ReturnDefaultIfAbsentValueMapping(@Nonnull ValueMapping function, @Nonnull @NotEmpty String returnValue) {
        composedFunction = Assert.isNotNull(function, "Composed value mapping function can not be null");
        result =
                Optional.of(Assert.isNull(StringSupport.trimOrNull(returnValue),
                        "Return value can not be null or empty"));
    }

    /** {@inheritDoc} */
    public Optional<String> apply(String input) {
        Optional<String> optionalResult = composedFunction.apply(input);
        if (optionalResult.isPresent()) {
            return optionalResult;
        } else {
            return result;
        }
    }
}