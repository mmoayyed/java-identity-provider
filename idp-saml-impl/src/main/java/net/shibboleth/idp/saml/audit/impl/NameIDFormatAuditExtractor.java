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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.AuthorizationDecisionStatement;
import org.opensaml.saml.saml1.core.SubjectStatement;
import org.opensaml.saml.saml2.core.ArtifactResponse;

import com.google.common.base.Function;

import net.shibboleth.utilities.java.support.logic.Constraint;

/** {@link Function} that returns the Name Identifier Format from an assertion in a response. */
public class NameIDFormatAuditExtractor implements Function<ProfileRequestContext,String> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> responseLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public NameIDFormatAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        SAMLObject response = responseLookupStrategy.apply(input);
        if (response != null) {
            
            // Step down into ArtifactResponses.
            if (response instanceof ArtifactResponse) {
                response = ((ArtifactResponse) response).getMessage();
            }
            
            if (response instanceof org.opensaml.saml.saml2.core.Response) {
                
                for (final org.opensaml.saml.saml2.core.Assertion assertion
                        : ((org.opensaml.saml.saml2.core.Response) response).getAssertions()) {
                    if (assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
                        return assertion.getSubject().getNameID().getFormat();
                    }
                }
                
            } else if (response instanceof org.opensaml.saml.saml1.core.Response) {

                for (final org.opensaml.saml.saml1.core.Assertion assertion
                        : ((org.opensaml.saml.saml1.core.Response) response).getAssertions()) {
                    for (final AuthenticationStatement statement : assertion.getAuthenticationStatements()) {
                        if (statement.getSubject() != null && statement.getSubject().getNameIdentifier() != null) {
                            return statement.getSubject().getNameIdentifier().getFormat();
                        }
                    }
                    for (final AttributeStatement statement : assertion.getAttributeStatements()) {
                        if (statement.getSubject() != null && statement.getSubject().getNameIdentifier() != null) {
                            return statement.getSubject().getNameIdentifier().getFormat();
                        }
                    }
                    for (final AuthorizationDecisionStatement statement
                            : assertion.getAuthorizationDecisionStatements()) {
                        if (statement.getSubject() != null && statement.getSubject().getNameIdentifier() != null) {
                            return statement.getSubject().getNameIdentifier().getFormat();
                        }
                    }
                    for (final SubjectStatement statement : assertion.getSubjectStatements()) {
                        if (statement.getSubject() != null && statement.getSubject().getNameIdentifier() != null) {
                            return statement.getSubject().getNameIdentifier().getFormat();
                        }
                    }
                }
                
            }
        }
        
        return null;
    }
// Checkstyle: CyclomaticComplexity ON
    
}