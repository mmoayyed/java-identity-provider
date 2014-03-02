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

package net.shibboleth.idp.saml.impl.nameid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.profile.AbstractSAML1NameIdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Generator for transient {@link NameIdentifier} objects.
 */
@ThreadSafeAfterInit
public class TransientSAML1NameIdentifierGenerator extends AbstractSAML1NameIdentifierGenerator {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TransientSAML1NameIdentifierGenerator.class);
    
    /** Strategy function to lookup RelyingPartyContext. */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Strategy function to lookup SubjectContext. */
    @Nonnull private Function<ProfileRequestContext, SubjectContext> subjectContextLookupStrategy;
    
    /** Generator for transients. */
    @NonnullAfterInit private TransientIdGenerationStrategy transientIdGenerator;
    
    /** Constructor. */
    public TransientSAML1NameIdentifierGenerator() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }
    
    /**
     * Set the lookup strategy to use to locate the {@link RelyingPartyContext}.
     * 
     * @param strategy lookup function to use
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy to use to locate the {@link SubjectContext}.
     * 
     * @param strategy lookup function to use
     */
    public synchronized void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        subjectContextLookupStrategy = Constraint.isNotNull(strategy,
                "SubjectContext lookup strategy cannot be null");
    }
    
    /**
     * Set the generator of transient IDs.
     * 
     * @param generator transient ID generator
     */
    public synchronized void setTransientIdGenerator(@Nonnull final TransientIdGenerationStrategy generator) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        transientIdGenerator = Constraint.isNotNull(generator, "TransientIdGenerationStrategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (transientIdGenerator == null) {
            throw new ComponentInitializationException("TransientIdGenerationStrategy cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getIdentifier(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {
        
        final String relyingPartyId = this.getDefaultSPNameQualifier(profileRequestContext);
        if (relyingPartyId == null) {
            log.debug("No relying party identifier available, can't generate transient ID");
            return null;
        }
        
        final SubjectContext subjectCtx = subjectContextLookupStrategy.apply(profileRequestContext);
        if (subjectCtx == null || subjectCtx.getPrincipalName() == null) {
            log.debug("No principal name available, can't generate transient ID");
            return null;
        }
        
        try {
            return transientIdGenerator.generate(relyingPartyId, subjectCtx.getPrincipalName());
        } catch (final ProfileException e) {
            log.debug("Exception generating transient ID", e);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getDefaultIdPNameQualifier(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx != null) {
            final RelyingPartyConfiguration rpConfig = rpCtx.getConfiguration();
            if (rpConfig != null) {
                return rpConfig.getResponderId();
            }
        }
        
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getDefaultSPNameQualifier(@Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx != null) {
            return rpCtx.getRelyingPartyId();
        }
        
        return null;
    }

}