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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.saml.nameid.AbstractSAML2NameIDGenerator;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Generator for transient {@link NameID} objects.
 */
@ThreadSafeAfterInit
public class TransientSAML2NameIDGenerator extends AbstractSAML2NameIDGenerator {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TransientSAML2NameIDGenerator.class);

    /** Strategy function to lookup SubjectContext. */
    @Nonnull private Function<ProfileRequestContext, SubjectContext> subjectContextLookupStrategy;

    /** Generator for transients. */
    @NonnullAfterInit private TransientIdGenerationStrategy transientIdGenerator;

    /** Constructor. */
    public TransientSAML2NameIDGenerator() {
        setFormat(NameID.TRANSIENT);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        setDefaultIdPNameQualifierLookupStrategy(new ResponderIdLookupFunction());
        setDefaultSPNameQualifierLookupStrategy(new RelyingPartyIdLookupFunction());
    }

    /**
     * Set the lookup strategy to use to locate the {@link SubjectContext}.
     * 
     * @param strategy lookup function to use
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        checkSetterPreconditions();
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Set the generator of transient IDs.
     * 
     * @param generator transient ID generator
     */
    public void setTransientIdGenerator(@Nonnull final TransientIdGenerationStrategy generator) {
        checkSetterPreconditions();
        transientIdGenerator = Constraint.isNotNull(generator, "TransientIdGenerationStrategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (transientIdGenerator == null) {
            throw new ComponentInitializationException("TransientIdGenerationStrategy cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable protected String getIdentifier(@Nonnull final ProfileRequestContext profileRequestContext)
            throws SAMLException {

        // Effective qualifier may override default in the case of an Affiliation.
        // This doesn't really impact transients typically, but for consistency...
        String relyingPartyId = getEffectiveSPNameQualifier(profileRequestContext);
        if (relyingPartyId == null) {
            final Function<ProfileRequestContext, String> lookup = getDefaultSPNameQualifierLookupStrategy();
            relyingPartyId = lookup != null ? lookup.apply(profileRequestContext) : null;
        }
        if (relyingPartyId == null) {
            log.debug("No relying party identifier available, can't generate transient ID");
            return null;
        }

        final SubjectContext subjectCtx = subjectContextLookupStrategy.apply(profileRequestContext);
        final String principalName = subjectCtx == null ? null : subjectCtx.getPrincipalName();
        if (principalName == null) {
            log.debug("No principal name available, can't generate transient ID");
            return null;
        }

        try {
            return transientIdGenerator.generate(relyingPartyId, principalName);
        } catch (final SAMLException e) {
            log.debug("Exception generating transient ID", e);
            return null;
        }
    }

}