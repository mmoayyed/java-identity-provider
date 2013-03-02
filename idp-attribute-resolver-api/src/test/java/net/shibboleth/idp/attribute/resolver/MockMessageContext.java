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

package net.shibboleth.idp.attribute.resolver;

import javax.xml.namespace.QName;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.SAMLMessageContext;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.security.credential.Credential;
import org.opensaml.ws.message.handler.HandlerChainResolver;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.OutTransport;

/** 
 * An old style {@link SAMLMessageContext} whose sole purpose is to provide 
 * a SubjectNameIdenfifier and an InboundMessageIssuer 
 */
/**
 *
 */
public class MockMessageContext implements SAMLMessageContext<SAMLObject, SAMLObject, SAMLObject> {

    /**
     * the SubjectNameIdenfifier.
     */
    private SAMLObject subjectNameIdenfifier;

    /**
     * the InboundMessageIssuer.
     */
    private String inboundMessageIssuer;

    public MockMessageContext(String issuer, SAMLObject nameId) {
        subjectNameIdenfifier = nameId;
        inboundMessageIssuer = issuer;
    }

    /** {@inheritDoc} */
    public String getCommunicationProfileId() {
        return null;
    }

    /** {@inheritDoc} */
    public XMLObject getInboundMessage() {
        return null;
    }

    /** {@inheritDoc} */
    public String getInboundMessageIssuer() {
        return inboundMessageIssuer;
    }

    /** {@inheritDoc} */
    public InTransport getInboundMessageTransport() {
        return null;
    }

    /** {@inheritDoc} */
    public XMLObject getOutboundMessage() {
        return null;
    }

    /** {@inheritDoc} */
    public String getOutboundMessageIssuer() {
        return null;
    }

    /** {@inheritDoc} */
    public OutTransport getOutboundMessageTransport() {
        return null;
    }

