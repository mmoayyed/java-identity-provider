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

package net.shibboleth.idp.saml.audit.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.ProxyRestriction;
import org.opensaml.saml.saml2.core.Response;

import net.shibboleth.shared.logic.Constraint;

/**
 * Base class for {@link Function} that returns content from the {@link ProxyRestriction} element.
 * 
 * @param <T> type of field being extracted
 * 
 * @since 4.2.0
 */
public abstract class AbstractProxyRestrictionAuditExtractor<T> implements Function<ProfileRequestContext,T> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> responseLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    protected AbstractProxyRestrictionAuditExtractor(
            @Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public T apply(@Nullable final ProfileRequestContext input) {
        SAMLObject response = responseLookupStrategy.apply(input);
        if (response != null) {
            
            // Step down into ArtifactResponses.
            if (response instanceof ArtifactResponse) {
                response = ((ArtifactResponse) response).getMessage();
            }
            
            if (response instanceof Response) {
                for (final Assertion assertion : ((Response) response).getAssertions()) {
                    final Conditions conditions = assertion.getConditions();
                    if (conditions != null) {
                        final ProxyRestriction condition = conditions.getProxyRestriction();
                        if (condition != null) {
                            return doApply(condition);
                        }
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Override point to do the extraction.
     * 
     * @param condition the input object
     * 
     * @return the extracted value
     */
    @Nullable protected abstract T doApply(@Nullable final ProxyRestriction condition);
    
}