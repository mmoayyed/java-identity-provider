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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.ProfileHandlerAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.profile.ProfileRequest;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Implementation of {@link ProfileHandlerAttributeRequestContext}.
 */
public class ShibbolethAttributeRequestContext implements ProfileHandlerAttributeRequestContext {

    /** Request to the profile that in turn is request attributes. */
    private ProfileRequest<ServletRequest> profileRequest;

    /** Attribute requester ID. */
    private String attributeRequester;

    /** Profile configuration in effect for the attribute request. */
    private ProfileConfiguration profileConfiguration;

    /** Method used to authenticate the principal. */
    private String princpalAuthenticationMethod;

    /** Name of the principal that requested attributes describe. */
    private String principalName;

    /** Relying party configuration in effect for the attribute request. */
    private RelyingPartyConfiguration relyingPartyConfiguration;

    /** Attributes being requested. */
    private Set<String> requestedAttributes;

    /** Current user session. */
    private Session userSession;

    /** Constructor. */
    public ShibbolethAttributeRequestContext() {
        requestedAttributes = new HashSet<String>();
    }

    /** {@inheritDoc} */
    public ProfileRequest<ServletRequest> getRequest() {
        return profileRequest;
    }

    /**
     * Sets the request to the profile that in turn is request attributes.
     * 
     * @param request request to the profile that in turn is request attributes
     */
    public void setRequest(ProfileRequest<ServletRequest> request) {
        profileRequest = request;
    }

    /** {@inheritDoc} */
    public String getAttributeRequester() {
        return attributeRequester;
    }

    /**
     * Sets the attribute requester ID.
     * 
     * @param id attribute requester ID
     */
    public void setAttributeRequester(String id) {
        attributeRequester = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    public ProfileConfiguration getProfileConfiguration() {
        return profileConfiguration;
    }

    /**
     * Sets the profile configuration in effect for the attribute request.
     * 
     * @param configuration profile configuration in effect for the attribute request
     */
    public void setProfileConfiguration(ProfileConfiguration configuration) {
        profileConfiguration = configuration;
    }

    /** {@inheritDoc} */
    public String getPrincipalAuthenticationMethod() {
        return princpalAuthenticationMethod;
    }

    /**
     * Sets the method used to authenticate the principal.
     * 
     * @param method method used to authenticate the principal
     */
    public void setPrincipalAuthenticationMethod(String method) {
        princpalAuthenticationMethod = DatatypeHelper.safeTrimOrNullString(method);
    }

    /** {@inheritDoc} */
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * Sets the name of the principal that requested attributes describe.
     * 
     * @param name name of the principal that requested attributes describe
     */
    public void setPrincipalName(String name) {
        principalName = DatatypeHelper.safeTrimOrNullString(name);
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getRelyingPartyConfiguration() {
        return relyingPartyConfiguration;
    }

    /**
     * Sets the relying party configuration in effect for the attribute request.
     * 
     * @param configuration relying party configuration in effect for the attribute request
     */
    public void setRelyingPartyConfiguration(RelyingPartyConfiguration configuration) {
        relyingPartyConfiguration = configuration;
    }

    /** {@inheritDoc} */
    public Set<String> getRequestedAttributes() {
        return requestedAttributes;
    }

    /** {@inheritDoc} */
    public Session getUserSession() {
        return userSession;
    }

    /**
     * Sets the current users session.
     * 
     * @param session current users session
     */
    public void setUserSession(Session session) {
        userSession = session;
    }
}