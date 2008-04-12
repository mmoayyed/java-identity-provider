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

package edu.internet2.middleware.shibboleth.common.profile.provider;

import java.util.Collection;
import java.util.Map;

import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.common.binding.encoding.SAMLMessageEncoder;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Contextual object used to accumlate information as profile requests are being processed.
 * 
 * @param <InboundMessage> type of inbound SAML message
 * @param <OutboundMessage> type of outbound SAML message
 * @param <NameIdentifierType> type of name identifier used for subjects
 * @param <ProfileConfigurationType> profile configuration type for current request
 */
public class BaseSAMLProfileRequestContext<InboundMessage extends SAMLObject, OutboundMessage extends SAMLObject, NameIdentifierType extends SAMLObject, ProfileConfigurationType extends ProfileConfiguration>
        extends BasicSAMLMessageContext<InboundMessage, OutboundMessage, NameIdentifierType> implements
        SAMLProfileRequestContext<InboundMessage, OutboundMessage, NameIdentifierType, ProfileConfigurationType> {
    
    /** Decoder used to decode inbound message. */
    private SAMLMessageDecoder messageDecoder;
    
    /** Encoder used to encode outbound message. */
    private SAMLMessageEncoder messageEncoder;
    
    /** Attributes retrieved for the principal. */
    private Map<String, BaseAttribute> principalAttributes;

    /** Authentication method used to authenticate the principal. */
    private String principalAuthenticationMethod;

    /** Principal name of the subject of the request. */
    private String principalName;

    /** Configuration for the profile. */
    private ProfileConfigurationType profileConfiguration;

    /** IDs of attribute released to relying party. */
    private Collection<String> releasedAttributeIds;

    /** Configuration for the relying party. */
    private RelyingPartyConfiguration relyingPartyConfiguration;

    /** IDs of attribute requested by relaying party. */
    private Collection<String> requestedAttributeIds;

    /** Current user's session. */
    private Session userSession;

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> getAttributes() {
        return principalAttributes;
    }

    /**
     * Gets the message decoder used to decode the message from the inbound transport.
     * 
     * @return message decoder used to decode the message from the inbound transport
     */
    public SAMLMessageDecoder getMessageDecoder() {
        return messageDecoder;
    }

    /**
     * Gets the message encoder used to encoder the message onto the outbound transport.
     * 
     * @return message encoder used to encoder the message onto the outbound transport
     */
    public SAMLMessageEncoder getMessageEncoder() {
        return messageEncoder;
    }

    /** {@inheritDoc} */
    public String getPrincipalAuthenticationMethod() {
        return principalAuthenticationMethod;
    }

    /** {@inheritDoc} */
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * Gets the configuration for the profile for the relying party.
     * 
     * @return configuration for the profile for the relying party
     */
    public ProfileConfigurationType getProfileConfiguration() {
        return profileConfiguration;
    }

    /** {@inheritDoc} */
    public Collection<String> getReleasedAttributes() {
        return releasedAttributeIds;
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getRelyingPartyConfiguration() {
        return relyingPartyConfiguration;
    }

    /** {@inheritDoc} */
    public Collection<String> getRequestedAttributesIds() {
        return requestedAttributeIds;
    }

    /** {@inheritDoc} */
    public Session getUserSession() {
        return userSession;
    }

    /** {@inheritDoc} */
    public void setAttributes(Map<String, BaseAttribute> attributes) {
        principalAttributes = attributes;
    }

    /**
     * Sets the message decoder used to decode the message from the inbound transport.
     * 
     * @param decoder message decoder used to decode the message from the inbound transport
     */
    public void setMessageDecoder(SAMLMessageDecoder decoder) {
        messageDecoder = decoder;
    }

    /**
     * Sets the message encoder used to encoder the message onto the outbound transport.
     * 
     * @param encoder message encoder used to encoder the message onto the outbound transport
     */
    public void setMessageEncoder(SAMLMessageEncoder encoder) {
        messageEncoder = encoder;
    }

    /** {@inheritDoc} */
    public void setPrincipalAuthenticationMethod(String method) {
        principalAuthenticationMethod = method;
    }

    /** {@inheritDoc} */
    public void setPrincipalName(String name) {
        principalName = name;
    }

    /** {@inheritDoc} */
    public void setProfileConfiguration(ProfileConfigurationType configuration) {
        profileConfiguration = configuration;
    }

    /** {@inheritDoc} */
    public void setReleasedAttributes(Collection<String> attributeIds) {
        releasedAttributeIds = attributeIds;
    }

    /** {@inheritDoc} */
    public void setRelyingPartyConfiguration(RelyingPartyConfiguration configuration) {
        relyingPartyConfiguration = configuration;
    }

    /** {@inheritDoc} */
    public void setRequestedAttributes(Collection<String> ids) {
        requestedAttributeIds = ids;
    }

    /** {@inheritDoc} */
    public void setUserSession(Session session) {
        userSession = session;
    }
}