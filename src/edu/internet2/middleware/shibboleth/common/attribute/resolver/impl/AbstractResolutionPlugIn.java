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

import java.util.Set;

import javolution.util.FastSet;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;

/**
 * Base class for all {@link ResolutionPlugIn}s.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
public abstract class AbstractResolutionPlugIn<ResolvedType> implements ResolutionPlugIn<ResolvedType> {

    /** The identifier for this plug-in. */
    private String id;

    /** Whether to propagate errors out of the plug-in as exceptions. */
    private boolean propagateErrors;

    /** IDs of the {@link AttributeDefinition}s this plug-in depends on. */
    private Set<String> attributeDefinitionDependencyIds;

    /** IDs of the {@link DataConnector}s this plug-in depends on. */
    private Set<String> dataConnectorDependencyIds;

    /** Constructor. */
    public AbstractResolutionPlugIn() {
        attributeDefinitionDependencyIds = new FastSet<String>();
        dataConnectorDependencyIds = new FastSet<String>();
    }

    /** {@inheritDoc} */
    public Set<String> getAttributeDefinitionDependencyIds() {
        return attributeDefinitionDependencyIds;
    }

    /** {@inheritDoc} */
    public Set<String> getDataConnectorDependencyIds() {
        return dataConnectorDependencyIds;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Set plug-in id.
     * 
     * @param newId new plug-in id
     */
    public void setId(String newId) {
        id = newId;
    }

    /** {@inheritDoc} */
    public boolean getPropagateErrors() {
        return propagateErrors;
    }

    /**
     * Set propagate errors flag.
     * 
     * @param newPropagateErrors new flag value
     */
    public void setPropagateErrors(boolean newPropagateErrors) {
        propagateErrors = newPropagateErrors;
    }
}