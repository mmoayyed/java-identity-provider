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

package net.shibboleth.idp.consent.flow.tou;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.context.TermsOfUseContext;
import net.shibboleth.idp.consent.flow.AbstractConsentAction;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Base class for terms of use consent actions.
 * 
 * Ensures that
 * <ul>
 * <li>the {@link ProfileInterceptorContext} is a {@link TermsOfUseContext}</li>
 * <li>the flow descriptor is a {@link TermsOfUseFlowDescriptor}</li>
 * </ul>
 * 
 * TODO details
 */
public abstract class AbstractTermsOfUseAction extends AbstractConsentAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractTermsOfUseAction.class);

    /** The terms of use context to operate on. */
    @Nullable private TermsOfUseContext termsOfUseContext;

    /** The terms of use flow descriptor. */
    @Nullable private TermsOfUseFlowDescriptor termsOfUseFlowDescriptor;
    
    /** Strategy used to find the {@link TermsOfUseContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext, TermsOfUseContext> termsOfUseContextLookupStrategy;
    
    /** Constructor. */
    public AbstractTermsOfUseAction() {
        termsOfUseContextLookupStrategy = new ChildContextLookup<>(TermsOfUseContext.class, false);
    }

    /**
     * Get the terms of use context.
     * 
     * @return the terms of use context
     */
    @Nullable public TermsOfUseContext getTermsOfUseContext() {
        return termsOfUseContext;
    }

    /**
     * Get the terms of use flow descriptor.
     * 
     * @return the terms of use flow descriptor
     */
    @Nullable public TermsOfUseFlowDescriptor getTermsOfUseFlowDescriptor() {
        return termsOfUseFlowDescriptor;
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        termsOfUseContext = termsOfUseContextLookupStrategy.apply(profileRequestContext);
        if (termsOfUseContext == null) {
            log.debug("{} Unable to locate terms of use context within profile request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        if (!(interceptorContext.getAttemptedFlow() instanceof TermsOfUseFlowDescriptor)) {
            log.debug("{} ProfileInterceptorFlowDescriptor is not an TermsOfUseFlowDescriptor", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        termsOfUseFlowDescriptor = (TermsOfUseFlowDescriptor) interceptorContext.getAttemptedFlow();

        return super.doPreExecute(profileRequestContext, interceptorContext);
    }
}
