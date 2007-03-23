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

import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.SAMLAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.WebApplicationAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * Shibboleth {@link AttributeRequestContext}.
 */
public class ShibbolethAttributeRequestContext implements SAMLAttributeRequestContext,
        WebApplicationAttributeRequestContext {

    /** Configuration information related to relying party. */
    private RelyingPartyConfiguration relyingPartyConfiguration;

    /** Metadata provider used to lookup entity information. */
    private MetadataProvider metadata;

    /** SAML metadata of attribute issuing entity. */
    private EntityDescriptor issuerMetadata;

    /** SAML metadata of attribute requesting entity. */
    private EntityDescriptor requesterMetadata;

    /** Principal name of user described by attributes. */
    private String principal;

    /** Method used to authenticate principal. */
    private String principalAuthenticationMethod;

    /** Servlet request that started attribute request. */
    private ServletRequest attributeRequest;

    /** IDs of requested attributes. */
    private Set<String> requestedAttributes;

    /** Constructor. */
    public ShibbolethAttributeRequestContext(){
        requestedAttributes = new HashSet<String>();
    }
    
    /**
     * Constructor.
     * 
     * @param provider metadata provider used to look up entity information
     * @param rpConfig relying party configuration information
     * 
     * @throws MetadataProviderException thrown if their is a problem locating entity information within the metadata
     *             provider
     */
    public ShibbolethAttributeRequestContext(MetadataProvider provider, RelyingPartyConfiguration rpConfig)
            throws MetadataProviderException {
        relyingPartyConfiguration = rpConfig;
        metadata = provider;
        issuerMetadata = provider.getEntityDescriptor(rpConfig.getProviderID());
        requesterMetadata = provider.getEntityDescriptor(rpConfig.getRelyingPartyID());
    }

    /** {@inheritDoc} */
    public EntityDescriptor getAttributeIssuerMetadata() {
        return issuerMetadata;
    }

    /** {@inheritDoc} */
    public MetadataProvider getMetadataProvider() {
        return metadata;
    }

    /** {@inheritDoc} */
    public EntityDescriptor getAttributeRequesterMetadata() {
        return requesterMetadata;
    }

    /** {@inheritDoc} */
    public String getAttributeIssuer() {
        return issuerMetadata.getEntityID();
    }

    /** {@inheritDoc} */
    public String getAttributeRequester() {
        return issuerMetadata.getEntityID();
    }

    /** {@inheritDoc} */
    public String getPrincipalAuthenticationMethod() {
        return principalAuthenticationMethod;
    }

    /**
     * Sets the method used to authenticate the principal.
     * 
     * @param method method used to authenticate the principal
     */
    public void setPrincipalAuthenticationMethod(String method) {
        principalAuthenticationMethod = DatatypeHelper.safeTrimOrNullString(method);
    }

    /** {@inheritDoc} */
    public String getPrincipalName() {
        return principal;
    }

    /**
     * Sets the name of the principal that the requested attributes describe.
     * 
     * @param name name of the principal that the requested attributes describe
     */
    public void setPrincipalName(String name) {
        principal = DatatypeHelper.safeTrimOrNullString(name);
    }

    /** {@inheritDoc} */
    public Set<String> getRequestedAttributes() {
        return requestedAttributes;
    }

    /**
     * Sets the attributes being requested.
     * 
     * @param attributes attributes being requested
     */
    public void setRequestedAttributes(Set<String> attributes) {
        requestedAttributes = attributes;
    }

    /** {@inheritDoc} */
    public ServletRequest getRequest() {
        return attributeRequest;
    }

    /**
     * Sets the servlet request that started the attribute request.
     * 
     * @param request servlet request that started the attribute request
     */
    public void setRequest(ServletRequest request) {
        attributeRequest = request;
    }
}