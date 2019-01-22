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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

import java.util.Arrays;
import java.util.Objects;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A Dependency that references to an Attribute Definition.
 */
public final class ResolverAttributeDefinitionDependency  {

    /** ID of the plugin that will produce the attribute. */
    @Nonnull @NotEmpty private final String dependencyPluginId;

    /** ID of the attribute, produced by the identified plugin, whose values will be used by the dependent plugin. */
    @Nullable private String dependencyAttributeId;

    /**
     * Constructor.
     * 
     * @param pluginId ID of the plugin that will produce the attribute, never null or empty
     */
    public ResolverAttributeDefinitionDependency(
            @Nonnull @NotEmpty @ParameterName(name="pluginId") final String pluginId) {
        dependencyPluginId =
                Constraint.isNotNull(StringSupport.trimOrNull(pluginId),
                        "Dependency plugin ID may not be null or empty");
    }

    /**
     * Gets the ID of the plugin that will produce the attribute.
     * 
     * @return ID of the plugin that will produce the attribute, never null or empty
     */
    @Nonnull public String getDependencyPluginId() {
        return dependencyPluginId;
    }

    /**
     * Set the attributeId.
     * 
     * @param attributeId ID of the attribute, produced by the identified plugin, whose values will be used by the
     *            dependent plugin
     */
    public void setDependencyAttributeId(@Nullable final String attributeId) {
        dependencyAttributeId = StringSupport.trimOrNull(attributeId);
    }

    /**
     * Gets the ID of the attribute, produced by the identified plugin, whose values will be used by the dependent
     * plugin.
     * 
     * @return ID of the attribute, produced by the identified plugin, whose values will be used by the dependent
     *         plugin, never null or empty
     */
    @Nullable public String getDependencyAttributeId() {
        return dependencyAttributeId;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int[] input = {getDependencyPluginId().hashCode(),
                getDependencyAttributeId()!=null?getDependencyAttributeId().hashCode(): 0};
        return Arrays.hashCode(input);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final ResolverAttributeDefinitionDependency other = (ResolverAttributeDefinitionDependency) obj;
        return Objects.equals(getDependencyPluginId(), other.getDependencyPluginId())
                && Objects.equals(getDependencyAttributeId(), other.getDependencyAttributeId());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("pluginId", dependencyPluginId)
                .add("attributeId", dependencyAttributeId).toString();
    }
}
