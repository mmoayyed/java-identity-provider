/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.shibboleth.idp.ComponentValidationException;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of Mapped Attributes. */
public class MappedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(MappedAttributeDefinition.class);

    /** Default return value. */
    private String defaultValue;

    /** Whether the definition passes thru unmatched values. */
    private boolean passThru;

    /** Value maps. */
    private Collection<ValueMap> valueMaps;

    /**
     * Constructor.
     * 
     * @param id the name
     * @param map the valuemap
     */
    public MappedAttributeDefinition(String id, Collection<ValueMap> map) {
        super(id);
        if (null == map) {
            valueMaps = new LazySet<ValueMap>();
        } else {
            valueMaps = map;
        }
    }

    /**
     * Gets the default return value.
     * 
     * @return the default return value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default return value.
     * 
     * @param newDefaultValue the default return value
     */
    public void setDefaultValue(String newDefaultValue) {
        defaultValue = newDefaultValue;
    }
    
    /** Set the value maps.
     * @param maps what to set.  Cannot be null.
     */
    public void setValueMaps(Collection<ValueMap> maps) {
        CollectionSupport.nonNullReplace(valueMaps, maps);
    }

    /**
     * Get the value maps.
     * 
     * @return the value maps.
     */
    public Collection<ValueMap> getValueMaps() {
        return Collections.unmodifiableCollection(valueMaps);
    }
    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<ResolverPluginDependency> depends = getDependencies();
        Attribute<Object> result = new Attribute<Object>(getId());
        Collection<Object> unmappedResults = new LazySet<Object>();
        Collection<Object> mappedResults;

        //
        // No input? No output
        //
        if (null == depends) {
            //
            // No input? No output
            //
            return null;
        }
        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                CollectionSupport.addNonNull(dependentAttribute.getValues(), unmappedResults);
            }
        }

        if (unmappedResults.isEmpty()) {
            log.debug("Attribute Definition {}: No values from dependency attributes", getId());
            if (!StringSupport.isNullOrEmpty(getDefaultValue())) {
                log.debug(
                    "Attribute Definition {}: Default value is not empty, adding it as the value for this attribute",
                    getId());
                result.addValue(getDefaultValue());
            }
            return result;
        }

        mappedResults = new LazySet<Object>();
        Set<String> mappedValues;
        for (Object o : unmappedResults) {
            if (o == null) {
                log.debug("Attribute Definition {}: null attribute value, skipping it", getId());
                continue;
            }
            mappedValues = mapValue(o.toString());
            mappedResults.addAll(mappedValues);
        }

        result.setValues(mappedResults);
        return result;
    }

    /**
     * Maps the value from a dependency in to the value(s) for this attribute.
     * 
     * @param value the value from the dependency
     * 
     * @return the set of attribute values that the given dependency value maps in to
     */
    protected Set<String> mapValue(String value) {
        log.debug("Attribute Definition {}: mapping depdenency attribute value {}", getId(), value);

        LazySet<String> mappedValues = new LazySet<String>();

        boolean valueMapMatch = false;
        if (!StringSupport.isNullOrEmpty(value)) {
            for (ValueMap valueMap : valueMaps) {
                mappedValues.addAll(valueMap.evaluate(value));
                if (!mappedValues.isEmpty()) {
                    valueMapMatch = true;
                }
            }

            if (!valueMapMatch) {
                if (passThru) {
                    mappedValues.add(value);
                } else if (getDefaultValue() != null) {
                    mappedValues.add(getDefaultValue());
                }
            }
        }

        log.debug("Attribute Definition {}: mapped depdenency attribute value {} to the values {}", new Object[] {
                getId(), value, mappedValues,});

        return mappedValues;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        if (passThru && !StringSupport.isNullOrEmpty(defaultValue)) {
            log.error("MappedAttributeDefinition (" + getId()
                    + ") may not have a DefaultValue string with passThru enabled.");
            throw new ComponentValidationException("MappedAttributeDefinition (" + getId()
                    + ") may not have a DefaultValue string with passThru enabled.");
        }
    }
}
