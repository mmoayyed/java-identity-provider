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

package net.shibboleth.idp.attribute.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.TransformedInputCollectionBuilder;
import net.shibboleth.utilities.java.support.collection.TransformedInputMapBuilder;
import net.shibboleth.utilities.java.support.logic.TrimOrNullStringFunction;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Optional;

/** A context which carries and collects information through an attribute resolution. */
@NotThreadSafe
public class AttributeResolutionContext extends BaseContext {

    /** Attributes that have been requested to be resolved. */
    private Set<Attribute> requestedAttributes;

    /** Attributes which were resolved and released by the attribute resolver. */
    private Map<String, Attribute> resolvedAttributes;

    /** Attribute definitions that have been resolved and the resultant attribute. */
    private final Map<String, ResolvedAttributeDefinition> resolvedAttributeDefinitions;

    /** Data connectors that have been resolved and the resultant attributes. */
    private final Map<String, ResolvedDataConnector> resolvedDataConnectors;

    /** Constructor. */
    public AttributeResolutionContext() {
        requestedAttributes = new TransformedInputCollectionBuilder().buildSet();

        TransformedInputMapBuilder mapBuilder =
                new TransformedInputMapBuilder().keyPreprocessor(TrimOrNullStringFunction.INSTANCE);
        resolvedAttributes = mapBuilder.buildMap();
        resolvedAttributeDefinitions = mapBuilder.buildMap();
        resolvedDataConnectors = mapBuilder.buildMap();
    }

    /**
     * Gets the set of attributes requested to be resolved.
     * 
     * @return set of attributes requested to be resolved
     */
    @Nonnull @NonnullElements public Set<Attribute> getRequestedAttributes() {
        return requestedAttributes;
    }

    /**
     * Sets the set of attributes requested to be resolved.
     * 
     * @param attributes attributes requested to be resolved
     */
    public void setRequestedAttributes(@Nullable @NullableElements final Set<Attribute> attributes) {
        requestedAttributes = new TransformedInputCollectionBuilder().addAll(attributes).buildSet();
    }

    /**
     * Gets the collection of resolved attributes.
     * 
     * @return set of resolved attributes
     */
    @Nonnull @NonnullElements public Map<String, Attribute> getResolvedAttributes() {
        return resolvedAttributes;
    }

    /**
     * Sets the set of resolved attributes.
     * 
     * @param attributes set of resolved attributes, may be null, empty or contain null values
     */
    public void setResolvedAttributes(@Nullable @NullableElements final Collection<Attribute> attributes) {
        TransformedInputMapBuilder<String, Attribute> mapBuilder =
                new TransformedInputMapBuilder<String, Attribute>().keyPreprocessor(TrimOrNullStringFunction.INSTANCE);

        if (attributes != null) {
            for (Attribute attribute : attributes) {
                if (attribute != null) {
                    mapBuilder.put(attribute.getId(), attribute);
                }
            }
        }

        resolvedAttributes = mapBuilder.buildMap();
    }

    /**
     * Gets the resolved attribute definitions that been recorded.
     * 
     * @return resolved attribute definitions that been recorded
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, ResolvedAttributeDefinition>
            getResolvedAttributeDefinitions() {
        return Collections.unmodifiableMap(resolvedAttributeDefinitions);
    }

    /**
     * Records the results of an attribute definition resolution.
     * 
     * @param definition the resolved attribute definition, must not be null
     * @param attribute the attribute produced by the given attribute definition, may be null
     * 
     * @throws AttributeResolutionException thrown if a result of a resolution for the given attribute definition have
     *             already been recorded
     */
    public void recordAttributeDefinitionResolution(@Nonnull final BaseAttributeDefinition definition,
            @Nonnull final Optional<Attribute> attribute) throws AttributeResolutionException {
        assert definition != null : "Resolver attribute definition can not be null";
        assert attribute != null : "Resolved attribute can not be null";

        if (resolvedAttributeDefinitions.containsKey(definition.getId())) {
            throw new AttributeResolutionException("The resolution of attribute definition " + definition.getId()
                    + " has already been recorded");
        }

        final ResolvedAttributeDefinition wrapper = new ResolvedAttributeDefinition(definition, attribute);
        resolvedAttributeDefinitions.put(definition.getId(), wrapper);
    }

    /**
     * Gets the resolved data connectors that been recorded.
     * 
     * @return resolved data connectors that been recorded
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, ResolvedDataConnector> getResolvedDataConnectors() {
        return Collections.unmodifiableMap(resolvedDataConnectors);
    }

    /**
     * Records the results of an data connector resolution.
     * 
     * @param connector the resolved data connector, must not be null
     * @param attributes the attribute produced by the given data connector, may be null
     * 
     * @throws AttributeResolutionException thrown if a result of a resolution for the given data connector has already
     *             been recorded
     */
    public void recordDataConnectorResolution(@Nonnull final BaseDataConnector connector,
            @Nonnull final Optional<Map<String, Attribute>> attributes) throws AttributeResolutionException {
        assert connector != null : "Resolver data connector can not be null";
        assert attributes != null : "Resolved attributes can not be null";

        if (resolvedDataConnectors.containsKey(connector.getId())) {
            throw new AttributeResolutionException("The resolution of data connector " + connector.getId()
                    + " has already been recorded");
        }

        final ResolvedDataConnector wrapper = new ResolvedDataConnector(connector, attributes);
        resolvedDataConnectors.put(connector.getId(), wrapper);
    }
}