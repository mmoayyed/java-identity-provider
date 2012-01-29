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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Represents the dependency of one {@link BaseResolverPlugin} upon another plugin. A plugin may depend on:
 * <ul>
 * <li>all attributes provided by another plugin</li>
 * <li>a specific attribute provided by another plugin</li>
 * </ul>
 */
@ThreadSafe
public final class ResolverPluginDependency {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ResolverPluginDependency.class);

    /** ID of the plugin that will produce the attribute. */
    private final String dependencyPluginId;

    /** ID of the attribute, produced by the identified plugin, whose values will be used by the dependent plugin. */
    private final Optional<String> dependencyAttributeId;
    
    /**
     * Constructor.
     * 
     * @param pluginId ID of the plugin that will produce the attribute, never null or empty
     * @param attributeId ID of the attribute, produced by the identified plugin, whose values will be used by the
     *            dependent plugin
     */
    public ResolverPluginDependency(@Nonnull @NotEmpty final String pluginId, @Nullable final String attributeId) {
        dependencyPluginId =
                Assert.isNotNull(StringSupport.trimOrNull(pluginId), "Dependency plugin ID may not be null or empty");

        dependencyAttributeId = Optional.fromNullable(StringSupport.trimOrNull(attributeId));
    }

    /**
     * Gets the ID of the plugin that will produce the attribute.
     * 
     * @return ID of the plugin that will produce the attribute, never null or empty
     */
    public String getDependencyPluginId() {
        return dependencyPluginId;
    }

    /**
     * Gets the ID of the attribute, produced by the identified plugin, whose values will be used by the dependent
     * plugin.
     * 
     * @return ID of the attribute, produced by the identified plugin, whose values will be used by the dependent
     *         plugin, never null or empty
     */
    public Optional<String> getDependencyAttributeId() {
        return dependencyAttributeId;
    }

    /**
     * A convenience method that fetches the dependent attribute from the current {@link AttributeResolutionContext}.
     * This method will first look for an {@link ResolvedAttributeDefinition} with an ID matching
     * {@link #dependencyPluginId} and, if found, will return the attribute resolved by that definition. If an attribute
     * definition can not be found and a {@link #dependencyAttributeId} was specified, then this method looks for a
     * {@link ResolvedDataConnector} with an ID matching {@link #dependencyPluginId} and, if found, returns the
     * attribute with a matching {@link #dependencyAttributeId}.
     * 
     * <p>
     * <strong>NOTE</strong>, this method does *not* actually trigger any attribute definition or data connector
     * resolution, it only looks for the cached results of previously resolved plugins within the current resolution
     * context.
     * </p>
     * 
     * @param resolutionContext current resolution context
     * 
     * @return the fetched attribute or {@link Optional#absent()} if no such attribute could be found in the current
     *         resolution context
     */
    public Optional<Attribute> getAttributeFromDependency(AttributeResolutionContext resolutionContext) {
        
        try {
            ResolvedAttributeDefinition attributeDefinition =
                    resolutionContext.getResolvedAttributeDefinitions().get(dependencyPluginId);
            if (attributeDefinition != null) {
                return attributeDefinition.resolve(resolutionContext);
            }

            ResolvedDataConnector dataConnector = resolutionContext.getResolvedDataConnectors().get(dependencyPluginId);
            if (dataConnector != null) {
                return Optional.fromNullable(dataConnector.resolve(resolutionContext).get().get(dependencyAttributeId));
            }
        } catch (AttributeResolutionException e) {
            // nothing to do here, resolved plugins don't thrown exceptions
        }

        return Optional.absent();
    }

    /**
     * A convenience method that fetches the dependent attributes from the current {@link AttributeResolutionContext}.
     * This method will first look for a {@link ResolvedDataConnector} with an ID matching {@link #dependencyPluginId}
     * and, if found, will return the attributes resolved by that data connector. If no such data connector is found,
     * this method will look for an {@link ResolvedAttributeDefinition} with an ID matching {@link #dependencyPluginId}
     * and, if found, will return the attribute, wrapped in a {@link Map}, resolved by that definition.
     * 
     * <p>
     * <strong>NOTE</strong>, this method does <strong>not</strong> trigger any attribute definition or data connector
     * resolution, it only looks for the cached results of previously resolved plugins within the current resolution
     * context.
     * </p>
     * 
     * @param resolutionContext current resolution context
     * 
     * @return the fetched attributes or {@link Optional#absent()} if no such attribute could be found in the current
     *         resolution context
     */
    public Optional<Map<String, Attribute>> getAttributesFromDependency(AttributeResolutionContext resolutionContext) {
        if (dependencyPluginId == null) {
            return Optional.absent();
        }

        try {
            ResolvedDataConnector dataConnector = resolutionContext.getResolvedDataConnectors().get(dependencyPluginId);
            if (dataConnector != null) {
                return dataConnector.resolve(resolutionContext);
            }

            ResolvedAttributeDefinition attributeDefinition =
                    resolutionContext.getResolvedAttributeDefinitions().get(dependencyPluginId);
            if (attributeDefinition != null) {
                Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();
                Optional<Attribute> optionalResult = attributeDefinition.resolve(resolutionContext);
                if (optionalResult.isPresent()) {
                    attributeMap.put(dependencyPluginId, optionalResult.get());
                    return Optional.of(attributeMap);
                }
            }

        } catch (AttributeResolutionException e) {
            // nothing to do here, resolved plugins don't thrown exceptions
        }

        return Optional.absent();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(dependencyPluginId, dependencyAttributeId);
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ResolverPluginDependency other = (ResolverPluginDependency) obj;
        if (Objects.equal(getDependencyPluginId(), other.getDependencyPluginId())
                && Objects.equal(getDependencyAttributeId(), other.getDependencyAttributeId())) {
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("pluginId", dependencyPluginId)
                .add("attributeId", dependencyAttributeId).toString();
    }
}