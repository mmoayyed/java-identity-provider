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

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.profile.SamlMetadataSubcontext;
import net.shibboleth.idp.saml.profile.SamlProtocolSubcontext;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.criterion.EntityIdCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.util.criteria.CriteriaSet;
import org.opensaml.util.resolver.Resolver;
import org.opensaml.util.resolver.ResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Base class for actions which add a populated {@link SamlMetadataSubcontext} to a given {@link MessageContext}. */
public abstract class AbstractAddSamlMetadataToMessageContext extends AbstractIdentityProviderAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractAddSamlMetadataToMessageContext.class);

    /** Resolver used to look up SAML metadata. */
    private Resolver<EntityDescriptor, CriteriaSet> metadataResolver;

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext) {
        MessageContext messageCtx = getMessageContext(profileRequestContext);
        if (messageCtx == null) {
            log.debug("Action {}: appropriate message context not available, skipping this action.", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        BasicMessageMetadataSubcontext msgMetadataCtx =
                messageCtx.getSubcontext(BasicMessageMetadataSubcontext.class, false);
        if (msgMetadataCtx == null) {
            log.debug("Action {}: message context did not contain basic message metadata, skipping this action.",
                    getId());
            return ActionSupport.buildProceedEvent(this);
        }
        EntityIdCriterion entiryIdCriterion = new EntityIdCriterion(msgMetadataCtx.getMessageIssuer());

        SamlProtocolSubcontext protocolCtx = messageCtx.getSubcontext(SamlProtocolSubcontext.class, false);
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

        CriteriaSet criteria = new CriteriaSet(entiryIdCriterion, protocolCriterion, roleCriterion);
        try {
            EntityDescriptor entityMetadata = metadataResolver.resolveSingle(criteria);

            SamlMetadataSubcontext metadataCtx = new SamlMetadataSubcontext(messageCtx);
            metadataCtx.setEntityDescriptor(entityMetadata);
            // TODO metadataCtx.setRoleDescriptor(descriptor);

            log.debug("Action {}: populated {} added to MessageContext.", getId(),
                    SamlMetadataSubcontext.class.getName());
            return ActionSupport.buildProceedEvent(this);
        } catch (ResolverException e) {
            // TODO should this error out the request or continue on?
            return null;
        }
    }

    /**
     * Gets the message context to which the {@link SamlMetadataSubcontext} will be added. If the returned value is null
     * this action will simply complete with an {@link ActionSupport#PROCEED_EVENT_ID} event.
     * 
     * @param profileRequestContext current request context
     * 
     * @return the message context to which the {@link SamlMetadataSubcontext} will be added, may be null
     */
    protected abstract MessageContext getMessageContext(ProfileRequestContext profileRequestContext);
}