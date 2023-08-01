/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLSelfEntityContext;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.logic.Constraint;

/**
 * Action that adds a {@link SAMLSelfEntityContext} to the inbound {@link MessageContext}
 * 
 * <p>
 * The {@link SAMLSelfEntityContext} is populated based on the identity of the IdP, as derived by a lookup strategy.
 * </p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_PROFILE_CTX}
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class PopulateInboundMessageContextWithSAMLSelf extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateInboundMessageContextWithSAMLSelf.class);

    /** Strategy used to obtain the self identity value. */
    @Nonnull private Function<ProfileRequestContext, String> selfIdentityLookupStrategy;

    /** Constructor. */
    public PopulateInboundMessageContextWithSAMLSelf() {
        selfIdentityLookupStrategy = new IssuerLookupFunction();
    }

    /**
     * Set the strategy used to locate the self identity value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setSelfIdentityLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        selfIdentityLookupStrategy = Constraint.isNotNull(strategy, "Self identity lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final SAMLSelfEntityContext selfContext =
                profileRequestContext.ensureInboundMessageContext().ensureSubcontext(SAMLSelfEntityContext.class);
        selfContext.setEntityId(selfIdentityLookupStrategy.apply(profileRequestContext));

        log.debug("{} Populated inbound message context with SAML self entityID: {}", getLogPrefix(),
                selfContext.getEntityId());
        
        if (selfContext.getEntityId() == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
        }
        
    }

}