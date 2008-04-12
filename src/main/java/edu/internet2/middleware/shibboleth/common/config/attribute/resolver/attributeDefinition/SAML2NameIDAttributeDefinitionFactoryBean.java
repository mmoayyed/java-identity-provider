/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.SAML2NameIDAttributeDefinition;

/** Factory bean for creating {@link SAML2NameIDAttributeDefinition}s. */
public class SAML2NameIDAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {
    
    /** Format of the NameID. */
    private String nameIdFormat;

    /** Name qualifier for the NameID. */
    private String nameIdQualifier;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return SAML2NameIDAttributeDefinition.class;
    }
    
    /**
     * Gets the format for the NameID used as an attribute value.
     * 
     * @return format for the NameID used as an attribute value
     */
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the format for the NameID used as an attribute value.
     * 
     * @param format format for the NameID used as an attribute value
     */
    public void setNameIdFormat(String format) {
        nameIdFormat = format;
    }

    /**
     * Gets the NameID qualifier for the NameID used as an attribute value.
     * 
     * @return NameID qualifier for the NameID used as an attribute value
     */
    public String getNameIdQualifier() {
        return nameIdQualifier;
    }

    /**
     * Sets the NameID qualifier for the NameID used as an attribute value.
     * 
     * @param qualifier NameID qualifier for the NameID used as an attribute value
     */
    public void setNameIdQualifier(String qualifier) {
        nameIdQualifier = qualifier;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        SAML2NameIDAttributeDefinition definition = new SAML2NameIDAttributeDefinition();
        populateAttributeDefinition(definition);
        
        definition.setNameIdQualifier(nameIdQualifier);
        definition.setNameIdFormat(nameIdFormat);
        
        return definition;
    }
}