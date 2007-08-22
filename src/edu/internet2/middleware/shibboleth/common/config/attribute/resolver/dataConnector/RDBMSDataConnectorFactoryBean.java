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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSColumnDescriptor;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.TemplateEngine;

/**
 * Spring factory bean that produces {@link RDBMSDataConnector}s.
 */
public class RDBMSDataConnectorFactoryBean extends BaseDataConnectorBeanFactory {

    /** Template engine used to transform query templates into queries. */
    private TemplateEngine templateEngine;

    /** Source of connections to the database. */
    private DataSource connectionDataSource;

    /** SQL query template. */
    private String queryTemplate;

    /** Result set column descriptors. */
    private List<RDBMSColumnDescriptor> columnDescriptors;

    /** Whether the database connections should be read-only. */
    private boolean readOnlyConnections;

    /** Whether the SQL query uses stored procedures. */
    private boolean queryUsesStoredProcedures;

    /** Whether results should be cached. */
    private boolean cacheResults;

    /** SQL query used to validate database connections. */
    private String connectionValidationQuery;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return RDBMSDataConnector.class;
    }

    /**
     * Gets whether query results should be cached.
     * 
     * @return whether query results should be cached
     */
    public boolean getCacheResults() {
        return cacheResults;
    }

    /**
     * Sets whether query results should be cached.
     * 
     * @param cache whether query results should be cached
     */
    public void setCacheResults(boolean cache) {
        cacheResults = cache;
    }

    /**
     * Gets the result set column descriptors.
     * 
     * @return result set column descriptors
     */
    public List<RDBMSColumnDescriptor> getColumnDescriptors() {
        return columnDescriptors;
    }

    /**
     * Sets the result set column descriptors.
     * 
     * @param descriptors result set column descriptors
     */
    public void setColumnDescriptors(List<RDBMSColumnDescriptor> descriptors) {
        columnDescriptors = descriptors;
    }

    /**
     * Gets the database connection source.
     * 
     * @return database connection source.
     */
    public DataSource getConnectionDataSource() {
        return connectionDataSource;
    }

    /**
     * Sets the database connection source.
     * 
     * @param source database connection source
     */
    public void setConnectionDataSource(DataSource source) {
        connectionDataSource = source;
    }

    /**
     * Gets the SQL query used to validate a connection's liveness.
     * 
     * @return SQL query used to validate a connection's liveness
     */
    public String getConnectionValidationQuery() {
        return connectionValidationQuery;
    }

    /**
     * Sets the SQL query used to validate a connection's liveness.
     * 
     * @param query SQL query used to validate a connection's liveness
     */
    public void setConnectionValidationQuery(String query) {
        connectionValidationQuery = DatatypeHelper.safeTrimOrNullString(query);
    }

    /**
     * Gets the SQL query template.
     * 
     * @return SQL query template
     */
    public String getQueryTemplate() {
        return queryTemplate;
    }

    /**
     * Sets the SQL query template.
     * 
     * @param template SQL query template
     */
    public void setQueryTemplate(String template) {
        queryTemplate = DatatypeHelper.safeTrimOrNullString(template);
    }

    /**
     * Gets whether the SQL query uses stored procedures.
     * 
     * @return whether the SQL query uses stored procedures
     */
    public boolean getQueryUsesStoredProcedures() {
        return queryUsesStoredProcedures;
    }

    /**
     * Sets whether the SQL query uses stored procedures.
     * 
     * @param storedProcedures whether the SQL query uses stored procedures
     */
    public void setQueryUsesStoredProcedures(boolean storedProcedures) {
        queryUsesStoredProcedures = storedProcedures;
    }

    /**
     * Gets whether the database connection is read-only.
     * 
     * @return whether the database connection is read-only
     */
    public boolean isReadOnlyConnections() {
        return readOnlyConnections;
    }

    /**
     * Sets whether the database connection is read-only.
     * 
     * @param readOnly whether the database connection is read-only
     */
    public void setReadOnlyConnections(boolean readOnly) {
        readOnlyConnections = readOnly;
    }

    /**
     * Gets the template engine used to construct the SQL query from the query template.
     * 
     * @return template engine used to construct the SQL query from the query template
     */
    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    /**
     * Sets the template engine used to construct the SQL query from the query template.
     * 
     * @param engine template engine used to construct the SQL query from the query template
     */
    public void setTemplateEngine(TemplateEngine engine) {
        templateEngine = engine;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        RDBMSDataConnector connector = new RDBMSDataConnector(getConnectionDataSource(),
                getConnectionValidationQuery(), getCacheResults());
        populateDataConnector(connector);
        connector.setTemplateEngine(getTemplateEngine());
        connector.setQueryTemplate(getQueryTemplate());
        connector.setUsesStoredProcedure(getQueryUsesStoredProcedures());
        connector.setConnectionReadOnly(isReadOnlyConnections());

        if (getColumnDescriptors() != null) {
            Map<String, RDBMSColumnDescriptor> columnDecriptors = connector.getColumnDescriptor();
            for (RDBMSColumnDescriptor descriptor : getColumnDescriptors()) {
                columnDecriptors.put(descriptor.getColumnName(), descriptor);
            }
        }

        connector.initialize();

        return connector;
    }
}