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

package net.shibboleth.idp.profile.audit.impl;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;

import net.shibboleth.idp.profile.AuditExtractorFunction;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** {@link AuditExtractorFunction} that returns the result of a {@link Function} as a singleton. */
public class SingletonAuditExtractor implements AuditExtractorFunction {

    /** Function to apply. */
    @Nonnull private final Function<ProfileRequestContext,Object> singletonLookupFunction;
    
    /**
     * Constructor.
     *
     * @param fn lookup function
     */
    public SingletonAuditExtractor(@Nonnull final Function<ProfileRequestContext,Object> fn) {
        singletonLookupFunction = Constraint.isNotNull(fn, "Function cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public Collection<String> apply(@Nullable final ProfileRequestContext input) {
        final Object data = singletonLookupFunction.apply(input);
        if (data != null) {
            return Collections.singletonList(data.toString());
        } else {
            return Collections.emptyList();
        }
    }

}