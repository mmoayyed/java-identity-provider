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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import java.util.List;

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.MappedAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ValueMap;

/**
 * Spring factory bean that produces {@link MappedAttributeDefinition}s.
 */
public class MappedAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** The default return value. */
    private String defaultValue;

    /** Whether the definition passes thru unmatched values. */
    private boolean passThru;

    /** Value maps. */
    private List<ValueMap> valueMaps;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return MappedAttributeDefinition.class;
    }

    /**
     * Gets the default return value.
     * 
     * @return the default return value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get whether the definition passes thru unmatched values.
     * 
     * @return whether the definition passes thru unmatched values
     */
    public boolean isPassThru() {
        return passThru;
    }

    /**
     * Gets the value maps.
     * 
     * @return the value maps.
     */
    public List<ValueMap> getValueMaps() {
        return valueMaps;
    }

    /**
     * Sets the default return value.
     * 
     * @param newDefaultValue the default return value
     */
    public void setDefaultValue(String newDefaultValue) {
        defaultValue = DatatypeHelper.safeTrimOrNullString(newDefaultValue);
    }

    /**
     * Sets whether the definition passes thru unmatched values.
     * 
     * @param newPassThru whether the definition passes thru unmatched values
     */
    public void setPassThru(boolean newPassThru) {
        passThru = newPassThru;
    }

    /**
     * Sets the value maps.
     * 
     * @param newValueMaps the value maps
     */
    public void setValueMaps(List<ValueMap> newValueMaps) {
        valueMaps = newValueMaps;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        populateAttributeDefinition(definition);

        definition.setDefaultValue(defaultValue);

        definition.setPassThru(passThru);

        definition.getValueMaps().addAll(valueMaps);

        return definition;
    }

}