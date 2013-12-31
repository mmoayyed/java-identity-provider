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

package net.shibboleth.idp.session.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.Function;

/**
 * A function to create a {@link BasicSPSession} based on profile execution state.
 * 
 * <p>This strategy is a default approach that uses the inbound message's {@link BasicMessageMetadataContext}
 * to obtain an issuer value, used as the {@link SPSession}'s relying party ID. The authentication flow ID
 * comes from the {@link AuthenticationResult} in the {@link AuthenticationContext}. The session has a 
 * creation time based on the time of execution, and the expiration is based on a configurable lifetime.</p> 
 */
public class BasicSPSessionCreationStrategy implements Function<ProfileRequestContext, SPSession> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BasicSPSessionCreationStrategy.class);
    
    /** Lifetime of sessions to create. */
    @Positive @Duration private final long sessionLifetime;
    
    /**
     * Constructor.
     * 
     * @param lifetime lifetime in milliseconds, determines expiration of {@link SPSession} to be created
     */
    public BasicSPSessionCreationStrategy(@Positive @Duration final long lifetime) {
        sessionLifetime = Constraint.isGreaterThan(0, lifetime, "Lifetime must be greater than 0");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public SPSession apply(@Nullable final ProfileRequestContext input) {
        
        if (input.getInboundMessageContext() == null) {
            log.debug("No inbound MessageContext, no SPSession created");
            return null;
        }
        
        final BasicMessageMetadataContext mdCtx =
                input.getInboundMessageContext().getSubcontext(BasicMessageMetadataContext.class, false);
        if (mdCtx == null || mdCtx.getMessageIssuer() == null) {
            log.debug("No message issuer found in inbound BasicMessageMetadataContext, no SPSession created");
            return null;
        }
        
        final AuthenticationContext authCtx = input.getSubcontext(AuthenticationContext.class, false);
        if (authCtx == null || authCtx.getAuthenticationResult() == null) {
            log.debug("No AuthenticationResult found in AuthenticationContext, no SPSession created");
            return null;
        }
        
        final long now = System.currentTimeMillis();
        return new BasicSPSession(mdCtx.getMessageIssuer(), authCtx.getAuthenticationResult().getAuthenticationFlowId(),
                now, now + sessionLifetime);
    }

}