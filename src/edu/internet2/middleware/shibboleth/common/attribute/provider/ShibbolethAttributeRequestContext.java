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

import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletRequest;

import org.apache.log4j.Logger;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.SAMLAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.WebApplicationAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Shibboleth attribute request context.
 */
public class ShibbolethAttributeRequestContext implements SAMLAttributeRequestContext,
        WebApplicationAttributeRequestContext {

    /** Class logger. */
    private final Logger log = Logger.getLogger(ShibbolethAttributeRequestContext.class);

    /** Metadata provider used to look up entity information. */
    private MetadataProvider metadatProvider;

    /** Relying party configuration in effect for the attribute request. */
    private RelyingPartyConfiguration relyingPartyConfiguration;

    /** Profile configuration in effect for the attribute request. */
    private ProfileConfiguration profileConfiguration;

    /** Attribute requester ID. */
    private String attributeRequester;

    /** SAML 1 or SAML 2 attribute query. */
    private SAMLObject attributeQuery;

    /** Name of the principal that requested attributes describe. */
    private String principalName;

    /** Method used to authenticate the principal. */
    private String princpalAuthenticationMethod;

    /** Current user session. */
    private Session userSession;

    /** Metadata for the attribute issuer. */
    private EntityDescriptor attributeIssuerMetadata;

    /** Metadata for the attribute requester. */
    private EntityDescriptor attributeRequestMetadata;

    /** Name identifier of the SAML subject of the attribute query. */
    private String subjectNameId;

    /** Format of the subject name identifier. */
    private String subjectNameIdFormat;

    /** Attributes being requested. */
    private Set<String> requestedAttributes;

    /** Servlet request that carried the attribute query. */
    private ServletRequest servletRequest;

    /** Constructor. */
    public ShibbolethAttributeRequestContext() {
        initialize();
    }

    /**
     * Constructor.
     * 
     * @param provider metadata provider used to look up entity information
     * @param rpConfig relying party configuration in effect for the attribute request
     */
    public ShibbolethAttributeRequestContext(MetadataProvider provider, RelyingPartyConfiguration rpConfig) {
        metadatProvider = provider;
        relyingPartyConfiguration = rpConfig;

        if (metadatProvider == null || relyingPartyConfiguration == null) {
            throw new IllegalArgumentException("Metadata provider and relying party configuration may not be null");
        }
        initialize();
    }

    /**
     * Constructor.
     * 
     * @param provider metadata provider used to look up entity information
     * @param rpConfig relying party configuration in effect for the attribute request
     * @param query SAML 1 attribute query of this request
     */
    public ShibbolethAttributeRequestContext(MetadataProvider provider, RelyingPartyConfiguration rpConfig,
            org.opensaml.saml1.core.AttributeQuery query) {
        metadatProvider = provider;
        relyingPartyConfiguration = rpConfig;
        attributeQuery = query;

        if (metadatProvider == null || relyingPartyConfiguration == null || query == null) {
            throw new IllegalArgumentException(
                    "Metadata provider, relying party configuration, and attribute query may not be null");
        }
        initialize();
    }

    /**
     * Constructor.
     * 
     * @param provider metadata provider used to look up entity information
     * @param rpConfig relying party configuration in effect for the attribute request
     * @param query SAML 2 attribute query of this request
     */
    public ShibbolethAttributeRequestContext(MetadataProvider provider, RelyingPartyConfiguration rpConfig,
            org.opensaml.saml2.core.AttributeQuery query) {
        metadatProvider = provider;
        relyingPartyConfiguration = rpConfig;
        attributeQuery = query;

        if (metadatProvider == null || relyingPartyConfiguration == null || query == null) {
            throw new IllegalArgumentException(
                    "Metadata provider, relying party configuration, and attribute query may not be null");
        }
        initialize();
    }

    /** {@inheritDoc} */
    public EntityDescriptor getAttributeIssuerMetadata() {
        return attributeIssuerMetadata;
    }

    /** {@inheritDoc} */
    public SAMLObject getAttributeQuery() {
        return attributeQuery;
    }

    /** {@inheritDoc} */
    public EntityDescriptor getAttributeRequesterMetadata() {
        return attributeRequestMetadata;
    }

    /** {@inheritDoc} */
    public MetadataProvider getMetadataProvider() {
        return metadatProvider;
    }

    /** {@inheritDoc} */
    public String getSubjectNameId() {
        return subjectNameId;
    }

    /**
     * Sets the subject name identifier.
     * 
     * @param id subject name identifier
     * 
     * @throws IllegalArgumentException thrown if this method is called when an attribute query has been provided
     */
    public void setSubjectNameId(String id) {
        if (attributeQuery != null) {
            throw new IllegalArgumentException("A subject name ID may not be given if an attribute query is present.");
        }

        subjectNameId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    public String getSubjectNameIdFormat() {
        return subjectNameIdFormat;
    }

    /**
     * Sets the format of subject name identifier.
     * 
     * @param format format of subject name identifier
     * 
     * @throws IllegalArgumentException thrown if this method is called when an attribute query has been provided
     */
    public void setSubjectNameIdFormat(String format) {
        if (attributeQuery != null) {
            throw new IllegalArgumentException(
                    "A subject name ID format may not be given if an attribute query is present.");
        }

        subjectNameIdFormat = DatatypeHelper.safeTrimOrNullString(format);
    }

    /** {@inheritDoc} */
    public String getAttributeRequester() {
        return attributeRequester;
    }

    /**
     * Sets the entity ID of the attribute request.
     * 
     * @param requester entity ID of the attribute request
     * 
     * @throws IllegalArgumentException thrown if this method is called when a SAML 2 attribute query has been provided
     */
    public void setAttributeRequester(String requester) {
        if (attributeQuery != null && attributeQuery instanceof org.opensaml.saml2.core.AttributeQuery) {
            throw new IllegalArgumentException(
                    "An attribute requester may not be given if a SAML 2 attribute query is present.");
        }

        attributeRequester = DatatypeHelper.safeTrimOrNullString(requester);
        try {
            attributeRequestMetadata = metadatProvider.getEntityDescriptor(attributeRequester);
        } catch (MetadataProviderException e) {
            log.warn("Unable to look up metadata for attribute requester: " + attributeRequester, e);
        }
    }

    /** {@inheritDoc} */
    public ProfileConfiguration getEffectiveProfileConfiguration() {
        return profileConfiguration;
    }

    /**
     * Sets the profile configuration in effect for this attribute request.
     * 
     * @param config profile configuration in effect for this attribute request
     * 
     * @throws IllegalArgumentException thrown if the given configuration is not one of the configuration contained with
     *             the relying party configuration provided at construction time
     */
    public void setEffectiveProfileConfiguration(ProfileConfiguration config) {
        if (relyingPartyConfiguration != null && config != null
                && !relyingPartyConfiguration.getProfileConfigurations().values().contains(config)) {
            throw new IllegalArgumentException(
                    "Profile configuration is not a valid configuration for the provided relying party.");
        }
        profileConfiguration = config;
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
     * 
     * @throws IllegalArgumentException thrown if this method is called when a user session has been provided
     */
    public void setPrincipalName(String name) {
        if (userSession != null) {
            throw new IllegalArgumentException("A principal name may not be given if a user session is present.");
        }

        principalName = DatatypeHelper.safeTrimOrNullString(name);
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getRelyingPartyConfiguration() {
        return relyingPartyConfiguration;
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
     * Sets the session of the current user. If a principal name was previously set it will be overwritten with the
     * principal name given in the user session.
     * 
     * @param session session of the current user
     */
    public void setUserSession(Session session) {
        userSession = session;
        principalName = null;
    }

    /** {@inheritDoc} */
    public ServletRequest getRequest() {
        return servletRequest;
    }

    /**
     * Sets the servlet request that started this request.
     * 
     * @param request servlet request that started this request
     */
    public void setRequest(ServletRequest request) {
        servletRequest = request;
    }

    /**
     * Initializes various internal variables given information provided during object construction.
     */
    protected void initialize() {
        requestedAttributes = new TreeSet<String>();

        if (attributeQuery != null) {
            if (attributeQuery instanceof org.opensaml.saml1.core.AttributeQuery) {
                org.opensaml.saml1.core.Subject subject = ((org.opensaml.saml1.core.AttributeQuery) attributeQuery)
                        .getSubject();
                NameIdentifier nameId = subject.getNameIdentifier();
                subjectNameId = nameId.getNameIdentifier();
                subjectNameIdFormat = nameId.getFormat();
            } else {
                org.opensaml.saml2.core.Subject subject = ((org.opensaml.saml2.core.AttributeQuery) attributeQuery)
                        .getSubject();
                NameID nameId = subject.getNameID();
                subjectNameId = nameId.getValue();
                subjectNameIdFormat = nameId.getFormat();

                Issuer issuer = ((org.opensaml.saml2.core.AttributeQuery) attributeQuery).getIssuer();
                attributeRequester = issuer.getValue();
                try {
                    attributeRequestMetadata = metadatProvider.getEntityDescriptor(attributeRequester);
                } catch (MetadataProviderException e) {
                    log.warn("Unable to look up metadata for attribute requester: " + attributeRequester, e);
                }
            }
        }

        if (relyingPartyConfiguration != null) {
            try {
                attributeIssuerMetadata = metadatProvider
                        .getEntityDescriptor(relyingPartyConfiguration.getProviderId());
            } catch (MetadataProviderException e) {
                log.warn("Unable to look up metadata for attribute issuer: "
                        + relyingPartyConfiguration.getProviderId(), e);
            }
        }
    }
}