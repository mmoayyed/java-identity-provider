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

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Message handler implementation that enforces the AuthnRequestsSigned flag of 
 * SAML 2 metadata element @{link {@link SPSSODescriptor} and/or a local profile
 * configuration option.
 */
public class SAML2AuthnRequestsSignedSecurityHandler
        extends org.opensaml.saml.saml2.binding.security.impl.SAML2AuthnRequestsSignedSecurityHandler{
    
    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2AuthnRequestsSignedSecurityHandler.class);

    /** Lookup strategy for relying party context. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Constructor. */
    public SAML2AuthnRequestsSignedSecurityHandler() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean isRequestSigningRequired(@Nonnull final MessageContext messageContext) {
        if (super.isRequestSigningRequired(messageContext)) {
            return true;
        }

        if (messageContext.getParent() instanceof ProfileRequestContext) {
            final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(
                    (ProfileRequestContext) messageContext.getParent());
            if (rpCtx != null && rpCtx.getProfileConfig() instanceof BrowserSSOProfileConfiguration) {
                return ((BrowserSSOProfileConfiguration) rpCtx.getProfileConfig()).isRequireSignedRequests(
                        (ProfileRequestContext) messageContext.getParent());
            }
        }
        
        log.warn("Unable to locate profile configuration in context tree");
        return false;
    }
    
}