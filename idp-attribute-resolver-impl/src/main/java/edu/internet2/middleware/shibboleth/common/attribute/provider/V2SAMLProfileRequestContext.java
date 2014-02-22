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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.component.IdentifiedComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * Emulation code for Scripted Attributes.
 */
public class V2SAMLProfileRequestContext implements IdentifiedComponent {

    /**
     * The Attribute Resolution Context, used to local the Principal.
     */
    private final AttributeResolutionContext resolutionContext;

    /** log. */
    private final Logger log = LoggerFactory.getLogger(V2SAMLProfileRequestContext.class);

    /** Id - taken from the resolver. */
    private final String id;

    /**
     * Constructor.
     * 
     * @param attributeResolutionContext the resolution context.
     * @param attributeId the id of the attribute being resolved.
     */
    public V2SAMLProfileRequestContext(@Nonnull final AttributeResolutionContext attributeResolutionContext,
            final String attributeId) {
        resolutionContext = Constraint.isNotNull(attributeResolutionContext, "Attribute Resolution Context was null");
        id = Constraint.isNotNull(StringSupport.trimOrNull(attributeId), "Attribute Id was null or empty");
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public String getId() {
        return id;
    }

    /**
     * Get the name of the principal associated with this resolution.
     * 
     * @return the Principal.
     */
    public String getPrincipalName() {
        return resolutionContext.getPrincipal();
    }

    /**
     * Get the Entity Id associate with this attribute issuer.
     * 
     * @return the entityId.
     */
    public String getPeerEntityId() {
        return resolutionContext.getAttributeRecipientID();
    }

    /**
     * Get the Entity Id associate with this attribute consumer.
     * 
     * @return the entityId.
     */
    public String getLocalEntityId() {
        return resolutionContext.getAttributeIssuerID();
    }

    // All other methods are stubs

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public SAMLObject getInboundSAMLMessage() {
        log.error("AttributeDefinition: '{}' call unsupported method getInboundSAMLMessage", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getInboundSAMLMessageId() {
        log.error("AttributeDefinition: '{}' call unsupported method getInboundSAMLMessageId", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public DateTime getInboundSAMLMessageIssueInstant() {
        log.error("AttributeDefinition: '{}' call unsupported method getInboundSAMLMessageIssueInstant", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getInboundSAMLProtocol() {
        log.error("AttributeDefinition: '{}' call unsupported method getInboundSAMLProtocol", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public EntityDescriptor getLocalEntityMetadata() {
        log.error("AttributeDefinition: '{}' call unsupported method getLocalEntityMetadata", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public QName getLocalEntityRole() {
        log.error("AttributeDefinition: '{}' call unsupported method getLocalEntityRole", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public RoleDescriptor getLocalEntityRoleMetadata() {
        log.error("AttributeDefinition: '{}' call unsupported method getLocalEntityRoleMetadata", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public MetadataResolver getMetadataResolver() {
        log.error("AttributeDefinition: '{}' call unsupported method getMetadataResolver", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getOuboundSAMLMessageSigningCredential() {
        log.error("AttributeDefinition: '{}' call unsupported method getOuboundSAMLMessageSigningCredential", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public byte[] getOutboundMessageArtifactType() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundMessageArtifactType", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public SAMLObject getOutboundSAMLMessage() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundSAMLMessage", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getOutboundSAMLMessageId() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundSAMLMessageId", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public DateTime getOutboundSAMLMessageIssueInstant() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundSAMLMessageIssueInstant", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getOutboundSAMLProtocol() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundSAMLProtocol", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Endpoint getPeerEntityEndpoint() {
        log.error("AttributeDefinition: '{}' call unsupported method getPeerEntityEndpoint", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public EntityDescriptor getPeerEntityMetadata() {
        log.error("AttributeDefinition: '{}' call unsupported method getPeerEntityMetadata", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public QName getPeerEntityRole() {
        log.error("AttributeDefinition: '{}' call unsupported method getPeerEntityRole", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public RoleDescriptor getPeerEntityRoleMetadata() {
        log.error("AttributeDefinition: '{}' call unsupported method getPeerEntityRoleMetadata", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getRelayState() {
        log.error("AttributeDefinition: '{}' call unsupported method getRelayState", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public SAMLObject getSubjectNameIdentifier() {
        log.error("AttributeDefinition: '{}' call unsupported method getSubjectNameIdentifier", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public boolean isInboundSAMLMessageAuthenticated() {
        log.error("AttributeDefinition: '{}' call unsupported method isInboundSAMLMessageAuthenticated", getId());
        return false;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundSAMLMessage(SAMLObject param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundSAMLMessage", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundSAMLMessageAuthenticated(boolean param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundSAMLMessageAuthenticated", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundSAMLMessageId(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundSAMLMessageId", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundSAMLMessageIssueInstant(DateTime param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundSAMLMessageIssueInstant", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundSAMLProtocol(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundSAMLProtocol", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setLocalEntityId(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setLocalEntityId", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setLocalEntityMetadata(EntityDescriptor param) {
        log.error("AttributeDefinition: '{}' call unsupported method setLocalEntityMetadata", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setLocalEntityRole(QName param) {
        log.error("AttributeDefinition: '{}' call unsupported method setLocalEntityRole", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setLocalEntityRoleMetadata(RoleDescriptor param) {
        log.error("AttributeDefinition: '{}' call unsupported method setLocalEntityRoleMetadata", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setMetadataResolver(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setMetadataResolver", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundMessageArtifactType(byte[] param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundMessageArtifactType", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundSAMLMessage(SAMLObject param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundSAMLMessage", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundSAMLMessageId(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundSAMLMessageId", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundSAMLMessageIssueInstant(DateTime param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundSAMLMessageIssueInstant", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundSAMLMessageSigningCredential(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundSAMLMessageSigningCredential", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundSAMLProtocol(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundSAMLProtocol", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPeerEntityEndpoint(Endpoint param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPeerEntityEndpoint", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPeerEntityId(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPeerEntityId", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPeerEntityMetadata(EntityDescriptor param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPeerEntityMetadata", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPeerEntityRole(QName param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPeerEntityRole", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPeerEntityRoleMetadata(RoleDescriptor param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPeerEntityRoleMetadata", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setRelayState(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setRelayState", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setSubjectNameIdentifier(SAMLObject param) {
        log.error("AttributeDefinition: '{}' call unsupported method setSubjectNameIdentifier", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getCommunicationProfileId() {
        log.error("AttributeDefinition: '{}' call unsupported method getCommunicationProfileId", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public XMLObject getInboundMessage() {
        log.error("AttributeDefinition: '{}' call unsupported method getInboundMessage", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getInboundMessageIssuer() {
        log.error("AttributeDefinition: '{}' call unsupported method getInboundMessageIssuer", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getInboundMessageTransport() {
        log.error("AttributeDefinition: '{}' call unsupported method getInboundMessageTransport", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public XMLObject getOutboundMessage() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundMessage", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getOutboundMessageIssuer() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundMessageIssuer", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getOutboundMessageTransport() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundMessageTransport", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getSecurityPolicyResolver() {
        log.error("AttributeDefinition: '{}' call unsupported method getSecurityPolicyResolver", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public boolean isIssuerAuthenticated() {
        log.error("AttributeDefinition: '{}' call unsupported method isIssuerAuthenticated", getId());
        return false;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setCommunicationProfileId(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setCommunicationProfileId", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundMessage(XMLObject param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundMessage", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundMessageIssuer(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundMessageIssuer", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setInboundMessageTransport(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setInboundMessageTransport", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundMessage(XMLObject param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundMessage", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundMessageIssuer(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundMessageIssuer", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundMessageTransport(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundMessageTransport", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setSecurityPolicyResolver(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setSecurityPolicyResolver", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getPreSecurityInboundHandlerChainResolver() {
        log.error("AttributeDefinition: '{}' call unsupported method getPreSecurityInboundHandlerChainResolver",
                getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPreSecurityInboundHandlerChainResolver(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPreSecurityInboundHandlerChainResolver",
                getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getPostSecurityInboundHandlerChainResolver() {
        log.error("AttributeDefinition: '{}' call unsupported method getPostSecurityInboundHandlerChainResolver",
                getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPostSecurityInboundHandlerChainResolver(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPostSecurityInboundHandlerChainResolver",
                getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getOutboundHandlerChainResolver() {
        log.error("AttributeDefinition: '{}' call unsupported method getOutboundHandlerChainResolver", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setOutboundHandlerChainResolver(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setOutboundHandlerChainResolver", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getProfileConfiguration() {
        log.error("AttributeDefinition: '{}' call unsupported method getProfileConfiguration", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getRelyingPartyConfiguration() {
        log.error("AttributeDefinition: '{}' call unsupported method getRelyingPartyConfiguration", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Object getUserSession() {
        log.error("AttributeDefinition: '{}' call unsupported method getUserSession", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setProfileConfiguration(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setProfileConfiguration", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setRelyingPartyConfiguration(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setRelyingPartyConfiguration", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setUserSession(Object param) {
        log.error("AttributeDefinition: '{}' call unsupported method setUserSession", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Collection getReleasedAttributes() {
        log.error("AttributeDefinition: '{}' call unsupported method getReleasedAttributes", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setReleasedAttributes(Collection param) {
        log.error("AttributeDefinition: '{}' call unsupported method setReleasedAttributes", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Collection<String> getRequestedAttributesIds() {
        log.error("AttributeDefinition: '{}' call unsupported method getRequestedAttributesIds", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setRequestedAttributes(Collection<String> param) {
        log.error("AttributeDefinition: '{}' call unsupported method setRequestedAttributes", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public Map<String, Object> getAttributes() {
        log.error("AttributeDefinition: '{}' call unsupported method getAttributes", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setAttributes(Map<String, Object> param) {
        log.error("AttributeDefinition: '{}' call unsupported method setAttributes", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @return null
     */
    public String getPrincipalAuthenticationMethod() {
        log.error("AttributeDefinition: '{}' call unsupported method getPrincipalAuthenticationMethod", getId());
        return null;
    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPrincipalAuthenticationMethod(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPrincipalAuthenticationMethod", getId());

    }

    /**
     * Stubbed failing function.
     * 
     * @param param ignored.
     */
    public void setPrincipalName(String param) {
        log.error("AttributeDefinition: '{}' call unsupported method setPrincipalName", getId());

    }

    /** {@inheritDoc}. */
    @Override
    public String toString() {
        return Objects.toStringHelper(V2SAMLProfileRequestContext.class).
                                      add("Id", getId()).
                                      add("PrincipalName", getPrincipalName()).
                                      add("PeerEntityId", getPeerEntityId()).
                                      add("LocalEntityId", getLocalEntityId()).
                                      toString();
    }
}
