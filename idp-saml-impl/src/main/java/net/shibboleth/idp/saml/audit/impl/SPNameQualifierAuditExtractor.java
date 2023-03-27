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
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectQuery;

import net.shibboleth.shared.logic.Constraint;

/** {@link Function} that returns the SPNameQualifier from a SAML Subject. */
public class SPNameQualifierAuditExtractor implements Function<ProfileRequestContext,String> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> messageLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public SPNameQualifierAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        messageLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        SAMLObject msg = messageLookupStrategy.apply(input);
        if (msg != null) {
            
            // Step down into ArtifactResponses.
            if (msg instanceof ArtifactResponse ar) {
                msg = ar.getMessage();
            }
            
            if (msg instanceof Response resp) {
                for (final Assertion assertion : resp.getAssertions()) {
                    assert assertion != null;
                    final String qualifier = apply(assertion);
                    if (qualifier != null) {
                        return qualifier;
                    }
                }
            } else if (msg instanceof LogoutRequest logout) {
                final NameID nameID = logout.getNameID();
                if (nameID != null) {
                    return nameID.getSPNameQualifier();
                }

            } else if (msg instanceof AuthnRequest ar) {
                final Subject subject = ar.getSubject();
                if (subject != null) {
                    final NameID nameID = subject.getNameID();
                    if (nameID != null) {
                        return nameID.getSPNameQualifier();
                    }
                }
            } else if (msg instanceof SubjectQuery q) {
                final Subject subject = q.getSubject();
                if (subject != null) {
                    final NameID nameID = subject.getNameID();
                    if (nameID != null) {
                        return nameID.getSPNameQualifier();
                    }
                }
            } else if (msg instanceof Assertion a) {
                return apply(a);
            }
        }
        
        return null;
    }
// Checkstyle: CyclomaticComplexity ON

    /**
     * Apply function to an assertion.
     * 
     * @param assertion assertion to operate on
     * 
     * @return the format, or null
     */
    @Nullable private String apply(@Nonnull final Assertion assertion) {
        final Subject subject = assertion.getSubject();
        if (subject != null) {
            final NameID nameID = subject.getNameID();
            if (nameID != null) {
                return nameID.getFormat();
            }
        }
        return null;
    }

}