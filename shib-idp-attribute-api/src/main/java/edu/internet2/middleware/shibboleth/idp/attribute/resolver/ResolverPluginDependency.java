/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.attribute.resolver;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.Strings;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;

/** Represents the dependency of one {@link BaseResolverPlugin} upon the attribute values produced by another plugin. */
@ThreadSafe
public class ResolverPluginDependency {

    /** ID of the plugin that will produce the attribute. */
    private final String dependencyPluginId;

    /** ID of the attribute, produced by the identified plugin, whose values will be used by the dependent plugin. */
    private final String dependencyAttributeId;

    /**
     * Constructor.
     * 
     * @param pluginId ID of the plugin that will produce the attribute, never null or empty
     * @param attributeId ID of the attribute, produced by the identified plugin, whose values will be used by the
     *            dependent plugin
     */
    public ResolverPluginDependency(final String pluginId, final String attributeId) {
        dependencyPluginId = Strings.trimOrNull(pluginId);
        Assert.isNotNull(dependencyPluginId, "Dependency plugin ID may not be null or empty");

        dependencyAttributeId = Strings.trimOrNull(attributeId);
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
    public String getDependencyAttributeId() {
        return dependencyAttributeId;
    }

    /**
     * Gets the dependent attribute, resolving the dependency if required.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return the resolved attribute or null if no such attribute can be resolved
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attribute
     */
    public Attribute<?> getDependantAttribute(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        BaseResolverPlugin<?> resolvedPlugin = resolutionContext.getResolvedPlugins().get(dependencyPluginId);

        if (resolvedPlugin instanceof BaseDataConnector) {
            return ((BaseDataConnector) resolvedPlugin).resolve(resolutionContext).get(dependencyAttributeId);
        } else if (resolvedPlugin instanceof BaseAttributeDefinition) {
            return ((BaseAttributeDefinition) resolvedPlugin).resolve(resolutionContext);
        }

        return null;
    }
}