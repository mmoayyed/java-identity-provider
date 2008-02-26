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

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.RegexSplitAttributeDefinition;

/** Factory bean for creating regular expression based splitting attribute definitions. */
public class RegexSplitAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** Regular expression used to split values. */
    private String regex;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return RegexSplitAttributeDefinition.class;
    }

    /**
     * Gets the regular expression used to split values.
     * 
     * @return regular expression used to split values
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Sets the regular expression used to split values.
     * 
     * @param regularExpression regular expression used to split values
     */
    public void setRegex(String regularExpression) {
        regex = regularExpression;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        RegexSplitAttributeDefinition definition = new RegexSplitAttributeDefinition(regex);
        populateAttributeDefinition(definition);

        return definition;
    }
}