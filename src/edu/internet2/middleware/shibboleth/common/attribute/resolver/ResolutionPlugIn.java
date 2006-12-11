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

package edu.internet2.middleware.shibboleth.common.attribute.resolver;

import java.util.List;

/**
 * A base interface for plugins that provide attributes.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
public interface ResolutionPlugIn<ResolvedType> {

    /**
     * Returns the unqiue ID of the plugin.
     * 
     * @return unqiue ID of the plugin
     */
    public String getId();

    /**
     * Gets whether resolution exceptions should be propagated or just logged. If exceptions are not propagated the
     * plugin is treated as if returned null during resolution.
     * 
     * @return whether resolution exceptions should be propagated or just logged
     */
    public boolean getPropagateErrors();

    /**
     * Gets the IDs of the attribute definitions this plugin is dependent on.
     * 
     * @return IDs of the attribute definitions this plugin is dependent on
     */
    public List<String> getAttributeDefinitionDependencyIds();

    /**
     * Gets the IDs of the data connectors this plugin is dependent on.
     * 
     * @return IDs of the data connectors this plugin is dependent on
     */
    public List<String> getDataConnectorDependencyIds();

    /**
     * Performs the attribute resolution for this plugin.
     * 
     * @param resolutionContext the context for the resolution
     * 
     * @return the attributes made available by the resolution
     * 
     * @throws AttributeResolutionException the problem that occured during the resolution
     */
    public ResolvedType resolve(ResolutionContext resolutionContext) throws AttributeResolutionException;
    
    /**
     * Validate the internal state of this plug-in.
     * 
     * @throws AttributeResolutionException if the plug-in has an invalid internal state
     */
    public void validate() throws AttributeResolutionException;
}