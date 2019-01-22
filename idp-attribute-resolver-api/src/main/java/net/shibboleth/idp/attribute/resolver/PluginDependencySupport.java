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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Support class for working with {@link ResolverPluginDependency}. */
public final class PluginDependencySupport {

    /** Log. */
    private static final Logger LOG = LoggerFactory.getLogger(PluginDependencySupport.class);

    /** Constructor. */
    private PluginDependencySupport() {

    }

    /**
     * Gets the values, as a single list, from all dependencies. This method only supports dependencies which contain an
     * attribute specifier (i.e. {@link ResolverPluginDependency#getDependencyAttributeId()} does not equal null). It is
     * therefore used inside Attribute definitions which only process a single attribute as input.
     * 
     * <p>
     * <strong>NOTE</strong>, this method does *not* actually trigger any attribute definition or data connector
     * resolution, it only looks for the cached results of previously resolved plugins within the current work context.
     * </p>
     * 
     * @param workContext current attribute resolver work context
     * @param attributeDependencies set of dependencies on attribute definitions
     * @param dataConnectorDependencies set of dependencies on data connector definitions
     * @param attributeDefinitionId the attributeID that these values will be associated with.
     * @return the merged value set. Returns an empty set if we were given a DataConnector as a dependency, but not
     *         attribute name
     */
    // Checkstyle: MethodLength|CyclomaticComplexity OFF
    @Nonnull @NonnullElements public static List<IdPAttributeValue<?>> getMergedAttributeValues(
            @Nonnull final AttributeResolverWorkContext workContext,
            @Nonnull @NonnullElements final Collection<ResolverAttributeDefinitionDependency> attributeDependencies,
            @Nonnull @NonnullElements final Collection<ResolverDataConnectorDependency> dataConnectorDependencies,
            @Nonnull final String attributeDefinitionId) {
        Constraint.isNotNull(workContext, "Attribute resolution context cannot be null");
        Constraint.isNotNull(attributeDependencies, "Resolver dependency collection cannot be null");

        final List<IdPAttributeValue<?>> values = new ArrayList<>();
        if (LOG.isTraceEnabled()) {
            LOG.trace("GetMergedAttribute Values for {}", attributeDefinitionId);
        }

        for (final ResolverAttributeDefinitionDependency attributeDependency : attributeDependencies) {
            Constraint.isNotNull(attributeDependency, "Resolver attribute dependency cannot be null");

            final String attributeId = attributeDependency.getDependencyPluginId();
            final ResolvedAttributeDefinition attributeDefinition =
                    workContext.getResolvedIdPAttributeDefinitions().get(attributeId);
            final IdPAttribute resolvedAttribute = attributeDefinition.getResolvedAttribute();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Merging Attribute Values from Attribute {}", attributeId);
            }
            mergeAttributeValues(resolvedAttribute, values);
        }

