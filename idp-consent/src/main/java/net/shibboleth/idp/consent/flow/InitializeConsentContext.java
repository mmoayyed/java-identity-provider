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

package net.shibboleth.idp.consent.flow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.AbstractProfileInterceptorAction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that creates a {@link ConsentContext} and attaches it to the current {@link ProfileRequestContext}.
 */
public class InitializeConsentContext extends AbstractProfileInterceptorAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeConsentContext.class);

    /** Consent flow descriptor. */
    @Nullable private ConsentFlowDescriptor consentFlowDescriptor;

    /**
     * Set the consent flow descriptor.
     * 
     * @param descriptor the consent flow descriptor
     */
    public void setConsentFlowDescriptor(@Nonnull final ConsentFlowDescriptor descriptor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        consentFlowDescriptor = Constraint.isNotNull(descriptor, "ConsentFlowDescriptor cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (consentFlowDescriptor == null) {
            // TODO Is this the correct event to return ?
            log.warn("{} A consent flow descriptor must be configured", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }

        return super.doPreExecute(profileRequestContext, interceptorContext);
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final ConsentContext consentContext = new ConsentContext();
        consentContext.setConsentFlowDescriptor(consentFlowDescriptor);

        profileRequestContext.addSubcontext(consentContext, true);

        log.debug("{} Created consent context {}", getLogPrefix(), consentContext);

        super.doExecute(profileRequestContext, interceptorContext);
    }
}
