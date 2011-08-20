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
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UnmodifiableComponentException;
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
    private String defaultValue;

    /** Whether the definition passes thru unmatched values. */
    private boolean passThru;

    /** Value maps. */
    private Collection<ValueMap> valueMaps;

    /**
     * Set the value maps. Cannot be called after initialization.
     * 
     * @param maps the value maps to apply
     */

    public synchronized void setValueMaps(final Collection<ValueMap> maps) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Mapped Attribute definition " + getId()
                    + " has already been initialized, Value maps can not be changed.");
        }

        final Set<ValueMap> working = CollectionSupport.addNonNull(maps, new LazySet<ValueMap>());

        if (working.isEmpty()) {
            log.info("Mapped Attribute definition " + getId() + " empty map supplied");
            valueMaps = Collections.EMPTY_SET;
        } else {
            valueMaps = Collections.unmodifiableSet(working);
        }
    }

    /**
     * Access to our value maps.
     * 
     * @return the value maps we were initialised with (never null after initialization, always normalized and
     *         unmodifiable).
     */
    public Collection<ValueMap> getValueMaps() {
        return valueMaps;
    }

    /**
     * Set the default value. Cannot be called after initialization.
     * 
     * @param defaultVal the default value to apply (if any)
     */
    public synchronized void setDefaultValue(final String defaultVal) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Mapped Attribute definition " + getId()
                    + " has already been initialized, default value can not be changed.");
        }
        defaultValue = StringSupport.trimOrNull(defaultVal);
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
     * Set the pass through value. Cannot be called after initialization.
     * 
     * @param passThruParm whether to pass unmatched values through or not.
     */
    public synchronized void setPassThru(final boolean passThruParm) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Mapped Attribute definition " + getId()
                    + " has already been initialized, passThru value can not be changed.");
        }
        passThru = passThruParm;
    }

    /**
     * Returns whether passThru (sic) is set for this version of this resolver. Defaults to false.
     * 
     * @return the value of passThru.
     */
    public boolean getPassThru() {
        return passThru;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == valueMaps) {
            throw new ComponentInitializationException("Mapped Attribute definition " + getId()
                    + " is being initialized, without value maps being set");
        }
        // Check configuration
        if (passThru && !StringSupport.isNullOrEmpty(defaultValue)) {
            throw new ComponentInitializationException("Mapped Attribute definition " + getId()
                            + ") must not have a DefaultValue string with passThru enabled.");
        }
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
