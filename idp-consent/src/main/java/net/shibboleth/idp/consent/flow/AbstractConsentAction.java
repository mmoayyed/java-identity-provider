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

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.interceptor.AbstractProfileInterceptorAction;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * A base class for consent actions.
 * 
 * In addition to the work performed by {@link AbstractProfileInterceptorAction}, this action also looks up and makes
 * available the {@link ConsentContext}.
 * 
 * Consent action implementations should override
 * {@link #doPreExecute(ProfileRequestContext, ProfileInterceptorContext, ConsentContext)} and
 * {@link #doExecute(ProfileRequestContext, ProfileInterceptorContext, ConsentContext)}.
 */
public abstract class AbstractConsentAction extends AbstractProfileInterceptorAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractConsentAction.class);

    /** The {@link AttributeContext} to operate on. */
    @Nullable private AttributeContext attributeContext;

    /** Strategy used to find the {@link AttributeContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;

    /** The {@link ConsentContext} to operate on. */
    @Nullable private ConsentContext consentCtx;

    /** Strategy used to find the {@link ConsentContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext, ConsentContext> consentContextlookupStrategy;

    /** Constructor. */
    public AbstractConsentAction() {
        attributeContextLookupStrategy =
                Functions.compose(new ChildContextLookup<>(AttributeContext.class),
                        new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class));

        consentContextlookupStrategy = new ChildContextLookup(ConsentContext.class, false);
    }

    /**
     * Get the attribute context.
     * 
     * @return the attribute context
     */
    @Nullable public AttributeContext getAttributeContext() {
        return attributeContext;
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        attributeContext = attributeContextLookupStrategy.apply(profileRequestContext);
        log.debug("{} Found attributeContext '{}'", getLogPrefix(), attributeContext);
        if (attributeContext == null) {
            log.error("{} Unable to locate attribute context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        consentCtx = consentContextlookupStrategy.apply(profileRequestContext);
        if (consentCtx == null) {
            log.error("{} Unable to locate consent context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        return super.doPreExecute(profileRequestContext, interceptorContext)
                && super.doPreExecute(profileRequestContext);
    }

    /**
     * Performs this consent action's pre-execute step. Default implementation returns true.
     * 
     * @param profileRequestContext the current profile request context
     * @param interceptorContext the profile interceptor context
     * @param consentContext the current consent context
     * 
     * @return true iff execution should continue
     */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext, @Nonnull final ConsentContext consentContext) {
        return true;
    }

    /** {@inheritDoc} */
    @Override protected final void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {
        doExecute(profileRequestContext, interceptorContext, consentCtx);
    }

    /**
     * Performs this consent action. Default implementation does nothing.
     * 
     * @param profileRequestContext the current profile request context
     * @param interceptorContext the profile interceptor context
     * @param consentContext the current consent context
     */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext, @Nonnull final ConsentContext consentContext) {

    }

}
