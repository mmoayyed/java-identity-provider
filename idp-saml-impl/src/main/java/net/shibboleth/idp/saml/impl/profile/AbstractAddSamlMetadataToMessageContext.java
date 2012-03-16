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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.profile.SamlMetadataContext;
import net.shibboleth.idp.saml.profile.SamlProtocolContext;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.criterion.EntityIdCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Base class for actions which add a populated {@link SamlMetadataContext} to a given {@link MessageContext}. */
public abstract class AbstractAddSamlMetadataToMessageContext extends AbstractIdentityProviderAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractAddSamlMetadataToMessageContext.class);

    /** Resolver used to look up SAML metadata. */
    private Resolver<EntityDescriptor, CriteriaSet> metadataResolver;

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext, final ProfileRequestContext profileRequestContext)
            throws ProfileException {
        MessageContext messageCtx = getMessageContext(profileRequestContext);
        if (messageCtx == null) {
            log.debug("Action {}: appropriate message context not available, skipping this action.", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        BasicMessageMetadataContext msgMetadataCtx = messageCtx.getSubcontext(BasicMessageMetadataContext.class, false);
        if (msgMetadataCtx == null) {
            log.debug("Action {}: message context did not contain basic message metadata, skipping this action.",
                    getId());
            return ActionSupport.buildProceedEvent(this);
        }
        EntityIdCriterion entityIdCriterion = new EntityIdCriterion(msgMetadataCtx.getMessageIssuer());

        SamlProtocolContext protocolCtx = messageCtx.getSubcontext(SamlProtocolContext.class, false);
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

        CriteriaSet criteria = new CriteriaSet(entityIdCriterion, protocolCriterion, roleCriterion);
        try {
            EntityDescriptor entityMetadata = metadataResolver.resolveSingle(criteria);

            SamlMetadataContext metadataCtx = new SamlMetadataContext();
            metadataCtx.setEntityDescriptor(entityMetadata);
            // TODO metadataCtx.setRoleDescriptor(descriptor);

            messageCtx.addSubcontext(metadataCtx);

            log.debug("Action {}: populated {} added to MessageContext.", getId(), SamlMetadataContext.class.getName());
            return ActionSupport.buildProceedEvent(this);
        } catch (ResolverException e) {
            // TODO should this error out the request or continue on?
            return null;
        }
    }

    /**
     * Gets the message context to which the {@link SamlMetadataContext} will be added. If the returned value is null
     * this action will simply complete with an {@link ActionSupport#PROCEED_EVENT_ID} event.
     * 
     * @param profileRequestContext current request context
     * 
     * @return the message context to which the {@link SamlMetadataContext} will be added, may be null
     */
    protected abstract MessageContext getMessageContext(ProfileRequestContext profileRequestContext);
}