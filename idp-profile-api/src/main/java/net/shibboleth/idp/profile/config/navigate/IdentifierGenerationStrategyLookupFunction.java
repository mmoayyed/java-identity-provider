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

package net.shibboleth.idp.profile.config.navigate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;

/**
 * A function that returns an {@link IdentifierGenerationStrategy} by way of a {@link RelyingPartyContext}
 * obtained via a lookup function, by default a child of the {@link ProfileRequestContext}.
 * 
 * <p>If a specific setting is unavailable, a default generator can be returned.</p>
 */
public class IdentifierGenerationStrategyLookupFunction
        implements ContextDataLookupFunction<ProfileRequestContext,IdentifierGenerationStrategy> {
    
    /** Default strategy to return. */
    @Nullable private IdentifierGenerationStrategy defaultGenerator;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Constructor. */
    public IdentifierGenerationStrategyLookupFunction() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }
    
    /**
     * Set the default {@link IdentifierGenerationStrategy} to return.
     * 
     * @param strategy  default generation strategy;
     */
    public synchronized void setDefaultIdentifierGenerationStrategy(
            @Nullable final IdentifierGenerationStrategy strategy) {
        defaultGenerator = strategy;
    }

    /**
     * Sets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public IdentifierGenerationStrategy apply(@Nullable final ProfileRequestContext input) {
        if (input != null) {
            final RelyingPartyContext rpc = relyingPartyContextLookupStrategy.apply(input);
            if (rpc != null) {
                final ProfileConfiguration pc = rpc.getProfileConfig();
                if (pc != null && pc.getSecurityConfiguration() != null) {
                    return pc.getSecurityConfiguration().getIdGenerator();
                }
            }
        }
        
        return defaultGenerator;
    }

}