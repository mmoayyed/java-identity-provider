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

package net.shibboleth.idp.saml.messaging.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.binding.impl.SAMLAddAttributeConsumingServiceHandler;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.logic.Constraint;

/**
 * Extension of OpenSAML handler that incorporates
 * {@link BrowserSSOProfileConfiguration#getRequestedAttributes(org.opensaml.profile.context.ProfileRequestContext)}.
 * 
 * @since 5.0.0
 */
public class AddAttributeConsumingServiceHandler extends SAMLAddAttributeConsumingServiceHandler {

    /** Lookup strategy for {@link ProfileRequestContext}. */
    @Nonnull private Function<MessageContext,ProfileRequestContext> profileRequestContextLookupStrategy;
    
    /** Lookup strategy for {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Constructor. */
    public AddAttributeConsumingServiceHandler() {
        profileRequestContextLookupStrategy = new ParentContextLookup<>(ProfileRequestContext.class); 
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }
    
    /**
     * Set the lookup strategy for the {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setProfileRequestContextLookupStrategy(
            @Nonnull final Function<MessageContext,ProfileRequestContext> strategy) {
        profileRequestContextLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy for the {@link RelyingPartyContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable @Unmodifiable @NotLive protected Collection<RequestedAttribute> getRequestedAttributes(
            @Nonnull final MessageContext messageContext, @Nonnull final AuthnRequest authn) {

        final ProfileRequestContext prc = profileRequestContextLookupStrategy.apply(messageContext);
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(prc);
        if (rpContext != null && rpContext.getProfileConfig() instanceof BrowserSSOProfileConfiguration sso) {
            Collection<RequestedAttribute> attrs = sso.getRequestedAttributes(prc);
            if (!attrs.isEmpty()) {
                final ArrayList<RequestedAttribute> copy = new ArrayList<>();
                copy.addAll(attrs);
                attrs = super.getRequestedAttributes(messageContext, authn);
                if (attrs != null) {
                    copy.addAll(attrs);
                }
                return copy;
            }
        }
        
        return super.getRequestedAttributes(messageContext, authn);
    }

}