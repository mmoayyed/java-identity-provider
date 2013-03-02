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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import com.google.common.base.Optional;

/**
 * A data connector that generates a unique ID by computing the SHA-1 hash of a given attribute value, the entity ID of
 * the inbound message issuer, and a provided salt.
 */
public class ComputedIDDataConnector extends BaseComputedIDDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ComputedIDDataConnector.class);

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == getSalt()) {
            throw new ComponentInitializationException("ComputedIDDataConnector definition '" + getId()
                    + "': no salt set");
        }

        if (getSalt().length < 16) {
            throw new ComponentInitializationException("ComputedIDDataConnector definition '" + getId()
                    + "': salt must be at least 16 bytes in size");
        }

    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Map<String, Attribute>> doDataConnectorResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            log.warn("Attribute definition '{}' no attribute recipient context provided ", getId());
            return Optional.absent();
        }

        String attributeRecipientID = attributeRecipientContext.getAttributeRecipientID();
        
        if (attributeRecipientID == null) {
            log.warn("ComputedIDDataConnector '{}' : No Attribute Recipient ID located, unable to compute ID", getId());
            return Optional.absent();
        }

        return encodeAsAttribute(generateComputedId(attributeRecipientID, resolveSourceAttribute(resolutionContext)));
    }

}
