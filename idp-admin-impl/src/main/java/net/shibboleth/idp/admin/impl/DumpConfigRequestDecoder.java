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

package net.shibboleth.idp.admin.impl;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.AbstractHttpServletRequestMessageDecoder;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.cas.config.AbstractProtocolConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Decodes an incoming configuration reporting message.
 */
public class DumpConfigRequestDecoder extends AbstractHttpServletRequestMessageDecoder {

    /** Name of the query parameter carrying the profile: {@value} . */
    @Nonnull @NotEmpty public static final String PROFILE_PARAM = "profile";

    /** Name of the query parameter carrying the protocol: {@value} . */
    @Nonnull @NotEmpty public static final String PROTOCOL_PARAM = "protocol";

    /** Name of the query parameter for the SAML 1 protocol: {@value} . */
    @Nonnull @NotEmpty public static final String SAML1_PARAM = "saml1";

    /** Name of the query parameter for the SAML 2 protocol: {@value} . */
    @Nonnull @NotEmpty public static final String SAML2_PARAM = "saml2";

    /** Name of the query parameter for the CAS protocol: {@value} . */
    @Nonnull @NotEmpty public static final String CAS_PARAM = "cas";

    /** Name of the query parameter for the OIDC protocol: {@value} . */
    @Nonnull @NotEmpty public static final String OIDC_PARAM = "oidc";

    /** Name of the query parameter carrying the requester: {@value} . */
    @Nonnull @NotEmpty public static final String REQUESTER_ID_PARAM = "requester";

    /** {@inheritDoc} */
    @Override
    protected void doDecode() throws MessageDecodingException {
        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            throw new MessageDecodingException("Unable to locate HttpServletRequest");
        }
        
        final String profile = getProfileId(request);
        final DumpConfigRequest message = new DumpConfigRequest(profile, getProtocolId(request),
                getRequesterId(request));
        final MessageContext messageContext = new MessageContext();
        messageContext.setMessage(message);
        setMessageContext(messageContext);
        
        final SAMLPeerEntityContext peerCtx = new SAMLPeerEntityContext();
        // TODO: allow for IdP role...
        peerCtx.setRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        peerCtx.setEntityId(message.getRequesterId());
        messageContext.addSubcontext(peerCtx, true);
        
        messageContext.ensureSubcontext(SAMLProtocolContext.class).setProtocol(message.getProtocolId());
    }

    /**
     * Get the profile ID.
     * 
     * @param request current HTTP request
     * 
     * @return the profile ID
     * 
     * @throws MessageDecodingException thrown if the request does not contain a profile ID
     */
    @Nonnull @NotEmpty protected String getProfileId(@Nonnull final HttpServletRequest request)
            throws MessageDecodingException {
        final String id = StringSupport.trimOrNull(request.getParameter(PROFILE_PARAM));
        if (id == null) {
            throw new MessageDecodingException("Request did not contain the " + PROFILE_PARAM + " query parameter.");
        }
        
        if (id.startsWith("http")) {
            return id;
        } else if (id.startsWith("/")) {
            return "http://shibboleth.net/ns/profiles" + id;
        } else {
            return "http://shibboleth.net/ns/profiles/" + id;
        }
    }

    /**
     * Get the ID of the requester.
     * 
     * @param request current HTTP request
     * 
     * @return the ID of the requester
     * 
     * @throws MessageDecodingException thrown if the request does not contain a requester name
     */
    @Nonnull @NotEmpty protected String getRequesterId(@Nonnull final HttpServletRequest request)
            throws MessageDecodingException {
        final String name = StringSupport.trimOrNull(request.getParameter(REQUESTER_ID_PARAM));
        if (name == null) {
            throw new MessageDecodingException("Request did not contain the " + REQUESTER_ID_PARAM
                    + " query parameter.");
        }
        return name;
    }

    /**
     * Get the protocol string used for metadata access.
     * 
     * @param request current HTTP request
     * 
     * @return the protocol
     * 
     * @throws MessageDecodingException if unable to determine the protocol 
     */
    @Nonnull @NotEmpty protected String getProtocolId(@Nonnull final HttpServletRequest request)
            throws MessageDecodingException {
        final String protocol = StringSupport.trimOrNull(request.getParameter(PROTOCOL_PARAM));
        if (protocol != null) {
            return protocol;
        }
        
        if (request.getParameter(SAML1_PARAM) != null) {
            return SAMLConstants.SAML11P_NS;
        } else if (request.getParameter(SAML2_PARAM) != null) {
            return SAMLConstants.SAML20P_NS;
        } else if (request.getParameter(CAS_PARAM) != null) {
            return AbstractProtocolConfiguration.PROTOCOL_URI;
        } else if (request.getParameter(OIDC_PARAM) != null) {
            return "http://openid.net/specs/openid-connect-core-1_0.html";
        }
        
        throw new MessageDecodingException("Request did not contain the " + PROTOCOL_PARAM
                + " query parameter.");
    }

}