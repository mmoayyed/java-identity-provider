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

import org.apache.log4j.Logger;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * Shibboleth SAML attribute request context.
 * 
 * @param <NameIdentifierType> identifier of the subject of the query; must be either
 *            <code>org.opensaml.saml1.core.NameIdentifier</code> or <code>org.opensaml.saml2.core.NameID</code>
 * @param <QueryType> type of SAML attribute query; must be either <code>org.opensaml.saml1.core.AttributeQuery</code>
 *            or <code>org.opensaml.saml2.core.AttributeQuery</code>
 */
public class ShibbolethSAMLAttributeRequestContext<NameIdentifierType extends SAMLObject, QueryType extends SAMLObject>
        extends ShibbolethAttributeRequestContext 
        implements SAMLAttributeRequestContext<NameIdentifierType, QueryType> {

    /** Class logger. */
    private final Logger log = Logger.getLogger(ShibbolethSAMLAttributeRequestContext.class);

    /** Metadata provider used to look up entity information. */
    private MetadataProvider metadataProvider;

    /** SAML 1 or 2 subject name identifier. */
    private NameIdentifierType subjectNameIdentifier;

    /** SAML 1 or 2 attribute query. */
    private QueryType attributeQuery;

    /** Metadata for the attribute issuer. */
    private EntityDescriptor attributeIssuerMetadata;

    /** Metadata for the attribute requester. */
    private EntityDescriptor attributeRequesterMetadata;

    /** Constructor. */
    public ShibbolethSAMLAttributeRequestContext() {
        super();
        initialize();
    }

    /**
     * Constructor.
     * 
     * @param provider metadata provider used to look up entity information
     * @param rpConfig relying party configuration in effect for the attribute request
     */
    public ShibbolethSAMLAttributeRequestContext(MetadataProvider provider, RelyingPartyConfiguration rpConfig) {
        super();

        if (provider == null || rpConfig == null) {
            throw new IllegalArgumentException("Metadata provider and relying party configuration may not be null");
        }

        metadataProvider = provider;
        setRelyingPartyConfiguration(rpConfig);

        initialize();
    }

    /**
     * Constructor.
     * 
     * @param provider metadata provider used to look up entity information
     * @param rpConfig relying party configuration in effect for the attribute request
     * @param query SAML attribute query of this request
     */
    public ShibbolethSAMLAttributeRequestContext(MetadataProvider provider, RelyingPartyConfiguration rpConfig,
            QueryType query) {
        super();

        if (provider == null || rpConfig == null || query == null) {
            throw new IllegalArgumentException(
                    "Metadata provider, relying party configuration, and attribute query may not be null");
        }
        metadataProvider = provider;
        setRelyingPartyConfiguration(rpConfig);
        attributeQuery = query;

        initialize();
    }

    /** {@inheritDoc} */
    public EntityDescriptor getAttributeIssuerMetadata() {
        if(attributeIssuerMetadata == null && metadataProvider != null && getRelyingPartyConfiguration() != null){
            try {
                attributeIssuerMetadata = metadataProvider.getEntityDescriptor(getRelyingPartyConfiguration()
                        .getProviderId());
            } catch (MetadataProviderException e) {
                log.warn("Unable to look up metadata for attribute issuer: "
                        + getRelyingPartyConfiguration().getProviderId(), e);
            }
        }
        return attributeIssuerMetadata;
    }

    /** {@inheritDoc} */
    public QueryType getAttributeQuery() {
        return attributeQuery;
    }

    /** {@inheritDoc} */
    public EntityDescriptor getAttributeRequesterMetadata() {
        if(attributeRequesterMetadata == null && metadataProvider != null && getAttributeRequester() != null){
            try {
                attributeRequesterMetadata = metadataProvider.getEntityDescriptor(getAttributeRequester());
            } catch (MetadataProviderException e) {
                log.warn("Unable to look up metadata for attribute requester: " + getAttributeRequester(), e);
            }
        }
        return attributeRequesterMetadata;
    }

    /** {@inheritDoc} */
    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    /** {@inheritDoc} */
    public NameIdentifierType getSubjectNameIdentifier() {
        return subjectNameIdentifier;
    }

    /**
     * Initializes various internal variables given information provided during object construction.
     */
    @SuppressWarnings("unchecked")
    protected void initialize() {
        if (attributeQuery != null) {
            if (attributeQuery instanceof org.opensaml.saml1.core.AttributeQuery) {
                org.opensaml.saml1.core.Subject subject = ((org.opensaml.saml1.core.AttributeQuery) attributeQuery)
                        .getSubject();
                subjectNameIdentifier = (NameIdentifierType) subject.getNameIdentifier();
            } else {
                org.opensaml.saml2.core.Subject subject = ((org.opensaml.saml2.core.AttributeQuery) attributeQuery)
                        .getSubject();
                subjectNameIdentifier = (NameIdentifierType) subject.getNameID();

                Issuer issuer = ((org.opensaml.saml2.core.AttributeQuery) attributeQuery).getIssuer();
                setAttributeRequester(issuer.getValue());

            }
        }
    }
}