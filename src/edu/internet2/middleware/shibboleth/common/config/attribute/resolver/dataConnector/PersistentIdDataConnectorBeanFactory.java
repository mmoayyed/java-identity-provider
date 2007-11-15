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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector;

import javax.sql.DataSource;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.PersistentIdDataConnector;

/** Spring factory bean for {@link PersistentIdDataConnector}s. */
public class PersistentIdDataConnectorBeanFactory extends BaseDataConnectorBeanFactory {

    /** Datasource used to communicate with database. */
    private DataSource datasource;

    /** Salt used when generate hashed IDs. */
    private String salt;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return PersistentIdDataConnector.class;
    }

    /**
     * Gets the datasource used to communicate with database.
     * 
     * @return datasource used to communicate with database
     */
    public DataSource getDatasource() {
        return datasource;
    }

    /**
     * Sets the datasource used to communicate with database.
     * 
     * @param source datasource used to communicate with database
     */
    public void setDatasource(DataSource source) {
        datasource = source;
    }

    /**
     * Gets the salt used when generate hashed IDs.
     * 
     * @return salt used when generate hashed IDs
     */
    public String getSalt() {
        return salt;
    }

    /**
     * Sets the salt used when generate hashed IDs.
     * 
     * @param salt salt used when generate hashed IDs
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        PersistentIdDataConnector connector;
        if (datasource != null) {
            connector = new PersistentIdDataConnector(datasource);
        } else {
            connector = new PersistentIdDataConnector(salt.getBytes());
        }

        populateDataConnector(connector);
        return connector;
    }
}