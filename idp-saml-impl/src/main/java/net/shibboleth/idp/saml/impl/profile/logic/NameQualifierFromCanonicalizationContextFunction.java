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

package net.shibboleth.idp.saml.impl.profile.logic;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml2.core.NameID;

import com.google.common.base.Function;

/**
 * A lookup function that accesses a {@link NameID} or {@link NameIdentifier} from the target of a
 * {@link SubjectCanonicalizationContext} and returns the NameQualifier attribute, if present.
 */
public class NameQualifierFromCanonicalizationContextFunction implements Function<ProfileRequestContext, String> {

    /** Lookup strategy for context to read from. */
    @Nonnull
    private Function<ProfileRequestContext,SubjectCanonicalizationContext> subjectCanonicalizationContextLookupStrategy;
    
    /** Constructor. */
    public NameQualifierFromCanonicalizationContextFunction() {
        subjectCanonicalizationContextLookupStrategy = new ChildContextLookup<>(SubjectCanonicalizationContext.class);
    }
    
    /**
     * Set the lookup strategy to use to locate the SubjectCanonicalizationContext to read from.
     * 
     * @param strategy  lookup strategy
     */
    public void setSubjectCanonicalizationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectCanonicalizationContext> strategy) {
        subjectCanonicalizationContextLookupStrategy = Constraint.isNotNull(strategy,
                "Lookup strategy for SubjectCanonicalizationContext cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        final SubjectCanonicalizationContext c14nCtx = subjectCanonicalizationContextLookupStrategy.apply(input);
        if (c14nCtx != null && c14nCtx.getSubject() != null) {
            final Set<NameIDPrincipal> nameIds = c14nCtx.getSubject().getPrincipals(NameIDPrincipal.class);
            if (nameIds != null && nameIds.size() == 1) {
                return apply(nameIds.iterator().next().getNameID());
            }
            
            final Set<NameIdentifierPrincipal> nameIdentifiers =
                    c14nCtx.getSubject().getPrincipals(NameIdentifierPrincipal.class);
            if (nameIdentifiers != null && nameIdentifiers.size() == 1) {
                return apply(nameIdentifiers.iterator().next().getNameIdentifier());
            }
        }
        
        return null;
    }

    /**
     * Return the relevant value from the target object.
     * 
     * @param id   target object
     * 
     * @return the relevant value
     */
    @Nullable protected String apply(@Nonnull final NameID id) {
        return id.getNameQualifier();
    }
    
    /**
     * Return the relevant value from the target object.
     * 
     * @param id   target object
     * 
     * @return the relevant value
     */
    @Nullable protected String apply(@Nonnull final NameIdentifier id) {
        return id.getNameQualifier();
    }
    
}