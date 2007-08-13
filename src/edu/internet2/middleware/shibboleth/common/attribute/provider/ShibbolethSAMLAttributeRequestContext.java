/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.NameID;

import edu.internet2.middleware.shibboleth.common.profile.ProfileMessageContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;

/**
 * Shibboleth SAML attribute request context.
 * 
 * @param <NameIdentifierType> identifier of the subject of the query; must be either {@link NameIdentifier} or
 *            {@link NameID}
 * @param <InboundMessageType> type of inbound SAML message
 * @param <OutboundMessageType> type of outbound SAML message
 * @param <ProfileConfigurationType> profile configuration type for current request
 */
public interface ShibbolethSAMLAttributeRequestContext<NameIdentifierType extends SAMLObject, InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, ProfileConfigurationType extends ProfileConfiguration>
        extends SAMLMessageContext<InboundMessageType, OutboundMessageType>,
        ProfileMessageContext<ProfileConfigurationType> {

    /**
     * Gets the subject's SAML name identifier.
     * 
     * @return subject's SAML name identifier
     */
    public NameIdentifierType getSubjectNameIdentifier();

    /**
     * Sets the subject's SAML name identifier.
     * 
     * @param identifier subject's SAML name identifier
     */
    public void setSubjectNameIdentifier(NameIdentifierType identifier);
}