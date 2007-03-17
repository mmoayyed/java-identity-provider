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
import java.util.Set;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;

/**
 * Wrapper for an {@link AttributeDefinition} within a {@link ResolutionContext}. This wrapper ensures that the
 * definition is resolved only once per context.
 */
public class ContextualAttributeDefinition implements AttributeDefinition {

    /** Wrapped attribute definition. */
    private AttributeDefinition definition;

    /** Cached result of resolving the attribute definition. */
    private Attribute attribute;

    /**
     * Constructor.
     * 
     * @param newDefinition attribute definition to wrap
     */
    public ContextualAttributeDefinition(AttributeDefinition newDefinition) {
        definition = newDefinition;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        return definition.equals(obj);
    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        return definition.hashCode();
    }

    /** {@inheritDoc} */
    public Set<String> getAttributeDefinitionDependencyIds() {
        return definition.getAttributeDefinitionDependencyIds();
    }

    /** {@inheritDoc} */
    public Map<String, AttributeEncoder> getAttributeEncoders() {
        return definition.getAttributeEncoders();
    }

    /** {@inheritDoc} */
    public Set<String> getDataConnectorDependencyIds() {
        return definition.getDataConnectorDependencyIds();
    }

    /** {@inheritDoc} */
    public String getId() {
        return definition.getId();
    }

    /** {@inheritDoc} */
    public boolean getPropagateErrors() {
        return definition.getPropagateErrors();
    }

    /** {@inheritDoc} */
    public Attribute resolve(ResolutionContext resolutionContext) throws AttributeResolutionException {
        if (attribute == null) {
            attribute = definition.resolve(resolutionContext);
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        definition.validate();
    }

    /** {@inheritDoc} */
    public boolean isDependencyOnly() {
        return definition.isDependencyOnly();
    }
}