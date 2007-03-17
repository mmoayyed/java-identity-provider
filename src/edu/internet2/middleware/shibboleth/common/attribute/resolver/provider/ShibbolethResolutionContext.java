/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.metadata.provider.MetadataProvider;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;

/**
 * Basic implementation of {@link ResolutionContext}.
 */
public class ShibbolethResolutionContext implements ResolutionContext {

    /** ID of entity making request. */
    private String attributeRequester;

    /** Metadata provider. */
    private MetadataProvider metadataProvider;

    /** Name of principal that the request is for. */
    private String principalName;

    /** Servlet request. */
    private ServletRequest servletRequest;

    /** Attribute Definitions that have been resolved for this request. */
    private Map<String, AttributeDefinition> definitions;

    /** Data Connectors that have been resolved for this request. */
    private Map<String, DataConnector> connectors;

    /** NameID of the subject this request is for. */
    private NameID subject;

    /** Constructor. */
    public ShibbolethResolutionContext() {
        definitions = new HashMap<String, AttributeDefinition>();
        connectors = new HashMap<String, DataConnector>();
    }

    /** {@inheritDoc} */
    public String getAttributeRequester() {
        return attributeRequester;
    }

    /** {@inheritDoc} */
    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    /** {@inheritDoc} */
    public String getPrincipalName() {
        return principalName;
    }

    /** {@inheritDoc} */
    public ServletRequest getRequest() {
        return servletRequest;
    }

    /** {@inheritDoc} */
    public Map<String, AttributeDefinition> getResolvedAttributeDefinitions() {
        return definitions;
    }

    /** {@inheritDoc} */
    public Map<String, DataConnector> getResolvedDataConnectors() {
        return connectors;
    }

    /** {@inheritDoc} */
    public NameID getSubject() {
        return subject;
    }

    /**
     * Set the ID of the entity making the request.
     * 
     * @param newAttributeRequester The attributeRequester to set.
     */
    public void setAttributeRequester(String newAttributeRequester) {
        attributeRequester = newAttributeRequester;
    }

    /**
     * Set the metadata provider.
     * 
     * @param newMetadataProvider The metadataProvider to set.
     */
    public void setMetadataProvider(MetadataProvider newMetadataProvider) {
        metadataProvider = newMetadataProvider;
    }

    /**
     * Set the principal name the request is for.
     * 
     * @param newPrincipalName The principalName to set.
     */
    public void setPrincipalName(String newPrincipalName) {
        principalName = newPrincipalName;
    }

    /**
     * Set the servlet request.
     * 
     * @param newServletRequest The servletRequest to set.
     */
    public void setRequest(ServletRequest newServletRequest) {
        servletRequest = newServletRequest;
    }

    /**
     * Set the NameID for the subject this request is for.
     * 
     * @param newSubject The subject to set.
     */
    public void setSubject(NameID newSubject) {
        subject = newSubject;
    }

}