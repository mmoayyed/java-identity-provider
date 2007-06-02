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

import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestContext;

/**
 * {@link AttributeRequestContext} that adds in SAML information.
 * 
 * @param <NameIdentifierType> identifier of the subject of the query; must be either
 *            <code>org.opensaml.saml1.core.NameIdentifier</code> or <code>org.opensaml.saml2.core.NameID</code>
 * @param <QueryType> type of SAML attribute query; must be either <code>org.opensaml.saml1.core.AttributeQuery</code>
 *            or <code>org.opensaml.saml2.core.AttributeQuery</code>
 */
public interface SAMLAttributeRequestContext<NameIdentifierType extends SAMLObject, QueryType extends SAMLObject>
        extends AttributeRequestContext {

    /**
     * Gets the name identifier of the subject of this request.
     * 
     * @return name identifier of the subject of this request
     */
    public NameIdentifierType getSubjectNameIdentifier();

    /**
     * Gets the metadata provider that may be used to lookup information about entities.
     * 
     * @return metadata provider that may be used to lookup information about entities
     */
    public MetadataProvider getMetadataProvider();

    /**
     * Gets the SAML metadata for the attribute requesting entity.
     * 
     * @return SAML metadata for the attribute requesting entity
     */
    public EntityDescriptor getAttributeRequesterMetadata();

    /**
     * Gets the SAML metadata for the attribute issuing entity.
     * 
     * @return SAML metadata for the attribute issuing entity
     */
    public EntityDescriptor getAttributeIssuerMetadata();

    /**
     * Gets the SAML 1 or SAML 2 attribute query.
     * 
     * @return SAML 1 or SAML 2 attribute query
     */
    public QueryType getAttributeQuery();
}