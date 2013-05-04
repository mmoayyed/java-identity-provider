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

package net.shibboleth.idp.attribute.resolver.impl.ad.mapped;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.UnsupportedAttributeTypeException;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

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

    /** Value maps. */
    private Set<ValueMap> valueMaps = Collections.emptySet();
    
    /** Whether the definition passes thru unmatched values. */
    private boolean passThru;
    
    /** Default return value. */
    @Nullable private StringAttributeValue defaultValue; 

    /**
     * Gets the functions used to map an input value to an output value.
     * 
     * @return functions used to map an input value to an output value
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<ValueMap> getValueMaps() {
        return valueMaps;
    }

    /**
     * Sets the functions used to map an input value to an output value.
     * 
     * @param mappings functions used to map an input value to an output value
     */
    public synchronized void setValueMaps(@Nullable @NullableElements final Collection<ValueMap> mappings) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        valueMaps = ImmutableSet.copyOf(Iterables.filter(mappings, Predicates.notNull()));
    }

    /**
     * Gets the default return value.
     * 
     * @return the default return value.
     */
    @Nullable public StringAttributeValue getDefaultAttributeValue() {
        return defaultValue;
    }

    /**
     * Gets the default return value.
     * 
     * @return the default return value.
     */
    @Nullable public String getDefaultValue() {
        if (null == defaultValue) {
            return null;
        }
        return defaultValue.getValue();
    }

    /**
     * Sets the default return value.
     * 
     * @param newDefaultValue the default return value
     */
    public void setDefaultValue(@Nullable String newDefaultValue) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        String trimmedDefault = StringSupport.trimOrNull(newDefaultValue);
        if (null == trimmedDefault) {
            defaultValue = null;
        } else {
            defaultValue = new StringAttributeValue(trimmedDefault);
        }
    }

    /**
     * Gets whether the definition passes thru unmatched values.
     * 
     * @return whether the definition passes thru unmatched values.
     */
    public boolean isPassThru() {
        return passThru;
    }

    /**
     * Sets whether the definition passes thru unmatched values.
     * 
     * @param newPassThru whether the definition passes thru unmatched values.
     */
    public void setPassThru(boolean newPassThru) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        passThru = newPassThru;
    }

    /**
     * Maps the value from a dependency in to the value(s) for this attribute.
     * 
     * @param value the value from the dependency
     * 
     * @return the set of attribute values that the given dependency value maps in to
     */
    protected Set<AttributeValue> mapValue(@Nullable String value) {
        log.debug("Attribute Definition {}: mapping depdenency attribute value {}", getId(), value);
        
        final String trimmedValue = StringSupport.trimOrNull(value);
        LazySet<AttributeValue> mappedValues = new LazySet<AttributeValue>();

        boolean valueMapMatch = false;
        if (null != trimmedValue) {
            for (ValueMap valueMap : valueMaps) {
                mappedValues.addAll(valueMap.apply(value));
                if (!mappedValues.isEmpty()) {
                    valueMapMatch = true;
                }
            }

            if (!valueMapMatch) {
                if (passThru) {
                    mappedValues.add(new StringAttributeValue(value));
                } else if (defaultValue != null) {
                    mappedValues.add(defaultValue);
                }
            }
        }

        log.debug("Attribute Definition {}: mapped depdenency attribute value {} to the values {}", new Object[] {
                getId(), value, mappedValues, });
        
        return mappedValues;
    }
    
    /** {@inheritDoc} */
    @Nonnull protected Attribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        final Set<AttributeValue> unmappedResults =
                PluginDependencySupport.getMergedAttributeValues(resolutionContext, getDependencies());
        log.debug("Attribute Definition '{}': Attempting to map the following values: {}", getId(), unmappedResults);

        // Bucket for results
        final Attribute resultAttribute = new Attribute(getId());
       
        if (unmappedResults == null || unmappedResults.isEmpty()) {
            log.debug("Attribute Definition {}: No values from dependencies", getId());
            if (null != defaultValue) {
                log.debug(
                        "Attribute Definition {}: Default value of {} added as the value for this attribute",
                        getId(), defaultValue);
                resultAttribute.getValues().add(defaultValue);
            }
        } else {

            for (AttributeValue unmappedValue : unmappedResults) {
                if (!(unmappedValue instanceof StringAttributeValue)) {
                    throw new ResolutionException(new UnsupportedAttributeTypeException("Attribute definition '"
                            + getId() + "' does not support dependency values of type "
                            + unmappedValue.getClass().getName()));
                }
    
                Set<AttributeValue> mappingResult = mapValue(((StringAttributeValue)unmappedValue).getValue());
                resultAttribute.getValues().addAll(mappingResult);
            }
        }
        return resultAttribute;
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        valueMaps = null;

        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (getDependencies().isEmpty()) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no dependencies were configured");
        }

        if (valueMaps.isEmpty()) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no value mappings were configured");
        }
    }
}