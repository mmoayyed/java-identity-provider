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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;

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

import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.attribute.impl.AttributeMappingNodeProcessor;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.service.ReloadableService;

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

    /** The registry of decoding rules. */
    @NonnullBeforeExec private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /** The context we use to get and put the {@link AttributeConsumingService}.*/
    private AttributeConsumingServiceContext acsContext;

    /**
     * Constructor.
     */
    public MapRequestedAttributesInAttributeConsumingService() {
        // At this point, by default  the SAMLMetadataContext hangs off the SAMLPeerContext
        final Function<ProfileRequestContext, AttributeConsumingServiceContext> acscls = 
                new ChildContextLookup<>(AttributeConsumingServiceContext.class).compose(
                        new ChildContextLookup<>(SAMLMetadataContext.class).compose(
                                new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(
                                        new InboundMessageContextLookup())));
        assert acscls != null;
        attributeConsumingServiceContextLookupStrategy = acscls;
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
     * Sets the service which provides attribute decoding rules.
     *
     * @param registry the registry service
     */
    public void setTranscoderRegistry(@Nonnull final ReloadableService<AttributeTranscoderRegistry> registry) {
        transcoderRegistry = Constraint.isNotNull(registry, "Attribute Transcoding Registry cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        acsContext = attributeConsumingServiceContextLookupStrategy.apply(profileRequestContext);
        
        if (transcoderRegistry == null) {
            log.warn("{} No Transcoder Regsitry set", getLogPrefix());
            return false;
        }
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
            log.trace("{} No AttributeConsumingService to map", getLogPrefix());
            return;
        }
        
        if (acs.getRequestedAttributes().isEmpty() || 
            acs.getObjectMetadata().containsKey(AttributesMapContainer.class) ||
            acs.getParent() != null) {
            log.trace("{} Skipping decode of AttributeConsumingService", getLogPrefix());
            // Nothing to map, already mapped, or attached to metadata (and hence already scanned)
            return;
        }
        
        try {
            assert transcoderRegistry!=null;
            final AttributeMappingNodeProcessor processor = new AttributeMappingNodeProcessor(transcoderRegistry);
            log.debug("{} Decoding RequestedAttributes for generated AttributeConsumingService", getLogPrefix());
            processor.process(acs);
        } catch (final FilterException e) {
            log.error("{} Error decoding RequestedAttributes", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.RUNTIME_EXCEPTION);
        }
    }

}
