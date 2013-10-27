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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Support class for working with {@link ResolverPluginDependency}. */
public final class PluginDependencySupport {

    /** Constructor. */
    private PluginDependencySupport() {

    }

    /**
     * Gets the values, as a single set, from all dependencies. This method only supports dependencies which contain an
     * attribute specifier (i.e. {@link ResolverPluginDependency#getDependencyAttributeId()} does not equal null). It
     * is therefore used inside Attribute definitions which only process a single attribute as input.
     * 
     * <p>
     * <strong>NOTE</strong>, this method does *not* actually trigger any attribute definition or data connector
     * resolution, it only looks for the cached results of previously resolved plugins within the current resolution
     * context.
     * </p>
     * 
     * @param resolutionContext current attribute resolution context
     * @param dependencies set of dependencies
     * 
     * @return the merged value set
     */
    @Nonnull @NonnullElements public static Set<AttributeValue<?>> getMergedAttributeValues(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull @NonnullElements final Collection<ResolverPluginDependency> dependencies) {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");
        Constraint.isNotNull(dependencies, "Resolver dependency collection can not be null");

        final Set<AttributeValue<?>> values = new HashSet<>();

        for (ResolverPluginDependency dependency : dependencies) {
            final IdPAttribute resolvedAttribute;
            Constraint.isNotNull(dependency, "Resolver dependency can not be null");

            ResolvedAttributeDefinition attributeDefinition =
                    resolutionContext.getResolvedIdPAttributeDefinitions().get(dependency.getDependencyPluginId());
            if (attributeDefinition != null) {
                resolvedAttribute = attributeDefinition.getResolvedAttribute();
                addAttributeValues(resolvedAttribute, values);
                continue;
            }

            ResolvedDataConnector dataConnector =
                    resolutionContext.getResolvedDataConnectors().get(dependency.getDependencyPluginId());
            if (dataConnector != null) {
                Constraint.isTrue(dependency.getDependencyAttributeId() != null, "Data connector dependencies "
                        + "must specify a dependant attribute ID");

                if (null != dataConnector.getResolvedAttributes()) {
                    resolvedAttribute =
                            dataConnector.getResolvedAttributes().get(dependency.getDependencyAttributeId());
                    addAttributeValues(resolvedAttribute, values);
                    continue;
                }
            }
        }

        return values;
    }

    /**
     * Gets the values from all dependencies. Attributes, with the same identifier but from different resolver plugins,
     * will have their values merged into a single set within this method's returned map. This method is the equivalent
     * of calling {@link #getMergedAttributeValues(AttributeResolutionContext, Collection)} for all attributes resolved
     * by all the given dependencies.  This is therefore used when an attribute definition may have multiple input
     * attributes (for instance scripted or templated definitions).
     * 
     * <p>
     * <strong>NOTE</strong>, this method does *not* actually trigger any attribute definition or data connector
     * resolution, it only looks for the cached results of previously resolved plugins within the current resolution
     * context.
     * </p>
     * 
     * @param resolutionContext current attribute resolution context
     * @param dependencies set of dependencies
     * 
     * @return the merged value set
     */
    public static Map<String, Set<AttributeValue<?>>> getAllAttributeValues(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Collection<ResolverPluginDependency> dependencies) {

        final HashMap<String, Set<AttributeValue<?>>> result = new HashMap<>();

        for (ResolverPluginDependency dependency : dependencies) {
            Constraint.isNotNull(dependency, "Resolver dependency can not be null");

            final ResolvedAttributeDefinition attributeDefinition =
                    resolutionContext.getResolvedIdPAttributeDefinitions().get(dependency.getDependencyPluginId());
            if (attributeDefinition != null) {
                addAttributeValues(attributeDefinition.getResolvedAttribute(), result);
                continue;
            }

            final ResolvedDataConnector dataConnector =
                    resolutionContext.getResolvedDataConnectors().get(dependency.getDependencyPluginId());
            if (dataConnector != null) {
                if (null != dataConnector.getResolvedAttributes()) {
                    addAttributeValues(dataConnector.getResolvedAttributes(), result);
                    continue;
                }
            }
        }

        return result;
    }

    /**
     * Adds the values of the attributes to the target collection of attribute values indexes by attribute ID.
     * 
     * @param sources the source attributes
     * @param target current set attribute values
     */
    @Nonnull private static void addAttributeValues(@Nonnull final Map<String, IdPAttribute> sources,
            @Nullable final Map<String, Set<AttributeValue<?>>> target) {
        for (IdPAttribute source : sources.values()) {
            if (source == null) {
                continue;
            }

            addAttributeValues(source, target);
        }
    }

    /**
     * Adds the values of the given attribute to the target collection of attribute values.
     * 
     * @param source the source attribute
     * @param target current set attribute values
     */
    @Nonnull private static void addAttributeValues(@Nullable final IdPAttribute source,
            @Nullable final Map<String, Set<AttributeValue<?>>> target) {
        if (source == null) {
            return;
        }
        Set<AttributeValue<?>> attributeValues = target.get(source.getId());
        if (attributeValues == null) {
            attributeValues = new HashSet<>();
            target.put(source.getId(), attributeValues);
        }

        addAttributeValues(source, attributeValues);
    }

    /**
     * Adds the values of the given attribute to the set of attribute values.
     * 
     * @param source the source attribute
     * @param target current set attribute values
     */
    @Nonnull private static void addAttributeValues(@Nullable final IdPAttribute source,
            @Nonnull final Set<AttributeValue<?>> target) {
        if (source != null) {
            target.addAll(source.getValues());
        }
    }
}