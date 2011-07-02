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

package net.shibboleth.idp.saml.impl.profile;

import javax.servlet.http.HttpServletRequest;

import net.jcip.annotations.NotThreadSafe;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.AbstractMessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/** Decodes an incoming Shibboleth Authentication Request message. */
@NotThreadSafe
public class IdpInitiatedSsoRequestMessageDecoder extends AbstractMessageDecoder<IdpInitatedSsoRequest> {

    /**
     * Deprecated name of the query parameter carrying the service provider entity ID: {@value} . Use of
     * {@link #ENTITY_ID_PARAM} is preferred.
     */
    public static final String PROVIDER_ID_PARAM = "providerId";

    /** Name of the query parameter carrying the service provider entity ID: {@value} . */
    public static final String ENTITY_ID_PARAM = "entityId";

    /**
     * Deprecated name of the query parameter carrying the service provider's assertion consumer service URL: {@value} .
     * Use of {@link #ACS_URL_PARAM} is preferred.
     */
    public static final String SHIRE_PARAM = "shire";

    /** Name of the query parameter carrying the service provider's assertion consumer service URL: {@value} . */
    public static final String ACS_URL_PARAM = "acs";

    /** Name of the query parameter carrying the service provider's target information: {@value} . */
    public static final String TARGET_PARAM = "target";

    /** Name of the query parameter carrying the current time at the service provider: {@value. } */
    public static final String TIME_PARAM = "time";

    /** Current Servlet request. */
    private HttpServletRequest currentRequest;

    /**
     * Constructor.
     * 
     * @param servletRequest current Servlet Request
     */
    public IdpInitiatedSsoRequestMessageDecoder(HttpServletRequest servletRequest) {
        Assert.isNotNull(servletRequest, "Current HTTP Servlet Request can not be null");
        currentRequest = servletRequest;
    }

    /** {@inheritDoc} */
    public void decode() throws MessageDecodingException {
        IdpInitatedSsoRequest authnRequest =
                new IdpInitatedSsoRequest(getEntityId(currentRequest), getShire(currentRequest),
                        getTarget(currentRequest), getTime(currentRequest));

        MessageContext<IdpInitatedSsoRequest> messageContext = getMessageContext();
        messageContext.setMessage(authnRequest);
    }

    /**
     * Gets the entity ID of the service provider.
     * 
     * @param request current HTTP request
     * 
     * @return the entity ID of the service provider
     * 
     * @throws MessageDecodingException thrown if the request does not contain a service provider entity ID
     */
    private String getEntityId(HttpServletRequest request) throws MessageDecodingException {
        String entityId = StringSupport.trimOrNull(request.getParameter(ENTITY_ID_PARAM));
        if (entityId == null) {
            entityId = StringSupport.trimOrNull(request.getParameter(PROVIDER_ID_PARAM));
        }

        if (entityId == null) {
            throw new MessageDecodingException("Shibboleth Authentication Request message did not contain either the "
                    + ENTITY_ID_PARAM + " (preferred) or " + PROVIDER_ID_PARAM + "query parameter.");
        }
        return entityId;
    }

    /**
     * Gets the assertion consumer service URL for the service provider.
     * 
     * @param request current HTTP request
     * 
     * @return the assertion consumer service URL, may be null if none is given in the request
     */
    private String getShire(HttpServletRequest request) {
        String acsUrl = StringSupport.trimOrNull(request.getParameter(ACS_URL_PARAM));
        if (acsUrl == null) {
            acsUrl = StringSupport.trimOrNull(request.getParameter(SHIRE_PARAM));
        }

        return acsUrl;
    }

    /**
     * Gets the opaque target sent by the service provider.
     * 
     * 
     * @param request current HTTP request
     * 
     * @return the target, or null if the service provider did not send one
     */
    private String getTarget(HttpServletRequest request) {
        return StringSupport.trimOrNull(request.getParameter(TARGET_PARAM));
    }

    /**
     * Gets the current time, in milliseconds since the epoch, at the service provider, if the service provider sent it.
     * 
     * @param request current HTTP request
     * 
     * @return the time sent by the service provider or 0 if not time was provided
     * 
     * @throws MessageDecodingException thrown if the time parameter given by the service provider is non-numeric or a
     *             negative time
     */
    private long getTime(HttpServletRequest request) throws MessageDecodingException {
        String timeString = StringSupport.trimOrNull(request.getParameter(TIME_PARAM));
        if (timeString == null) {
            return 0;
        }

        try {
            long time = Long.parseLong(timeString);
            if (time > 0) {
                throw new MessageDecodingException("Shibboleth Authentication Request contained a negative time value");
            }
            return time * 1000;
        } catch (NumberFormatException e) {
            throw new MessageDecodingException("Shibboleth Authentication Request contained a non-numeric time value");
        }
    }
}