    /** {@inheritDoc} */
    public SecurityPolicyResolver getSecurityPolicyResolver() {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isIssuerAuthenticated() {
        return false;
    }

    /** {@inheritDoc} */
    public void setCommunicationProfileId(String id) {
    }

    /** {@inheritDoc} */
    public void setInboundMessage(XMLObject message) {
    }

    /** {@inheritDoc} */
    public void setInboundMessageIssuer(String issuer) {
    }

    /** {@inheritDoc} */
    public void setInboundMessageTransport(InTransport transport) {
    }

    /** {@inheritDoc} */
    public void setOutboundMessage(XMLObject message) {
    }

    /** {@inheritDoc} */
    public void setOutboundMessageIssuer(String issuer) {
    }

    /** {@inheritDoc} */
    public void setOutboundMessageTransport(OutTransport transport) {
    }

    /** {@inheritDoc} */
    public void setSecurityPolicyResolver(SecurityPolicyResolver resolver) {
    }

    /** {@inheritDoc} */
    public HandlerChainResolver getPreSecurityInboundHandlerChainResolver() {
        return null;
    }

    /** {@inheritDoc} */
    public void setPreSecurityInboundHandlerChainResolver(HandlerChainResolver newHandlerChainResolver) {
    }

    /** {@inheritDoc} */
    public HandlerChainResolver getPostSecurityInboundHandlerChainResolver() {
        return null;
    }

    /** {@inheritDoc} */
    public void setPostSecurityInboundHandlerChainResolver(HandlerChainResolver newHandlerChainResolver) {
    }

    /** {@inheritDoc} */
    public HandlerChainResolver getOutboundHandlerChainResolver() {
        return null;
    }

    /** {@inheritDoc} */
    public void setOutboundHandlerChainResolver(HandlerChainResolver newHandlerChainResolver) {
    }

    /** {@inheritDoc} */
    public SAMLObject getInboundSAMLMessage() {

        return null;
    }

    /** {@inheritDoc} */
    public String getInboundSAMLMessageId() {

        return null;
    }

    /** {@inheritDoc} */
    public DateTime getInboundSAMLMessageIssueInstant() {

        return null;
    }

    /** {@inheritDoc} */
    public String getInboundSAMLProtocol() {

        return null;
    }

    /** {@inheritDoc} */
    public String getLocalEntityId() {
        return null;
    }

    /** {@inheritDoc} */
    public EntityDescriptor getLocalEntityMetadata() {
        return null;
    }

    /** {@inheritDoc} */
    public QName getLocalEntityRole() {
        return null;
    }

    /** {@inheritDoc} */
    public RoleDescriptor getLocalEntityRoleMetadata() {
        return null;
    }

    /** {@inheritDoc} */
    public MetadataProvider getMetadataProvider() {
        return null;
    }

    /** {@inheritDoc} */
    public Credential getOuboundSAMLMessageSigningCredential() {
        return null;
    }

    /** {@inheritDoc} */
    public byte[] getOutboundMessageArtifactType() {
        return null;
    }

    /** {@inheritDoc} */
    public SAMLObject getOutboundSAMLMessage() {
        return null;
    }

    /** {@inheritDoc} */
    public String getOutboundSAMLMessageId() {
        return null;
    }

    /** {@inheritDoc} */
    public DateTime getOutboundSAMLMessageIssueInstant() {
        return null;
    }

    /** {@inheritDoc} */
    public String getOutboundSAMLProtocol() {
        return null;
    }

    /** {@inheritDoc} */
    public Endpoint getPeerEntityEndpoint() {
        return null;
    }

    /** {@inheritDoc} */
    public String getPeerEntityId() {

        return null;
    }

    /** {@inheritDoc} */
    public EntityDescriptor getPeerEntityMetadata() {

        return null;
    }

    /** {@inheritDoc} */
    public QName getPeerEntityRole() {

        return null;
    }

    /** {@inheritDoc} */
    public RoleDescriptor getPeerEntityRoleMetadata() {

        return null;
    }

    /** {@inheritDoc} */
    public String getRelayState() {

        return null;
    }

    /** {@inheritDoc} */
    public SAMLObject getSubjectNameIdentifier() {
        return subjectNameIdenfifier;
    }

    /** {@inheritDoc} */
    public boolean isInboundSAMLMessageAuthenticated() {

        return false;
    }

    /** {@inheritDoc} */
    public void setInboundSAMLMessage(SAMLObject message) {

    }

    /** {@inheritDoc} */
    public void setInboundSAMLMessageAuthenticated(boolean isAuthenticated) {

    }

    /** {@inheritDoc} */
    public void setInboundSAMLMessageId(String id) {

    }

    /** {@inheritDoc} */
    public void setInboundSAMLMessageIssueInstant(DateTime instant) {

    }

    /** {@inheritDoc} */
    public void setInboundSAMLProtocol(String protocol) {

    }

    /** {@inheritDoc} */
    public void setLocalEntityId(String id) {

    }

    /** {@inheritDoc} */
    public void setLocalEntityMetadata(EntityDescriptor metadata) {

    }

    /** {@inheritDoc} */
    public void setLocalEntityRole(QName role) {

    }

    /** {@inheritDoc} */
    public void setLocalEntityRoleMetadata(RoleDescriptor role) {

    }

    /** {@inheritDoc} */
    public void setMetadataProvider(MetadataProvider provider) {

    }

    /** {@inheritDoc} */
    public void setOutboundMessageArtifactType(byte[] type) {

    }

    /** {@inheritDoc} */
    public void setOutboundSAMLMessage(SAMLObject message) {

    }

    /** {@inheritDoc} */
    public void setOutboundSAMLMessageId(String id) {

    }

    /** {@inheritDoc} */
    public void setOutboundSAMLMessageIssueInstant(DateTime instant) {

    }

    /** {@inheritDoc} */
    public void setOutboundSAMLMessageSigningCredential(Credential credential) {

    }

    /** {@inheritDoc} */
    public void setOutboundSAMLProtocol(String protocol) {

    }

    /** {@inheritDoc} */
    public void setPeerEntityEndpoint(Endpoint endpoint) {

    }

    /** {@inheritDoc} */
    public void setPeerEntityId(String id) {

    }

    /** {@inheritDoc} */
    public void setPeerEntityMetadata(EntityDescriptor metadata) {

    }

    /** {@inheritDoc} */
    public void setPeerEntityRole(QName role) {

    }

    /** {@inheritDoc} */
    public void setPeerEntityRoleMetadata(RoleDescriptor role) {

    }

    /** {@inheritDoc} */
    public void setRelayState(String relayState) {

    }

    /** {@inheritDoc} */
    public void setSubjectNameIdentifier(SAMLObject identifier) {

    }

}
