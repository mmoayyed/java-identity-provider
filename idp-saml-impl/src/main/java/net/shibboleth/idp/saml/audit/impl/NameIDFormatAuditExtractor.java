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
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.AuthorizationDecisionStatement;
import org.opensaml.saml.saml1.core.SubjectStatement;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.LogoutRequest;

import net.shibboleth.shared.logic.Constraint;

/** {@link Function} that returns the Name Identifier Format from a SAML Subject. */
public class NameIDFormatAuditExtractor implements Function<ProfileRequestContext,String> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> messageLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public NameIDFormatAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        messageLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        SAMLObject msg = messageLookupStrategy.apply(input);
        if (msg != null) {
            
            // Step down into ArtifactResponses.
            if (msg instanceof ArtifactResponse) {
                msg = ((ArtifactResponse) msg).getMessage();
            }
            
            if (msg instanceof org.opensaml.saml.saml2.core.Response) {
                
                for (final org.opensaml.saml.saml2.core.Assertion assertion
                        : ((org.opensaml.saml.saml2.core.Response) msg).getAssertions()) {
                    assert assertion != null;
                    final String format = apply(assertion);
                    if (format != null) {
                        return format;
                    }
                }
                
            } else if (msg instanceof LogoutRequest) {
                
                if (((LogoutRequest) msg).getNameID() != null) {
                    return ((LogoutRequest) msg).getNameID().getFormat();
                }

            } else if (msg instanceof AuthnRequest) {
                
                if (((AuthnRequest) msg).getSubject() != null &&
                        ((AuthnRequest) msg).getSubject().getNameID() != null) {
                    return ((AuthnRequest) msg).getSubject().getNameID().getFormat();
                }
                
            } else if (msg instanceof org.opensaml.saml.saml1.core.Response) {

                for (final org.opensaml.saml.saml1.core.Assertion assertion
                        : ((org.opensaml.saml.saml1.core.Response) msg).getAssertions()) {
                    assert assertion != null;
                    final String format = apply(assertion);
                    if (format != null) {
                        return format;
                    }
                }
            } else if (msg instanceof org.opensaml.saml.saml2.core.SubjectQuery) {

                if (((org.opensaml.saml.saml2.core.SubjectQuery) msg).getSubject() != null &&
                        ((org.opensaml.saml.saml2.core.SubjectQuery) msg).getSubject().getNameID() != null) {
                    return ((org.opensaml.saml.saml2.core.SubjectQuery) msg).getSubject().getNameID().getFormat();
                }
            } else if (msg instanceof org.opensaml.saml.saml1.core.SubjectQuery) {

                if (((org.opensaml.saml.saml1.core.SubjectQuery) msg).getSubject() != null &&
                        ((org.opensaml.saml.saml1.core.SubjectQuery) msg).getSubject().getNameIdentifier() != null) {
                    return ((org.opensaml.saml.saml1.core.SubjectQuery)
                            msg).getSubject().getNameIdentifier().getFormat();
                }
                
            } else if (msg instanceof org.opensaml.saml.saml2.core.Assertion) {
                return apply((org.opensaml.saml.saml2.core.Assertion) msg);
            } else if (msg instanceof org.opensaml.saml.saml1.core.Assertion) {
                return apply((org.opensaml.saml.saml1.core.Assertion) msg);
            }
        }
        
        return null;
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON

    /**
     * Apply function to an assertion.
     * 
     * @param assertion assertion to operate on
     * 
     * @return the format, or null
     */
    @Nullable private String apply(@Nonnull final org.opensaml.saml.saml2.core.Assertion assertion) {
        if (assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
            return assertion.getSubject().getNameID().getFormat();
        }
        return null;
    }

// Checkstyle: CyclomaticComplexity OFF
    /**
     * Apply function to an assertion.
     * 
     * @param assertion assertion to operate on
     * 
     * @return the format, or null
     */
    @Nullable private String apply(@Nonnull final org.opensaml.saml.saml1.core.Assertion assertion) {

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
        
        return null;
    }
// Checkstyle: CyclomaticComplexity ON

}