        for (final ResolverDataConnectorDependency dataConnectorDependency : dataConnectorDependencies) {
            Constraint.isNotNull(dataConnectorDependency, "Resolver data connector dependency cannot be null");

            final String dataConnectorId = dataConnectorDependency.getDependencyPluginId();
            final ResolvedDataConnector dataConnector = workContext.getResolvedDataConnectors().get(dataConnectorId);
            if (dataConnector != null) {
                final Map<String, IdPAttribute> resolvedAttrs = dataConnector.getResolvedAttributes();
                if (null != resolvedAttrs) {
                    for (final Entry<String, IdPAttribute> entry : resolvedAttrs.entrySet()) {
                        if (dataConnectorDependency.isAllAttributes()
                                || dataConnectorDependency.getAttributeNames().contains(entry.getKey())) {
                            mergeAttributeValues(entry.getValue(), values);
                        }
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Merging Attribute {} from DataConnecteor {}",
                                    entry.getValue(), dataConnectorId);
                        }
                    }
                }
            }
        }
        return values;
    }
    // Checkstyle: MethodLength|CyclomaticComplexity ON

    /**
     * Gets the values from all dependencies. Attributes, with the same identifier but from different resolver plugins,
     * will have their values merged into a single list within this method's returned map. This method is the equivalent
     * of calling {@link #getMergedAttributeValues(AttributeResolverWorkContext, Collection)} for all attributes
     * resolved by all the given dependencies. This is therefore used when an attribute definition may have multiple
     * input attributes (for instance scripted or templated definitions).
     * 
     * <p>
     * <strong>NOTE</strong>, this method does *not* actually trigger any attribute definition or data connector
     * resolution, it only looks for the cached results of previously resolved plugins within the current work context.
     * </p>
     * 
     * @param workContext current attribute resolver work context
     * @param dataConnectorDependencies set of dependencies on data connector definitions
     * @param attributeDependencies set of dependencies
     * 
     * @return the merged value set
     */
    // Checkstyle: MethodLength|CyclomaticComplexity OFF
    public static Map<String, List<IdPAttributeValue<?>>> getAllAttributeValues(
            @Nonnull final AttributeResolverWorkContext workContext,
            @Nonnull final Collection<ResolverAttributeDefinitionDependency> attributeDependencies,
            @Nonnull @NonnullElements final Collection<ResolverDataConnectorDependency> dataConnectorDependencies) {

        final HashMap<String, List<IdPAttributeValue<?>>> result = new HashMap<>();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Getting all Attribute Values");
        }

        for (final ResolverAttributeDefinitionDependency dependency : attributeDependencies) {
            Constraint.isNotNull(dependency, "Attribute Definition dependency cannot be null");

            final String attributeId = dependency.getDependencyPluginId();
            final ResolvedAttributeDefinition attributeDefinition =
                    workContext.getResolvedIdPAttributeDefinitions().get(attributeId);
            if (attributeDefinition != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Adding Attribute {}", attributeId);
                }
                addAttribute(attributeDefinition.getResolvedAttribute(), result);
            }
        }
        
        for (final ResolverDataConnectorDependency dataConnectorDependency : dataConnectorDependencies) {
            Constraint.isNotNull(dataConnectorDependency, "Data Connector dependency cannot be null");

            // Just add those attributes specified
            final String dataConnectorId = dataConnectorDependency.getDependencyPluginId();
            final ResolvedDataConnector dataConnector =
                    workContext.getResolvedDataConnectors().get(dataConnectorId);
            if (dataConnector != null) { 
                final Map<String, IdPAttribute> resolvedAttrs = dataConnector.getResolvedAttributes();
                if (null != resolvedAttrs) {
                    for (final Entry<String, IdPAttribute> entry : resolvedAttrs.entrySet()) {
                        if (dataConnectorDependency.isAllAttributes()
                                || dataConnectorDependency.getAttributeNames().contains(entry.getKey())) {
                            addAttribute(entry.getValue(), result);
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Adding Attribute {} from Data Connector {}",
                                        entry.getValue(), dataConnectorId);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    // Checkstyle: MethodLength|CyclomaticComplexity ON

    /**
     * Adds the values of the attributes to the target collection of attribute values indexes by attribute ID.
     * 
     * @param sources the source attributes
     * @param target current set attribute values
     */
    @Nonnull private static void mergeAttributes(@Nonnull final Map<String, IdPAttribute> sources,
            @Nullable final Map<String, List<IdPAttributeValue<?>>> target) {
        for (final IdPAttribute source : sources.values()) {
            if (source == null) {
                continue;
            }

            addAttribute(source, target);
        }
    }

    /**
     * Adds the values of the given attribute to the target collection of attribute values.
     * 
     * @param source the source attribute
     * @param target current set attribute values
     */
    @Nonnull private static void addAttribute(@Nullable final IdPAttribute source,
            @Nullable final Map<String, List<IdPAttributeValue<?>>> target) {
        if (source == null) {
            return;
        }
        List<IdPAttributeValue<?>> attributeValues = target.get(source.getId());
        if (attributeValues == null) {
            attributeValues = new ArrayList<>();
            target.put(source.getId(), attributeValues);
        }

        mergeAttributeValues(source, attributeValues);
    }

    /**
     * Adds the values of the given attribute to the set of attribute values.
     * 
     * @param source the source attribute
     * @param target current set attribute values
     */
    @Nonnull private static void mergeAttributeValues(@Nullable final IdPAttribute source,
            @Nonnull final List<IdPAttributeValue<?>> target) {
        if (source != null) {
            target.addAll(source.getValues());
        }
    }
}