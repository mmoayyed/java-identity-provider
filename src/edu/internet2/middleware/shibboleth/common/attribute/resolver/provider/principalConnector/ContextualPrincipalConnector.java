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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector;

import java.util.Set;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Wrapper for a {@link PrincipalConnector} within a resolution context. This wrapper ensures that the connector is
 * resolved only once per context.
 */
public class ContextualPrincipalConnector implements PrincipalConnector {

    /** Wrapped principal connector. */
    private PrincipalConnector connector;

    /** Cached result of resolving the connector. */
    private String principal;

    /**
     * Constructor.
     * 
     * @param newConnector principal connector to wrap
     */
    public ContextualPrincipalConnector(PrincipalConnector newConnector) {
        this.connector = newConnector;
    }

    /** {@inheritDoc} */
    public Set<String> getDependencyIds() {
        return connector.getDependencyIds();
    }

    /** {@inheritDoc} */
    public String getFormat() {
        return connector.getFormat();
    }

    /** {@inheritDoc} */
    public String getId() {
        return connector.getId();
    }

    /** {@inheritDoc} */
    public Set<String> getRelyingParties() {
        return connector.getRelyingParties();
    }

    /** {@inheritDoc} */
    public String resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        if (principal == null) {
            principal = connector.resolve(resolutionContext);
        }

        return principal;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        connector.validate();
    }

}