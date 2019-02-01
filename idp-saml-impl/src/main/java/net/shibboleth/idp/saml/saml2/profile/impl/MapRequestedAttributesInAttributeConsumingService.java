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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.attribute.mapping.AttributesMapContainer;
import net.shibboleth.idp.saml.metadata.impl.AttributeMappingNodeProcessor;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.AttributeConsumingServiceContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action that ensures that the attributes in the ACS (if any) are mapped.
 */
public class MapRequestedAttributesInAttributeConsumingService extends AbstractProfileAction  {

    /** Logger. */
    @Nonnull private final Logger log =
            LoggerFactory.getLogger(MapRequestedAttributesInAttributeConsumingService.class);

    /** strategy to get the {@link AttributeConsumingServiceContext}. */
    @Nonnull private Function<ProfileRequestContext, AttributeConsumingServiceContext>
        attributeConsumingServiceContextLookupStrategy;

    /** The attribute resolver we use to map attributes. */
    @Nullable private ReloadableService<AttributeResolver> attributeResolverService;

    /** The context we use to get and put the {@link AttributeConsumingService}.*/
    private AttributeConsumingServiceContext acsContext;

    /**
     * Constructor.
     */
    public MapRequestedAttributesInAttributeConsumingService() {
        super();
        // At this point, by default  the SAMLMetadataContext hangs off the SAMLPeerContext
        attributeConsumingServiceContextLookupStrategy =
                new ChildContextLookup(AttributeConsumingServiceContext.class).compose(
                        new ChildContextLookup<>(SAMLMetadataContext.class).compose(
                                new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(
                                        new InboundMessageContextLookup())));
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
        return true;
    }

    /** {@inheritDoc}*/
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (acsContext == null) {
            log.trace("{} AttributeConsumingServiceContext not found", getLogPrefix());
            return;
        }

        final AttributeConsumingService acs = acsContext.getAttributeConsumingService();
        if (acs == null) {
            log.trace("{} no AttributeConsumingService to map", getLogPrefix());
            return;
        }
        
        if (acs.getRequestAttributes().isEmpty() ||
            acs.getObjectMetadata().containsKey(AttributesMapContainer.class) ||
            acs.getParent() != null) {
            log.trace("{} skipping mapping for AttributeConsumingService", getLogPrefix());
            // Nothing to map, already mapped, or attached to metadata (and hence already scanned)
            return;
        }
        try {
            final AttributeMappingNodeProcessor processor = new AttributeMappingNodeProcessor(attributeResolverService);
            log.debug("{} mapping requested Attributes for generated AttributeConsumingService", getLogPrefix());
            processor.process(acs);
        } catch (final FilterException e) {
            log.error("{} Error mapping Attributesresponding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.RUNTIME_EXCEPTION);
        }
    }

}
