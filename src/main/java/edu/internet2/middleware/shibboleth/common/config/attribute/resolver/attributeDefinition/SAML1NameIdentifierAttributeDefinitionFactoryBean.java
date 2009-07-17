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

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.SAML1NameIdentifierAttributeDefinition;

/** Factory bean for creating {@link SAML1NameIdentifierAttributeDefinition}s. */
public class SAML1NameIdentifierAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** Format of the NameIdentifier. */
    private String nameIdentifierFormat;

    /** Name qualifier for the NameIdentifier. */
    private String nameIdentifierQualifier;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return SAML1NameIdentifierAttributeDefinition.class;
    }

    /**
     * Gets the format for the NameIdentifier used as an attribute value.
     * 
     * @return format for the NameIdentifier used as an attribute value
     */
    public String getNameIdentifierFormat() {
        return nameIdentifierFormat;
    }

    /**
     * Sets the format for the NameIdentifier used as an attribute value.
     * 
     * @param format format for the NameIdentifier used as an attribute value
     */
    public void setNameIdentifierFormat(String format) {
        nameIdentifierFormat = format;
    }

    /**
     * Gets the NameIdentifier qualifier for the NameIdentifier used as an attribute value.
     * 
     * @return NameIdentifier qualifier for the NameIdentifier used as an attribute value
     */
    public String getNameIdentifierQualifier() {
        return nameIdentifierQualifier;
    }

    /**
     * Sets the NameIdentifier qualifier for the NameIdentifier used as an attribute value.
     * 
     * @param qualifier NameIdentifier qualifier for the NameIdentifier used as an attribute value
     */
    public void setNameIdentifierQualifier(String qualifier) {
        nameIdentifierQualifier = qualifier;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        SAML1NameIdentifierAttributeDefinition definition = new SAML1NameIdentifierAttributeDefinition();
        populateAttributeDefinition(definition);

        definition.setNameIdQualifier(nameIdentifierQualifier);
        definition.setNameIdFormat(nameIdentifierFormat);

        return definition;
    }
}