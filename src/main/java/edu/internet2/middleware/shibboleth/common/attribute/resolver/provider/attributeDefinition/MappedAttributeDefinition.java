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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * The RegexAttributeDefinition allows regular expression based replacements on attribute values, using the regex syntax
 * allowed by {@link java.util.regex.Pattern}.
 */
public class MappedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(MappedAttributeDefinition.class);

    /** Default return value. */
    private String defaultValue;

    /** Whether the definition passes thru unmatched values. */
    private boolean passThru;

    /** Value maps. */
    private Collection<ValueMap> valueMaps;

    
    /** Constructor. */
    public MappedAttributeDefinition() {
       valueMaps = new ArrayList<ValueMap>(5); 
    }
    
    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        BasicAttribute<String> attribute = new BasicAttribute<String>();
        attribute.setId(getId());
        boolean valueMapMatch;

        for (Object o : getValuesFromAllDependencies(resolutionContext)) {
            valueMapMatch = false;
            Set<String> mappedValues;

            for (ValueMap valueMap : valueMaps) {
                mappedValues = valueMap.evaluate(o.toString());
                if (!mappedValues.isEmpty()) {
                    valueMapMatch = true;
                    attribute.getValues().addAll(mappedValues);
                }
            }

            if (!valueMapMatch) {
                if (passThru) {
                    attribute.getValues().add(o.toString());
                } else if (!DatatypeHelper.isEmpty(defaultValue)) {
                    attribute.getValues().add(getDefaultValue());
                }
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        if (passThru && !DatatypeHelper.isEmpty(defaultValue)) {
            log.error("MappedAttributeDefinition (" + getId()
                    + ") may not have a DefaultValue string with passThru enabled.");
            throw new AttributeResolutionException("MappedAttributeDefinition (" + getId()
                    + ") may not have a DefaultValue string with passThru enabled.");
        }
    }

    /**
     * Gets the default return value.
     * @return the default return value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default return value.
     * @param newDefaultValue the default return value
     */
    public void setDefaultValue(String newDefaultValue) {
        defaultValue = newDefaultValue;
    }

    /**
     * Gets whether the definition passes thru unmatched values.
     * @return whether the definition passes thru unmatched values.
     */
    public boolean isPassThru() {
        return passThru;
    }

    /**
     * Sets whether the definition passes thru unmatched values.
     * @param newPassThru whether the definition passes thru unmatched values.
     */
    public void setPassThru(boolean newPassThru) {
        passThru = newPassThru;
    }

    /**
     * Get the value maps.
     * @return the value maps.
     */
    public Collection<ValueMap> getValueMaps() {
        return valueMaps;
    }

}