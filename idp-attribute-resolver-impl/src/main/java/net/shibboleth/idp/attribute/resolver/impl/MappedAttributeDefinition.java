/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import net.jcip.annotations.ThreadSafe;
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

/**
 * Implementation of Mapped Attributes.
 * 
 * An attribute definition take the values from previous resolution stages and convert as it creates the output
 * attribute. Each value is compared with a lookup table (a {@link java.util.Collection} of @link{ValueMap}s and if it
 * matches then the appropriate value(s) is/are substituted. Non matches are either passed through or are removed
 * depending on the setting 'passThru'.
 * */
@ThreadSafe
public class MappedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(MappedAttributeDefinition.class);

    /** Default return value. */
    private final String defaultValue;

    /** Whether the definition passes thru unmatched values. */
    private final boolean passThru;

    /** Value maps. */
    private final Collection<ValueMap> valueMaps;

    /**
     * Constructor.
     * 
     * @param id the name
     * @param maps the value maps to apply
     * @param defaultVal the default value to apply (if any)
     * @param passThruParm whether to pass unmatched values through or not.
     */
    public MappedAttributeDefinition(final String id, final Collection<ValueMap> maps, final String defaultVal,
            final boolean passThruParm) {
        super(id);

        final Set<ValueMap> working = new LazySet<ValueMap>();
        CollectionSupport.addNonNull(maps, working);
        valueMaps = Collections.unmodifiableSet(working);

        defaultValue = StringSupport.trimOrNull(defaultVal);
        passThru = passThruParm;

        // Check configuration
        if (passThru && !StringSupport.isNullOrEmpty(defaultValue)) {
            final String message =
                    "MappedAttributeDefinition {} (" + getId()
                            + ") may not have a DefaultValue string with passThru enabled.";
            org.opensaml.util.Assert.isTrue(false, message);
        }
    }

    /**
     * Access to our value maps.
     * 
     * @return the value maps we were initialised with (never null, always normalized and unmodifiable)
     */
    public Collection<ValueMap> getValueMaps() {
        return valueMaps;
    }

    /**
     * Gets the default return value.
     * 
     * @return the default return value. Can be null but can never be "";
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns whether passThru (sic) is set for this version of this resolver.
     * 
     * @return the value of passThru.
     */
    public boolean getPassThru() {
        return passThru;
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            return null;
        }

        final Collection<Object> unmappedResults = new LazySet<Object>();
        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                CollectionSupport.addNonNull(dependentAttribute.getValues(), unmappedResults);
            }
        }

        // Bucket for results
        final Attribute<Object> resultAttribute = new Attribute<Object>(getId());

        if (unmappedResults.isEmpty()) {
            log.debug("Attribute Definition {}: No values from dependency attributes", getId());
            if (null != getDefaultValue()) {
                log.debug("Attribute Definition {}: Default value "
                        + "is not empty, adding it as the value for this attribute", getId());
                resultAttribute.addValue(getDefaultValue());
            }
            return resultAttribute;
        }

        final Collection<Object> mappedValues = new LazySet<Object>();
        for (Object o : unmappedResults) {
            if (o == null) {
                log.debug("Attribute Definition {}: null attribute value, skipping it", getId());
                continue;
            }
            Set<String> mappedValuesThisObject = mapValue(o.toString());
            mappedValues.addAll(mappedValuesThisObject);
        }

        resultAttribute.setValues(mappedValues);
        return resultAttribute;
    }

    /**
     * Maps the value from a dependency in to the value(s) for this attribute.
     * 
     * @param value the value from the dependency
     * 
     * @return the set of attribute values that the given dependency value maps in to
     */
    protected Set<String> mapValue(final String value) {
        log.debug("Attribute Definition {}: mapping depdenency attribute value {}", getId(), value);

        final LazySet<String> mappedValues = new LazySet<String>();
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

}
