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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.metadata.impl.AttributeMappingNodeProcessor;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.core.xml.util.XMLObjectSupport.CloneOutputOption;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.AttributeConsumingServiceContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.ext.reqattr.RequestedAttributes;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Action that add any {@link RequestedAttribute}s to the previously populated {@link AttributeConsumingServiceContext}.
 */
public class AddRequestedAttributesToAttributeConsumingService extends AbstractProfileAction  {

    /** Logger. */
    @Nonnull private final Logger log =
            LoggerFactory.getLogger(AddRequestedAttributesToAttributeConsumingService.class);

    /** strategy to get the {@link AttributeConsumingServiceContext}. */
    @Nonnull private Function<ProfileRequestContext, AttributeConsumingServiceContext>
        attributeConsumingServiceContextLookupStrategy;

    /** strategy to get the {@link AttributeConsumingServiceContext}. */
    @Nonnull private Function<ProfileRequestContext, List<RequestedAttribute>>
        requestedAttributesLookupStrategy;

    /** The attribute resolver we use to map attributes. */
    @Nullable private ReloadableService<AttributeResolver> attributeResolverService;

    /** The context we use to get and put the {@link AttributeConsumingService}.*/
    private AttributeConsumingServiceContext acsContext;

    /** Lookup strategy for an {@link AttributeConsumingService} index. */
    @Nullable private Function<ProfileRequestContext,Integer> indexLookupStrategy;

    /**
     * Constructor.
     */
    public AddRequestedAttributesToAttributeConsumingService() {
        super();
        // At this point, by default  the SAMLMetadataContext hangs off the SAMLPeerContext
        attributeConsumingServiceContextLookupStrategy =
                Functions.compose(
                         new ChildContextLookup(AttributeConsumingServiceContext.class),
                         Functions.compose(
                                 new ChildContextLookup<>(SAMLMetadataContext.class),
                                 Functions.compose(
                                        new ChildContextLookup<>(SAMLPeerEntityContext.class),
                                        new InboundMessageContextLookup())));

        requestedAttributesLookupStrategy = new RequestedAttributesLookup();
        indexLookupStrategy = new AuthnRequestIndexLookup();
    }

    /**
     * Set the strategy to locate the {@link AttributeConsumingServiceContext} from the {@link ProfileRequestContext}.
     *
     * @param strategy lookup strategy
     */
    public void setAttributeConsumingServiceContextLookupStrategy(@Nonnull final
            Function<ProfileRequestContext,AttributeConsumingServiceContext> strategy) {
        attributeConsumingServiceContextLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeConsumingServiceContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy to locate the {@link AttributeConsumingServiceContext} from the {@link ProfileRequestContext}.
     *
     * @param strategy lookup strategy
     */
    public void setRequestedAttributesLookupStrategy(@Nonnull final
            Function<ProfileRequestContext,List<RequestedAttribute>> strategy) {
        requestedAttributesLookupStrategy = Constraint.isNotNull(strategy,
                "RequestedAttributesLookupStrategy lookup strategy cannot be null");
    }

    /**
     * Set the strategy to locate the {@link AttributeConsumingService} index from the {@link MessageContext}.
     *
     * @param strategy lookup strategy
     */
    public void setIndexLookupStrategy(@Nullable final Function<ProfileRequestContext,Integer> strategy) {
        indexLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeConsumingService index lookup strategy cannot be null");
    }

    /**
     * Sets the service which does the attribute mapping.
     *
     * @param resolverService the service for the attribute resolver we are to derive unmapping info from
     */
    public void setResolverService(@Nonnull final ReloadableService<AttributeResolver> resolverService) {
        attributeResolverService = Constraint.isNotNull(resolverService, "AttributeResolver cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        acsContext = attributeConsumingServiceContextLookupStrategy.apply(profileRequestContext);
        if (acsContext == null) {
            log.error("{} Unable to find AttributeConsumingServiceContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        return true;
    }

    /** {@inheritDoc}*/
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        /** The Requested Attributes, if any. */
        final List<RequestedAttribute> requestedAttributes =
                requestedAttributesLookupStrategy.apply(profileRequestContext);
        if (requestedAttributes == null || requestedAttributes.isEmpty()) {
            // Nothing further to do
            return;
        }
        if (indexLookupStrategy.apply(profileRequestContext) != null) {
            // "In the event that both are present, an Identity Provider SHOULD"
            // ignore the extension
            return;
        }

        try {
            // Create the ACS
            final AttributeConsumingService newACS = (AttributeConsumingService)
                    XMLObjectSupport.buildXMLObject(AttributeConsumingService.DEFAULT_ELEMENT_NAME);
            // Add in the RequestedAttributes
            for (final RequestedAttribute attribute: requestedAttributes) {
                newACS.getRequestAttributes().add(
                        XMLObjectSupport.cloneXMLObject(attribute, CloneOutputOption.DropDOM));
            }
            // Canonical processing
            final AttributeMappingNodeProcessor processor = new AttributeMappingNodeProcessor(attributeResolverService);
            processor.process(newACS);
            // And plug it in
            acsContext.setAttributeConsumingService(newACS);
        } catch (final FilterException e) {
            log.error("{} Error mapping Attributesresponding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.RUNTIME_EXCEPTION);
        } catch (final MarshallingException e) {
            log.error("{} Error Cloning RequestedAttributes", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.RUNTIME_EXCEPTION);
       } catch (final UnmarshallingException e) {
           log.error("{} Error Cloning RequestedAttributes", getLogPrefix(), e);
           ActionSupport.buildEvent(profileRequestContext, EventIds.RUNTIME_EXCEPTION);
        }
    }

    /** Default lookup function that reads from a SAML 2 {@link AuthnRequest}. */
    private class RequestedAttributesLookup implements Function<ProfileRequestContext,List<RequestedAttribute>> {

        /** {@inheritDoc} */
        @Override
        public List<RequestedAttribute> apply(@Nullable final ProfileRequestContext input) {
            if (input == null) {
                return null;
            }
            final MessageContext messageContext = input.getInboundMessageContext();
            final Object message = messageContext.getMessage();
            if (message == null || !(message instanceof AuthnRequest)) {
                return null;
            }
            final Extensions extensions = ((AuthnRequest) message).getExtensions();
            if (extensions == null) {
                return null;
            }
            final List<XMLObject> bindings = extensions.getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME);
            if (bindings == null || bindings.isEmpty()) {
                return null;
            }
            return ((RequestedAttributes)bindings.get(0)).getRequestedAttributes();
        }
    }

    /** Default lookup function that reads from a SAML 2 {@link AuthnRequest}. */
    private class AuthnRequestIndexLookup implements Function<ProfileRequestContext,Integer> {

        /** {@inheritDoc} */
        @Override
        public Integer apply(@Nullable final ProfileRequestContext input) {
            if (input == null) {
                return null;
            }
            final MessageContext messageContext = input.getInboundMessageContext();
            final Object message = messageContext.getMessage();
            if (message != null && message instanceof AuthnRequest) {
                return ((AuthnRequest) message).getAttributeConsumingServiceIndex();
            }
            return null;
        }
    }
}
