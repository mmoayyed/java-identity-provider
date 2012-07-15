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

package net.shibboleth.idp.saml.impl.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.saml.common.context.SamlMetadataContext;
import org.opensaml.saml.common.context.SamlProtocolContext;
import org.opensaml.saml.criterion.EntityIdCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** Action that creates and adds a {@link SamlMetadataContext} to a {@link MessageContext}. */
@Events({@Event(id = EventIds.NO_MSG_CTX, description = "No message context was available in the request"),
        @Event(id = EventIds.NO_MSG_MD, description = "No message metadata available in message context")})
public class AddSamlMetadataToMessageContext extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddSamlMetadataToMessageContext.class);

    /** Resolver used to look up SAML metadata. */
    private final Resolver<EntityDescriptor, CriteriaSet> metadataResolver;

    /** Strategy used to lookup the {@link MessageContext}. */
    private Function<ProfileRequestContext, MessageContext> messageContextLookupStrategy;

    /**
     * Constructor. Initializes {@link #messageContextLookupStrategy} to {@link ChildContextLookup}.
     * 
     * @param resolver resolver used to look up SAML metadata
     */
    public AddSamlMetadataToMessageContext(@Nonnull final Resolver<EntityDescriptor, CriteriaSet> resolver) {
        super();

        metadataResolver = Constraint.isNotNull(resolver, "Metadata resolver can not be null");

        messageContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, MessageContext>(MessageContext.class, false);
    }

    /**
     * Gets the resolver used to look up SAML metadata.
     * 
     * @return resolver used to look up SAML metadata
     */
    @Nonnull public Resolver<EntityDescriptor, CriteriaSet> getMetadataResolver() {
        return metadataResolver;
    }

    /**
     * Gets the strategy used to lookup the {@link MessageContext}.
     * 
     * @return strategy used to lookup the {@link MessageContext}
     */
    @Nonnull public Function<ProfileRequestContext, MessageContext> getMessageContextLookupStrategy() {
        return messageContextLookupStrategy;
    }

    /**
     * Sets the strategy used to lookup the {@link MessageContext}.
     * 
     * @param strategy strategy used to lookup the {@link MessageContext}
     */
    public synchronized void setMessageContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, MessageContext> strategy) {
        messageContextLookupStrategy =
                Constraint.isNotNull(strategy, "Message context lookup strategy can not be null");
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nullable final HttpServletRequest httpRequest,
            @Nullable final HttpServletResponse httpResponse, @Nullable final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final MessageContext messageCtx = messageContextLookupStrategy.apply(profileRequestContext);
        if (messageCtx == null) {
            log.debug("Action {}: appropriate message context not available", getId());
            return ActionSupport.buildEvent(this, EventIds.NO_MSG_CTX);
        }

        final BasicMessageMetadataContext msgMetadataCtx =
                messageCtx.getSubcontext(BasicMessageMetadataContext.class, false);
        if (msgMetadataCtx == null) {
            log.debug("Action {}: message context did not contain basic message metadata", getId());
            return ActionSupport.buildEvent(this, EventIds.NO_MSG_MD);
        }

        final EntityIdCriterion entityIdCriterion = new EntityIdCriterion(msgMetadataCtx.getMessageIssuer());

        final SamlProtocolContext protocolCtx = messageCtx.getSubcontext(SamlProtocolContext.class, false);
        ProtocolCriterion protocolCriterion = null;
        EntityRoleCriterion roleCriterion = null;
        if (protocolCtx != null) {
            if (protocolCtx.getProtocol() != null) {
                protocolCriterion = new ProtocolCriterion(protocolCtx.getProtocol());
            }
            if (protocolCtx.getRole() != null) {
                roleCriterion = new EntityRoleCriterion(protocolCtx.getRole());
            }
        }

        final CriteriaSet criteria = new CriteriaSet(entityIdCriterion, protocolCriterion, roleCriterion);
        try {
            final EntityDescriptor entityMetadata = metadataResolver.resolveSingle(criteria);

            final SamlMetadataContext metadataCtx = new SamlMetadataContext();
            metadataCtx.setEntityDescriptor(entityMetadata);
            // TODO need to look up role descriptor
            // metadataCtx.setRoleDescriptor(descriptor);

            messageCtx.addSubcontext(metadataCtx);

            log.debug("Action {}: populated {} added to MessageContext.", getId(), SamlMetadataContext.class.getName());
            return ActionSupport.buildProceedEvent(this);
        } catch (ResolverException e) {
            // TODO should this error out the request or continue on?
            return null;
        }
    }
}