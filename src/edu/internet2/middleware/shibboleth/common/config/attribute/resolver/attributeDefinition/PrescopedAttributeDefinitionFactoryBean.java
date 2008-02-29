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

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.PrescopedAttributeDefinition;

/**
 * Spring factory bean that produces {@link PrescopedAttributeDefinition}s.
 */
public class PrescopedAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** Delimiter between value and scope. */
    private String scopeDelimiter;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return PrescopedAttributeDefinition.class;
    }

    /**
     * Gets the delimiter between value and scope.
     * 
     * @return delimiter between value and scope
     */
    public String getScopeDelimiter() {
        return scopeDelimiter;
    }

    /**
     * Sets the delimiter between value and scope.
     * 
     * @param delimiter delimiter between value and scope
     */
    public void setScopeDelimiter(String delimiter) {
        scopeDelimiter = DatatypeHelper.safeTrimOrNullString(delimiter);
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        PrescopedAttributeDefinition definition = new PrescopedAttributeDefinition(getScopeDelimiter());
        populateAttributeDefinition(definition);

        return definition;
    }
}