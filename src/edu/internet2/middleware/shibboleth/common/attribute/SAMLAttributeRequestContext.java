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

package edu.internet2.middleware.shibboleth.common.attribute;

import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;

/**
 *{@link AttributeRequestContext} that adds in SAML information.
 */
public interface SAMLAttributeRequestContext extends AttributeRequestContext{
    
    /**
     * Gets the SAML 1 NameIdentifier or SAML 2 NameID value of the subject.
     * 
     * @return SAML 1 NameIdentifier or SAML 2 NameID value of the subject
     */
    public String getSubjectNameId();
    
    /**
     * Gets the format of the subject name ID.
     * 
     * @return format of the subject name ID
     */
    public String getSubjectNameIdFormat();

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
     * Gets the SAML 1 or SAML 2 attribute query message.
     * 
     * @return SAML 1 or SAML 2 attribute query message
     */
    public SAMLObject getAttributeQuery();
}