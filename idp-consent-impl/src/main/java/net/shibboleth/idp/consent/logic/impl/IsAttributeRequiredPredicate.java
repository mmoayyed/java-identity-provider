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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.AttributeConsumingServiceContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.navigate.AttributeConsumerServiceLookupFunction;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.utilities.java.support.logic.Predicate;

/**
 * Predicate that determines whether an IdP attribute is required by the requester.
 */
public class IsAttributeRequiredPredicate implements Predicate<IdPAttribute> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IsAttributeRequiredPredicate.class);

    /** Strategy used to find the {@link SAMLMetadataContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;

    /** Strategy used to find the {@link AttributeConsumingService} from the {@link SAMLMetadataContext}. */
    @Nonnull private Function<SAMLMetadataContext,AttributeConsumingService> acsLookupStrategy;

    /** Map of requested attributes. */
    @Nullable private final Multimap<String,IdPAttribute> requestedAttributesMap;

    /**
     * Constructor.
     *
     * @param request the HTTP request
     */
    public IsAttributeRequiredPredicate(@Nullable final HttpServletRequest request) {
        metadataContextLookupStrategy =
                new ChildContextLookup<>(SAMLMetadataContext.class).compose(
                        new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(
                                new OutboundMessageContextLookup()));
        acsLookupStrategy =
                new AttributeConsumerServiceLookupFunction().compose(
                        new ChildContextLookup<>(AttributeConsumingServiceContext.class));
        final ProfileRequestContext prc = getProfileRequestContext(request);
        requestedAttributesMap = getRequestedAttributes(prc);
    }

    // TODO setters for strategies ?

    /**
     * Get the profile request context from the HTTP servlet request.
     * 
     * @param httpRequest the HTTP request
     * @return the profile request context or <code>null</code>
     */
    @Nullable protected ProfileRequestContext getProfileRequestContext(@Nullable final HttpServletRequest httpRequest) {
        if (httpRequest != null) {
            final Object object = httpRequest.getAttribute(ProfileRequestContext.BINDING_KEY);
            if (object != null && object instanceof ProfileRequestContext) {
                return (ProfileRequestContext) object;
            }
            log.warn("Unable to find ProfileRequestContext in HTTP request");
        } else {
            log.warn("HTTP request is not available");
        }
        return null;
    }

    /**
     * Get the map of requested attributes from the profile request context.
     * 
     * @param prc the profile request context
     * @return the map of requested attributes or <code>null</code>
     */
    @Nullable protected Multimap<String, IdPAttribute>
            getRequestedAttributes(@Nullable final ProfileRequestContext prc) {
        if (prc != null) {
            final SAMLMetadataContext metadataContext = metadataContextLookupStrategy.apply(prc);
            if (metadataContext != null) {
                final AttributeConsumingService acs = acsLookupStrategy.apply(metadataContext);
                if (acs != null) {
                    final List<AttributesMapContainer> maps = acs.getObjectMetadata().get(AttributesMapContainer.class);
                    if (maps != null && !maps.isEmpty()) {
                        if (maps.size() > 1) {
                            log.warn("More than one set of mapped attributes found, using the first.");
                        }
                        return maps.get(0).get();
                    }
                }
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final IdPAttribute input) {
        if (input != null && requestedAttributesMap != null && !requestedAttributesMap.isEmpty()) {
            final Collection<IdPAttribute> requestedAttrs = requestedAttributesMap.get(input.getId());
            if (requestedAttrs != null) {
                for (final IdPAttribute requestedAttr : requestedAttrs) {
                    if (requestedAttr instanceof IdPRequestedAttribute
                            && ((IdPRequestedAttribute) requestedAttr).isRequired()) {
                        log.debug("Attribute '{}' is required", input);
                        return true;
                    }
                }
            }
        }
        log.debug("Attribute '{}' is not required", input);
        return false;
    }

}
