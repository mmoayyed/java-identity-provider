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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.AbstractHttpServletRequestMessageDecoder;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes an incoming metadata query request.
 */
public class MetadataQueryRequestDecoder extends AbstractHttpServletRequestMessageDecoder {

    /** Name of the query parameter carrying the entityID: {@value} . */
    @Nonnull @NotEmpty public static final String ENTITY_ID_PARAM = "entityID";

    /** Name of the query parameter carrying the protocol: {@value} . */
    @Nonnull @NotEmpty public static final String PROTOCOL_PARAM = "protocol";

    /** Name of the query parameter for the SAML 1 protocol: {@value} . */
    @Nonnull @NotEmpty public static final String SAML1_PARAM = "saml1";

    /** Name of the query parameter for the SAML 2 protocol: {@value} . */
    @Nonnull @NotEmpty public static final String SAML2_PARAM = "saml2";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(MetadataQueryRequestDecoder.class);
    
    /** {@inheritDoc} */
    @Override
    protected void doDecode() throws MessageDecodingException {
        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            throw new MessageDecodingException("Unable to locate HttpServletRequest");
        }
        
        final MetadataQueryRequest message = new MetadataQueryRequest();
        message.setEntityID(getEntityID(request));
        message.setProtocol(getProtocol(request));
        
        final MessageContext messageContext = new MessageContext();
        messageContext.setMessage(message);
        setMessageContext(messageContext);
        
        final SAMLPeerEntityContext peerCtx = new SAMLPeerEntityContext();
        peerCtx.setRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        peerCtx.setEntityId(message.getEntityID());
        messageContext.addSubcontext(peerCtx, true);
        
        if (message.getProtocol() != null) {
            messageContext.getSubcontext(SAMLProtocolContext.class, true).setProtocol(message.getProtocol());
        }
    }

    /**
     * Get the entityID parameter.
     * 
     * @param request current HTTP request
     * 
     * @return the entityID
     * 
     * @throws MessageDecodingException thrown if the request does not contain an entityID
     */
    @Nonnull @NotEmpty protected String getEntityID(@Nonnull final HttpServletRequest request)
            throws MessageDecodingException {
        final String name = StringSupport.trimOrNull(request.getParameter(ENTITY_ID_PARAM));
        if (name == null) {
            throw new MessageDecodingException("Request did not contain the " + ENTITY_ID_PARAM
                    + " query parameter.");
        }
        return name;
    }

    /**
     * Get the protocol.
     * 
     * @param request current HTTP request
     * 
     * @return the protocol, or null
     */
    @Nullable protected String getProtocol(@Nonnull final HttpServletRequest request) {
        final String protocol = StringSupport.trimOrNull(request.getParameter(PROTOCOL_PARAM));
        if (protocol != null) {
            return protocol;
        }
        
        if (request.getParameter(SAML1_PARAM) != null) {
            return SAMLConstants.SAML11P_NS;
        } else if (request.getParameter(SAML2_PARAM) != null) {
            return SAMLConstants.SAML20P_NS;
        }
        
        return null;
    }

}