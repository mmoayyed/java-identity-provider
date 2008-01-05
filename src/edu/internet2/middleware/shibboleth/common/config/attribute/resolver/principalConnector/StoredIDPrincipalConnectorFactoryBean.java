/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.principalConnector;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.StoredIDDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.StoredIDPrincipalConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.TransientPrincipalConnector;

/**
 * Spring factory bean for {@link TransientPrincipalConnector}s.
 */
public class StoredIDPrincipalConnectorFactoryBean extends BasePrincipalConnectorFactoryBean {

    /** Data connector that produced the ID. */
    private StoredIDDataConnector idProducer;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return StoredIDPrincipalConnector.class;
    }

    /**
     * Gets the data connector that produced the ID.
     * 
     * @return data connector that produced the ID
     */
    public StoredIDDataConnector getIdProducer() {
        return idProducer;
    }

    /**
     * Sets the data connector that produced the ID.
     * 
     * @param producer data connector that produced the ID
     */
    public void setIdProducer(StoredIDDataConnector producer) {
        idProducer = producer;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        StoredIDPrincipalConnector connector = new StoredIDPrincipalConnector(getIdProducer());
        populatePrincipalConnector(connector);

        return connector;
    }
}