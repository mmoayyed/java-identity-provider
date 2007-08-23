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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.util.List;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Wrapper for a {@link DataConnector} within a resolution context. This wrapper ensures that the connector is resolved
 * only once per context.
 */
public class ContextualDataConnector implements DataConnector {

    /** Wrapped data connector. */
    private DataConnector connector;

    /** Cached result of resolving the data connector. */
    private Map<String, BaseAttribute> attributes;

    /**
     * Constructor.
     * 
     * @param newConnector data connector to wrap
     */
    public ContextualDataConnector(DataConnector newConnector) {
        this.connector = newConnector;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        return connector.equals(obj);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return connector.hashCode();
    }

    /** {@inheritDoc} */
    public List<String> getDependencyIds() {
        return connector.getDependencyIds();
    }

    /** {@inheritDoc} */
    public List<String> getFailoverDependencyIds() {
        return connector.getFailoverDependencyIds();
    }

    /** {@inheritDoc} */
    public String getId() {
        return connector.getId();
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        if (attributes == null) {
            attributes = connector.resolve(resolutionContext);
        }

        return attributes;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        connector.validate();
    }
}