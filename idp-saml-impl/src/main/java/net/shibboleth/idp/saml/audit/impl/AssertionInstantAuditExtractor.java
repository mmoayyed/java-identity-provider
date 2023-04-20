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

import java.time.Instant;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.ArtifactResponse;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;

/** {@link Function} that returns the IssueInstant attribute from the assertions in a response. */
public class AssertionInstantAuditExtractor implements Function<ProfileRequestContext,Collection<Instant>> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> responseLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public AssertionInstantAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Nullable public Collection<Instant> apply(@Nullable final ProfileRequestContext input) {
        SAMLObject message = responseLookupStrategy.apply(input);
        if (message != null) {
            
            // Step down into ArtifactResponses.
            if (message instanceof ArtifactResponse) {
                message = ((ArtifactResponse) message).getMessage();
            }
            
            if (message instanceof org.opensaml.saml.saml2.core.Response resp) {
                
                final var assertions = resp.getAssertions();
                if (!assertions.isEmpty()) {
                    return assertions.
                            stream().
                            map(org.opensaml.saml.saml2.core.Assertion::getIssueInstant).
                            collect(Collectors.toList());
                }
                
            } else if (message instanceof org.opensaml.saml.saml1.core.Response resp) {

                final var assertions = resp.getAssertions();
                if (!assertions.isEmpty()) {
                    return assertions.
                            stream().
                            map(org.opensaml.saml.saml1.core.Assertion::getIssueInstant).
                            collect(Collectors.toList());
                }
                
            } else if (message instanceof org.opensaml.saml.saml2.core.Assertion a) {
                final Instant ts = a.getIssueInstant();
                if (ts != null) {
                    return CollectionSupport.singletonList(ts);
                }
            } else if (message instanceof org.opensaml.saml.saml1.core.Assertion a) {
                final Instant ts = a.getIssueInstant();
                if (ts != null) {
                    return CollectionSupport.singletonList(ts);
                }
            }
        }
        
        return CollectionSupport.emptyList();
    }
// Checkstyle: CyclomaticComplexity ON

}