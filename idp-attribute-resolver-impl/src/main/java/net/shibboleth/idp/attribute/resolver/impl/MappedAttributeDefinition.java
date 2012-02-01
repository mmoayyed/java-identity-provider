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
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
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
    private Collection<ValueMapping> valueMappings;

    /**
     * Gets the functions used to map an input value to an output value.
     * 
     * @return functions used to map an input value to an output value
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<ValueMapping> getValueMappings() {
        return valueMappings;
    }

    /**
     * Sets the functions used to map an input value to an output value.
     * 
     * @param mappings functions used to map an input value to an output value
     */
    public synchronized void setValueMappings(@Nullable @NullableElements final Collection<ValueMapping> mappings) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        valueMappings = ImmutableList.copyOf(Iterables.filter(mappings, Predicates.notNull()));
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Attribute> doAttributeResolution(
            @Nonnull final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        assert resolutionContext != null : "Attribute resolution context can not be null";

        ifNotInitializedThrowUninitializedComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        final Set<AttributeValue> unmappedResults =
                PluginDependencySupport.getMergedAttributeValues(resolutionContext, getDependencies());
        log.debug("Attribute Definition '{}': Attempting to map the following values: {}", getId(), unmappedResults);

        // Bucket for results
        final Attribute resultAttribute = new Attribute(getId());

        Optional<String> mappingResult;
        for (AttributeValue unmappedValue : unmappedResults) {
            if (!(unmappedValue instanceof StringAttributeValue)) {
                // TODO probably make this a specific exception type
                throw new AttributeResolutionException("Attribute definition '" + getId()
                        + "' does not support dependency values of type " + unmappedValue.getClass().getName());
            }

            for (ValueMapping function : valueMappings) {
                mappingResult = function.apply((String) unmappedValue.getValue());
                if (mappingResult.isPresent()) {
                    log.debug("Attribuge definition '{}' mapped value '{}' to '{}'", new Object[] {getId(),
                            unmappedValue.getValue(), mappingResult.get(),});
                    resultAttribute.getValues().add(new StringAttributeValue(mappingResult.get()));
                } else {
                    log.debug("Attribuge definition '{}' was unable to map value '{}' to another value", getId(),
                            unmappedValue.getValue());
                }
            }
        }

        return Optional.of(resultAttribute);
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        valueMappings = null;

        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (getDependencies() == null || getDependencies().isEmpty()) {
            throw new ComponentInitializationException("No dependecies have been specified for attribute definition "
                    + getId());
        }

        if (valueMappings == null || valueMappings.isEmpty()) {
            throw new ComponentInitializationException("No value mapping have been specified for attribute definition "
                    + getId());
        }
    }
}