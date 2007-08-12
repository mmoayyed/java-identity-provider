/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider;

import java.util.Map;

import org.opensaml.xml.util.ValueTypeIndexedMap;

import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethSAMLAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;

/**
 * Contextual information for performing an attribute resolution.
 */
public class ShibbolethResolutionContext {

    /** Attribute request context. */
    private ShibbolethSAMLAttributeRequestContext requestContext;

    /** Resolution plug-ins that have been resolved for this request. */
    private ValueTypeIndexedMap<String, ResolutionPlugIn> resolvedPlugins;

    /**
     * Constructor.
     * 
     * @param context the attribute request this resolution is being performed for
     */
    public ShibbolethResolutionContext(ShibbolethSAMLAttributeRequestContext context) {
        requestContext = context;
        resolvedPlugins = new ValueTypeIndexedMap<String, ResolutionPlugIn>(ShibbolethAttributeResolver.PLUGIN_TYPES);
    }

    /**
     * Gets the attribute request that started this resolution.
     * 
     * @return attribute request that started this resolution
     */
    public ShibbolethSAMLAttributeRequestContext getAttributeRequestContext() {
        return requestContext;
    }

    /**
     * Get the resolution plug-ins that have been resolved for this request.
     * 
     * @return the plug-ins that have been resolved for this request.
     */
    public Map<String, ResolutionPlugIn> getResolvedPlugins() {
        return resolvedPlugins;
    }

    /**
     * Get an unmodifiable map of the attribute definitions that have been resolved for this request. To add new
     * definitions, use {@link #getResolvedPlugins} to retrieve a modifiable collection.
     * 
     * @return definitions that have been resolved for this request
     */
    public Map<String, AttributeDefinition> getResolvedAttributeDefinitions() {
        return resolvedPlugins.subMap(AttributeDefinition.class);
    }

    /**
     * Get an unmodifiable map of the data connectors that have been resolved for this request. To add new connectors,
     * use {@link #getResolvedPlugins} to retrieve a modifiable collection.
     * 
     * @return connectors that have been resolved for this request
     */
    public Map<String, DataConnector> getResolvedDataConnectors() {
        return resolvedPlugins.subMap(DataConnector.class);
    }
}