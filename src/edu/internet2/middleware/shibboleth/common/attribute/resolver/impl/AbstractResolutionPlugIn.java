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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.impl;

import java.util.List;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;

/**
 * Base class for all {@link ResolutionPlugIn}s.
 */
public abstract class AbstractResolutionPlugIn<ResolvedType> implements ResolutionPlugIn<ResolvedType> {

    /** The identifier for this plug-in. */
    private static String id;

    /** Whether to propagate errors out of the plug-in as exceptions. */
    private boolean propagateErrors;

    /** IDs of the {@link AttributeDefinition}s this plug-in depends on */
    private List<String> attributeDefinitionDependencyIds;

    /** IDs of the {@link DataConnector}s this plug-in depends on */
    private List<String> dataConnectorDependencyIds;

    /** {@inheritDoc} */
    public List<String> getAttributeDefinitionDependencyIds() {
        return attributeDefinitionDependencyIds;
    }

    /** {@inheritDoc} */
    public List<String> getDataConnectorDependencyIds() {
        return dataConnectorDependencyIds;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public boolean getPropagateErrors() {
        return propagateErrors;
    }

}