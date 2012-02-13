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

package net.shibboleth.idp.attribute.resolver.impl.ad.mapped;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Assert;

import com.google.common.base.Optional;

/**
 * A {@Link ValueMapping} that returns the input value if the wrapped {@link ValueMapping} returns
 * {@link Optional#absent()}.
 */
public class ReturnInputIfAbsentMappingValue implements ValueMapping {

    /** Value mapping function composed with this function. */
    private ValueMapping composedFunction;

    /**
     * Constructor.
     * 
     * @param function value mapping function composed with this function
     */
    public ReturnInputIfAbsentMappingValue(@Nonnull ValueMapping function) {
        composedFunction = Assert.isNotNull(function, "Composed value mapping function can not be null");
    }

    /** {@inheritDoc} */
    public Optional<String> apply(String input) {
        Optional<String> optionalResult = composedFunction.apply(input);
        if (optionalResult.isPresent()) {
            return optionalResult;
        } else {
            return Optional.of(input);
        }
    }
}